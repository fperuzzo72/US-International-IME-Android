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

**Status: applied to all five on 2026-08-27**, along with the AltGr wiring at
every call site (eleven of them across the five). None of it has been compiled
as firmware or tested on hardware yet. See each project's
`docs/DEVELOPMENT_LOG.md` for what landed where.

## Where the reference came from

The specification is `tools/kbdusx.xml` in this repository: a disassembly of
`KBDUSX.DLL`, the layout Windows itself loads for "United States-International",
published at <https://kbdlayout.info/kbdusx/>. Every table in this repo, Kotlin
and C alike, is generated from that file rather than transcribed, so there is
one source of truth and no chance of a typo drifting between projects. The full
layout is written out for humans in [LAYOUT_REFERENCE.md](LAYOUT_REFERENCE.md).

The existing firmware table was compared against it entry by entry. The result
is worth stating plainly: **of the 53 compositions currently in `dead_keys.h`,
all 53 are correct.** Nothing is wrong. There are three entries missing, and one
half of the layout that was never implemented.

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

The patch is [`microslate-patch/dead_keys.patch`](microslate-patch/dead_keys.patch),
which applies cleanly to any of the five copies:

```bash
git apply --directory=editor/src /path/to/dead_keys.patch
```

Adjust `--directory` per repo: `editor/src` for MicroWriter, MicroBASIC and
MicroBASIC-PaperS3, `src` for the other two.

## Two dead keys in a row: no change needed

An earlier draft of this document proposed a second change here, to
`deadKeyProcess()`. It has been withdrawn, and the reasoning is worth keeping,
because the existing code turned out to be right.

The current implementation handles a non-composable character by emitting the
dead key as a literal and requeueing the character. When the requeued character
is itself a dead key, the requeue path stores it as a new pending accent. So
`'` `'` `a` produces `'á`: one literal apostrophe, then an accented vowel.

The draft argued this should instead emit both literals and clear the state,
giving `''a`. That was reasoning, not evidence. `KBDUSX.DLL` stores no
`dead + dead` entries, so the behaviour is decided by the keyboard driver rather
than by the layout, and the disassembly cannot show it.

**Observed on macOS, 2026-08-27: `'` `'` `a` produces `'á`.** The requeue
behaviour is what a real system does, and it is what the firmware has always
done.

One honest caveat: that observation is from macOS, and the layout this project
targets is the Windows one. The two could in principle differ on this point,
since it is driver behaviour on both. But there is now evidence on one side and
nothing on the other, and the evidence agrees with the code that is already
shipping, so there is no reason left to change it. The Android IME was brought
into line with the firmware rather than the other way round.

## Change 2: the AltGr layer

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
| Two dead keys in a row | requeues, so `''a` gives `'á` | matches observed behaviour | none |
| AltGr keys | 0 | 37 | add `altgr_keys.h` and the call sites |
| CapsLock on AltGr | n/a | swaps case pairs only | comes with the table |
