# US-International IME for Android

A single-purpose Android input method that gives a physical or Bluetooth
keyboard the **Windows US-International layout**, dead keys and the full AltGr
layer included, on any Android device.

`'` then `a` gives `á`. `~` then `n` gives `ñ`. `AltGr+e` gives `é` directly.
`'` then space gives `'`. The way it works on Windows, because the tables are
generated from the layout Windows itself loads.

## Why

Android's own "US International" physical keyboard layout has a long-running
habit of breaking after system updates: dead key composition stops working, or
works in one app and not another, or loses the AltGr layer entirely. See the
[Android community thread](https://support.google.com/android/thread/343601562).

An IME sits ahead of the destination app in the key event path, so it can do the
composition itself and hand the app a finished character. That sidesteps the
system layout completely instead of waiting for it to be fixed.

This was written for a Bigme HiBreak Pro (Android 14) and an Onyx Boox Mini C
(Android 11), and is confirmed working on both, but there is nothing
device-specific in it.

## What it does and does not do

It does one layout, correctly:

- All five dead keys: `'` acute, `` ` `` grave, `~` tilde, `^` circumflex,
  `"` diaeresis, with all 56 compositions Windows defines.
- The complete AltGr layer, all 37 keys: precomposed letters (é ñ ç ä ü ó ...),
  letters with no dead key route (ß å æ ø ð þ), currency (€ £ ¥ ¤ ¢),
  punctuation (¡ ¿ « » ¶ °), maths (× ÷ ¼ ½ ¾ ² ³ ¹), and the spacing accents
  ´ and ¨.
- Caps Lock handled the way the layout defines it, which means it inverts Shift
  only where the pair is a real case pair. `AltGr+q` becomes `Ä` under Caps Lock,
  `AltGr+1` stays `¡`.

It deliberately does not do: custom or importable layouts, per-app or per-device
profiles, a settings screen, or an on-screen keyboard.

Only hardware key events are touched. Every event from a virtual keyboard, and
every key this layout does not define (arrows, function keys, Enter, shortcuts
with Ctrl or left Alt), falls straight through to the framework.

## The on-screen keyboard trade-off

Worth knowing before installing, because it follows from what an IME is: on
Android, the input method **is** the on-screen keyboard. While this one is
selected, tapping a text field will not bring up your usual soft keyboard,
because this IME has taken that role and it has no keys to tap.

That is usually fine on the devices this was built for, since a hardware
keyboard is attached and Android hides the soft keyboard anyway. For the times
it is not, the input view offers a single button that opens the system keyboard
picker, so switching back is one tap. Android's own keyboard-switching shortcut
works as well.

If you want on-screen typing and this layout at the same time, you want two
IMEs and a switch between them, not this app.

## Install

Grab the APK from the latest [build run](../../actions), or build it yourself:

```bash
./gradlew assembleRelease
```

The build run publishes it as `US-Intl-IME.apk`; a local build writes
`app-release.apk` to `app/build/outputs/apk/release/`.

Then:

1. Install the APK. If an older build is already on the device, **uninstall it
   first**: CI signs each run with a fresh debug key, so a new APK will not
   install over an older one. Switch to another keyboard before uninstalling,
   so you are never left without an input method.
2. Open the app. Turning on a keyboard is the one genuinely awkward part of
   installing an IME: it lives across two different Android settings surfaces,
   and the second is a pop-up you cannot deep-link to. The app's only screen
   walks the three steps in order and ticks off the ones already done.

   - **Turn the keyboard on**, in the system keyboard list.
   - **Choose it**, from the keyboard chooser. Same pop-up you use to switch
     back to your usual keyboard later.
   - **Set the physical layout to English (US)**, optional. This app does the
     accents itself and never reads the system layout, so leaving yours on US
     International changes nothing. Plain English (US) just stops the two from
     looking like they disagree.

That screen is a setup wizard, not a settings screen. There is nothing to
configure.

## How the layout is defined

Nothing in this repository is transcribed by hand.

`tools/kbdusx.xml` is a disassembly of `KBDUSX.DLL`, the layout Windows loads
for "United States-International", from <https://kbdlayout.info/kbdusx/>.
Everything else is generated from it:

| Generator | Output |
| --- | --- |
| `tools/gen_layout.py` | `app/src/main/java/.../UsIntlTables.kt`, the Kotlin tables |
| `tools/gen_reference_doc.py` | [`docs/LAYOUT_REFERENCE.md`](docs/LAYOUT_REFERENCE.md), the layout in readable form |
| `tools/gen_microslate_patch.py` | [`docs/microslate-patch/altgr_keys.h`](docs/microslate-patch/altgr_keys.h), the same layer in C |

CI regenerates the Kotlin tables on every build and fails if the committed file
differs, so the tables cannot drift from the source.

## Layout of the code

| File | What it holds |
| --- | --- |
| `UsIntlIme.kt` | The `InputMethodService`. Event filtering, modifier reading, commit. The only file that knows about `KeyEvent`. |
| `UsIntlLayout.kt` | Which of a key's four positions applies, given Shift, Caps Lock and AltGr. |
| `Composer.kt` | The dead key state machine. |
| `UsIntlTables.kt` | Generated. The layout data. |
| `SetupActivity.kt` | Opens the system input method settings and finishes. Not a settings screen. |

`UsIntlLayout`, `Composer` and `UsIntlTables` have no Android imports, so the
whole composition behaviour is covered by plain JVM unit tests in
`app/src/test`.

## Related

The composition logic started life in
[MicroWriter](https://github.com/fperuzzo72/MicroWriter) and
[microslate-firmware-US-International](https://github.com/fperuzzo72/microslate-firmware-US-International),
as C for an ESP32-C3 talking to a Bluetooth keyboard over NimBLE. The event
source is different there, HID reports rather than Android `KeyEvent`, but the
composition is the same problem.

Those projects ship a smaller table. [`docs/DEADKEY_TABLE_UPDATE.md`](docs/DEADKEY_TABLE_UPDATE.md)
is the hand-off for bringing them up to the full layout: what is missing, what
behaves differently, and a ready-to-apply patch.

## License

MIT. See [LICENSE](LICENSE).
