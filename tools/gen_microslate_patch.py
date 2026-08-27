#!/usr/bin/env python3
"""
Emits the C artifacts needed to bring the MicroSlate/MicroWriter firmware table
up to the full Windows US-International layout.

Reads the same source of truth as the Android tables (tools/kbdusx.xml) and
writes docs/microslate-patch/altgr_keys.h.

Usage:  python3 tools/gen_microslate_patch.py
"""
import os
import xml.etree.ElementTree as ET

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
XML = os.path.join(HERE, "kbdusx.xml")
OUT = os.path.join(ROOT, "docs/microslate-patch/altgr_keys.h")

# Windows virtual key -> USB HID usage id, matching the ids hidToAscii()
# already switches on in input_handler.cpp.
VK_TO_HID = {
    "VK_OEM_MINUS": 0x2D, "VK_OEM_PLUS": 0x2E, "VK_OEM_4": 0x2F,
    "VK_OEM_6": 0x30, "VK_OEM_5": 0x31, "VK_OEM_1": 0x33,
    "VK_OEM_7": 0x34, "VK_OEM_3": 0x35, "VK_OEM_COMMA": 0x36,
    "VK_OEM_PERIOD": 0x37, "VK_OEM_2": 0x38, "VK_SPACE": 0x2C,
    "VK_0": 0x27,
}
for i, letter in enumerate("ABCDEFGHIJKLMNOPQRSTUVWXYZ"):
    VK_TO_HID["VK_" + letter] = 0x04 + i
for i in range(1, 10):
    VK_TO_HID["VK_%d" % i] = 0x1D + i

HID_NAME = dict(
    [(0x2D, "-"), (0x2E, "="), (0x2F, "["), (0x30, "]"), (0x31, "\\"),
     (0x33, ";"), (0x34, "'"), (0x35, "`"), (0x36, ","), (0x37, "."),
     (0x38, "/"), (0x2C, "space"), (0x27, "0")]
    + [(0x04 + i, chr(ord("a") + i)) for i in range(26)]
    + [(0x1D + i, str(i)) for i in range(1, 10)]
)


def text_of(res):
    t = res.get("Text")
    if t is not None:
        return t
    cps = res.get("TextCodepoints")
    if cps:
        return "".join(chr(int(c, 16)) for c in cps.split())
    return None


def c_utf8(s):
    return '"' + "".join("\\x%02X" % b for b in s.encode("utf-8")) + '"'


def main():
    root = ET.parse(XML).getroot()
    rows = []
    for pk in root.find("PhysicalKeys"):
        vk = pk.get("VK") or ""
        if vk not in VK_TO_HID:
            continue
        by_with = {(r.get("With") or ""): r for r in pk.findall("Result")}
        plain = by_with.get("VK_CONTROL VK_MENU")
        shifted = by_with.get("VK_SHIFT VK_CONTROL VK_MENU")
        plain = text_of(plain) if plain is not None else None
        shifted = text_of(shifted) if shifted is not None else None
        if not plain and not shifted:
            continue
        caps = bool(plain and shifted and plain.upper() == shifted and plain != shifted)
        rows.append((VK_TO_HID[vk], plain, shifted, caps))
    rows.sort()

    L = []
    w = L.append
    w("#pragma once")
    w("")
    w("// ---------------------------------------------------------------------------")
    w("// altgr_keys.h - the AltGr layer of the US-International layout")
    w("//")
    w("// GENERATED FILE - regenerate with tools/gen_microslate_patch.py in")
    w("// https://github.com/fperuzzo72/US-International-IME-Android")
    w("//")
    w("// Source of truth: a disassembly of the KBDUSX.DLL that ships with Windows,")
    w("// published at https://kbdlayout.info/kbdusx/")
    w("//")
    w("// This is the half of the layout the dead key engine does not cover. AltGr is")
    w("// the right Alt key (HID modifier bit 0x40, MOD_ALT_RIGHT in config.h).")
    w("//")
    w("// Usage, alongside deadKeyProcess():")
    w("//")
    w("//   if (isAltGr(modifiers)) {")
    w("//     const char* s = altGrToUtf8(keyCode, modifiers, capsLockOn);")
    w("//     if (s) {")
    w("//       // an AltGr character is a literal, so it resolves any pending")
    w("//       // dead key first, exactly like a non-composable letter would")
    w("//       if (deadKeyHasPending()) editorInsertUtf8(deadKeyFlush());")
    w("//       editorInsertUtf8(s);")
    w("//     }")
    w("//     return;  // AltGr positions the layout leaves empty type nothing")
    w("//   }")
    w("// ---------------------------------------------------------------------------")
    w("")
    w("#include <stdint.h>")
    w("#include <stdbool.h>")
    w("")
    w('#include "config.h"  // MOD_ALT_RIGHT, isShift()')
    w("")
    w("#ifdef __cplusplus")
    w('extern "C" {')
    w("#endif")
    w("")
    w("// AltGr is the right Alt key. Left Alt stays a plain modifier, so Alt")
    w("// shortcuts keep working.")
    w("static inline bool isAltGr(uint8_t mod) {")
    w("    return (mod & MOD_ALT_RIGHT) != 0;")
    w("}")
    w("")
    w("typedef struct {")
    w("    uint8_t     hid;      // USB HID usage id, same ids hidToAscii() uses")
    w("    const char* plain;    // AltGr")
    w("    const char* shifted;  // AltGr + Shift, or NULL where the layout is empty")
    w("    bool        caps;     // CapsLock swaps the two (real case pairs only)")
    w("} AltGrEntry;")
    w("")
    w("static const AltGrEntry ALTGR_TABLE[] = {")
    for hid, plain, shifted, caps in rows:
        p = c_utf8(plain) if plain else "NULL"
        s = c_utf8(shifted) if shifted else "NULL"
        note = "%s -> %s" % (HID_NAME.get(hid, "?"), plain or "-")
        if shifted:
            note += " / shift %s" % shifted
        w("    { 0x%02X, %-20s %-20s %-6s },  // %s"
          % (hid, p + ",", s + ",", ("true" if caps else "false"), note))
    w("    { 0x00, NULL, NULL, false }  // sentinel")
    w("};")
    w("")
    w("// Returns the UTF-8 string for an AltGr combination, or NULL when the layout")
    w("// leaves that position empty (AltGr+F, for instance) and nothing should be")
    w("// typed at all.")
    w("static const char* altGrToUtf8(uint8_t hid, uint8_t modifiers, bool capsLockOn) {")
    w("    for (int i = 0; ALTGR_TABLE[i].hid != 0x00; i++) {")
    w("        if (ALTGR_TABLE[i].hid != hid) continue;")
    w("        bool shifted = isShift(modifiers);")
    w("        if (capsLockOn && ALTGR_TABLE[i].caps) shifted = !shifted;")
    w("        const char* out = shifted ? ALTGR_TABLE[i].shifted : ALTGR_TABLE[i].plain;")
    w("        if (out == NULL) out = ALTGR_TABLE[i].plain;  // fall back to the base level")
    w("        return out;")
    w("    }")
    w("    return NULL;")
    w("}")
    w("")
    w("#ifdef __cplusplus")
    w("}")
    w("#endif")
    w("")

    with open(OUT, "w", encoding="utf-8") as f:
        f.write("\n".join(L))
    print("wrote %s (%d AltGr keys)" % (OUT, len(rows)))


if __name__ == "__main__":
    main()
