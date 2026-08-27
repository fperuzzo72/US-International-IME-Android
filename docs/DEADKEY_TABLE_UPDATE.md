# Updating the firmware table to the full US-International layout

This is the hand-off document for the projects that already ship the minimal
dead key table, so they can be brought up to the same fidelity as this IME.

Three repositories carry the table, and as of this writing the file is
byte-identical in all three:

- `MicroWriter/editor/src/dead_keys.h`
- `microslate-firmware-US-International/src/dead_keys.h`
- `_arquivo-microslate-touch-deadkeys/src/dead_keys.h`

Everything below applies to all three unchanged.

## Where the reference came from

The specification is `tools/kbdusx.xml` in this repository: a disassembly of
`KBDUSX.DLL`, the layout Windows itself loads for "United States-International",
published at <https://kbdlayout.info/kbdusx/>. Every table in this repo, Kotlin
and C alike, is generated from that file rather than transcribed, so there is
one source of truth and no chance of a typo drifting between projects. The full
layout is written out for humans in [LAYOUT_REFERENCE.md](LAYOUT_REFERENCE.md).

The existing firmware table was compared against it entry by entry. The result
is worth stating plainly: **of the 53 compositions currently in `dead_keys.h`,
all 53 are correct.** Nothing is wrong. There are three entries missing, one
behavioural difference, and one half of the layout that was never implemented.

## Change 1: three missing compositions

Windows defines 56 compositions across the five dead keys. The firmware has 53.
The three that are absent:

| Dead key | Base | Result | Codepoint |
| --- | --- | --- | --- |
| `'` acute | `y` | ý | U+00FD |
| `'` acute | `Y` | Ý | U+00DD |
| `"` diaeresis | `y` | ÿ | U+00FF |

Note the asymmetry on the last one: the Windows layout has lowercase `ÿ` but no
uppercase `Ÿ` on the diaeresis dead key. That is not an oversight in this
document, it is what the layout does, and the IME reproduces it. Typing `"` then
`Y` gives `"Y`, two characters.

## Change 2: two dead keys in a row

The current `deadKeyProcess()` handles a non-composable character by emitting
the dead key as a literal and requeueing the character. That is right for
`'` then `q`, which gives `'q`. It goes wrong when the requeued character is
itself a dead key: the requeue path stores it as a new pending accent, so
`''` leaves an accent armed and `''a` comes out as `'á`.

On Windows a second dead key does not stack and does not re-arm. `''` is two
apostrophes, `''a` is `''a`, and `'~` is `'~`. This is how people type an
apostrophe without reaching for the space bar, so it matters in practice.

The fix is local to `deadKeyProcess()`: when no composition is found and the
incoming character is itself a dead key, emit both literals and clear the state
instead of requeueing.

Both this change and Change 1 are in
[`microslate-patch/dead_keys.patch`](microslate-patch/dead_keys.patch), which
applies cleanly to any of the three copies:

```bash
git apply --directory=editor/src /path/to/dead_keys.patch
```

Adjust `--directory` per repo: `editor/src` for MicroWriter, `src` for the
other two.

### One thing to verify on a real Windows machine

The `''` behaviour above is the one item in this document that came from
reasoning about the layout rather than from the layout file itself. Windows
stores no `dead + dead` entries in `KBDUSX.DLL`, so what happens there is
decided by the Windows keyboard driver, not by the layout, and the disassembly
cannot show it. Documentation for the layout says only that "hitting the
spacebar or a non-accented letter after a dead key produces the key's normal
value", which does not settle whether the second dead key is consumed or
re-armed.

If a Windows box is ever handy, the deciding test is three keystrokes: `'` `'`
`a`. If it produces `''a`, this implementation is right. If it produces `'á`,
then the original requeue behaviour was right all along and both this IME and
the patch should be reverted on that one point. Everything else here is taken
directly from the layout data.

## Change 3: the AltGr layer

This is the larger addition, and it is entirely new: the firmware currently has
no AltGr handling at all. `hidToAscii()` looks at Shift and CapsLock only, so
the right Alt key does nothing.

The Windows layout puts 37 keys on AltGr, which is roughly half of the layout by
character count. It covers precomposed letters that need no dead key at all
(AltGr+E gives é directly, AltGr+N gives ñ, AltGr+comma gives ç), letters that
have no dead key route at all (ß å æ ø ð þ), currency (€ £ ¥ ¤ ¢), punctuation
(¡ ¿ « » ¶ °), maths (× ÷ ¼ ½ ¾ ² ³ ¹), and the spacing accents ´ and ¨ on the
apostrophe key.

The generated table and lookup function are in
[`microslate-patch/altgr_keys.h`](microslate-patch/altgr_keys.h), written in the
house style of `dead_keys.h`: a static table, UTF-8 hex escapes with the
character in a trailing comment, and a single `static inline` entry point. Drop
it next to `dead_keys.h`.

### Wiring it in

`hidToAscii()` returns a `char`, and every AltGr character is multi-byte UTF-8,
so AltGr cannot go through it. It has to branch earlier, on the same path the
dead key engine already uses. There are two call sites, both in
`input_handler.cpp`: the editor at `handleEditorKey()` and the title editor at
`handleTitleEditKey()`. Both already know how to insert a UTF-8 string, because
`deadKeyProcess()` hands them one.

At each site, before the existing `char c = hidToAscii(keyCode, modifiers);`
line:

```c
if (isAltGr(modifiers)) {
    const char* s = altGrToUtf8(keyCode, modifiers, capsLockOn);
    if (s) {
        // An AltGr character is a literal, so it resolves a pending dead key
        // first, exactly the way a non-composable letter does.
        if (deadKeyHasPending()) editorInsertUtf8(deadKeyFlush());
        if (editorHasSelection()) editorDeleteSelection();
        editorInsertUtf8(s);
        screenDirty = true;
    }
    return;  // AltGr positions the layout leaves empty type nothing
}
```

`isAltGr()` ships in `altgr_keys.h` and is one line: `MOD_ALT_RIGHT` is already
defined in `config.h` as `0x40`, it simply was never read. Left Alt stays
untouched, so Alt shortcuts keep working.

Two details the table already encodes, so the call site does not have to think
about them:

- **Empty positions.** AltGr+F, AltGr+G and friends produce nothing on this
  layout. `altGrToUtf8()` returns `NULL` for those, and the `return` above means
  they type nothing, which is what Windows does.
- **CapsLock.** It swaps the AltGr and Shift+AltGr levels only where the pair is
  a genuine case pair, so CapsLock turns AltGr+Q into Ä but leaves AltGr+1 as ¡.
  The `caps` column in the table records which is which. It was derived
  mechanically: `caps` is true exactly where the uppercase of the AltGr
  character equals the Shift+AltGr character.

### Scope note

The firmware is a writing tool with a fixed font, so there is one thing to check
before shipping the AltGr layer: whether `EpdFont` actually has glyphs for the
symbols. The accented Latin letters are almost certainly there already, since
the dead keys produce them. Characters like ¤ ¶ ‘ ’ þ ð æ ø may not be. A
missing glyph is a rendering problem, not a table problem, and the table can
ship whole while the font catches up. Worth a look at the glyph coverage before
deciding whether to expose all 37 keys or start with the letters.

## Summary

| | Current firmware | Windows | Action |
| --- | --- | --- | --- |
| Dead key compositions | 53 | 56 | add ý, Ý, ÿ |
| Dead key correctness | 53 of 53 correct | | none |
| Two dead keys in a row | re-arms the second | emits both literals | change, verify on Windows |
| AltGr keys | 0 | 37 | add `altgr_keys.h` and two call sites |
| CapsLock on AltGr | n/a | swaps case pairs only | comes with the table |
