# Updating the firmware table to the full US-International layout

This is the hand-off document for the projects that already ship the dead key
table, so they can be brought up to the same fidelity as this IME.

Five repositories carry the table, and as of this writing the file is
byte-identical in all five:

| Repository | Path |
| --- | --- |
| `MicroWriter` | `editor/src/dead_keys.h` |
| `MicroBASIC` | `editor/src/dead_keys.h` |
| `MicroBASIC-PaperS3` | `editor/src/dead_keys.h` |
| `microslate-firmware-US-International` | `src/dead_keys.h` |
| `_arquivo-microslate-touch-deadkeys` | `src/dead_keys.h` |

Everything below applies to all five unchanged.

**Status: not adopted, on purpose.** All three changes were applied to all five
projects on 2026-08-27 and then rolled back the same day. What stays in each
project is the generated `altgr_keys.h`, sitting next to `dead_keys.h` with
nothing calling it: no include, no call, no code changed for it.

The reason is use, not doubt. There is no AltGr keyboard in play with that
firmware, and the virtual keyboard has no such key, so the layer would be
wiring with nothing on the other end of it. Each project's
`docs/DEVELOPMENT_LOG.md` carries that decision and points back here.

This document is therefore the adoption path rather than a record of what
happened. It is written to still be correct on the day someone picks it up.

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

The patch for this change is
[`microslate-patch/dead_keys.patch`](microslate-patch/dead_keys.patch), which
applies cleanly to any of the five copies:

```bash
git apply --directory=editor/src /path/to/dead_keys.patch
```

Adjust `--directory` per repo: `editor/src` for MicroWriter, MicroBASIC and
MicroBASIC-PaperS3, `src` for the other two.

## Change 2: two dead keys in a row

`deadKeyProcess()` handles a non-composable character by emitting the dead key
as a literal and requeueing the character. That is right for `'` then `q`,
which gives `'q`. It behaves differently when the requeued character is itself
a dead key: the requeue path stores it as a new pending accent, so `'` `'` `a`
comes out as `'á`.

Windows does not do that. **Measured on Windows: `'` `'` `a` gives `''a`.** The
second dead key does not stack and does not re-arm; both apostrophes come out
as literals and the state is cleared.

This one is worth recording as a small case study, because it went back and
forth twice. `KBDUSX.DLL` stores no `dead + dead` entries, so the behaviour is
decided by the keyboard driver rather than the layout, and the disassembly
cannot show it. A first draft assumed `''a` by reasoning. A test on macOS then
showed `'á`, which matched the firmware's requeue path, so the change was
withdrawn and the Android IME was brought into line with the firmware. A test
on Windows afterwards showed `''a` after all.

So the two platforms genuinely differ here, and the layout this project targets
is the Windows one. The Android IME is back to `''a`, verified by unit test.
The firmware, if this change is adopted, would need the same:

```c
        // No composition found: emit the dead key as a literal.
        static char flush_buf[3];
        flush_buf[0] = dead;

        // A second dead key does not stack and does not re-arm: on Windows,
        // '' is two apostrophes and ''a is ''a. Requeuing `ch` here would
        // store it as a new pending dead key instead, which is what macOS
        // does and Windows does not.
        if (deadKeyIsDeadChar(ch)) {
            flush_buf[1] = ch;
            flush_buf[2] = '\0';
            return flush_buf;
        }

        // Otherwise emit the dead key alone and requeue `ch`.
        flush_buf[1] = '\0';
        _dead_requeue_char = ch;
        return flush_buf;
```

The lesson worth keeping: everything else in this document came out of the
layout file and was right the first time. The one item that came out of
reasoning was wrong twice before a measurement settled it.

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
dead key engine already uses. Every site that calls `deadKeyProcess()` is a site
that needs this branch, and they already know how to insert a UTF-8 string,
because `deadKeyProcess()` hands them one.

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

These are writing tools with a fixed font, so the open question was whether
`EpdFont` has glyphs for the symbols, not just for the accented letters the
dead keys already produce.

It does. Checked against the intervals in `EpdFont/scripts/fontconvert.py`:
Latin-1 Supplement, General Punctuation and Currency Symbols are all enabled,
which covers **all 60 characters** of the AltGr layer, ¤ ¶ ‘ ’ þ ð æ ø and the
euro sign included. Nothing has to wait for the font, and there is no reason to
ship a subset of the 37 keys.

## Summary

| | Current firmware | Windows | Action |
| --- | --- | --- | --- |
| Dead key compositions | 53 | 56 | add ý, Ý, ÿ |
| Dead key correctness | 53 of 53 correct | | none |
| Two dead keys in a row | requeues, so `''a` gives `'á` | `''a` | change `deadKeyProcess()` |
| AltGr keys | 0 | 37 | add `altgr_keys.h` and the call sites |
| CapsLock on AltGr | n/a | swaps case pairs only | comes with the table |
