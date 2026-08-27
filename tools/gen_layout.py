#!/usr/bin/env python3
"""
Generates UsIntlTables.kt from the Windows US-International layout dump.

Source of truth: tools/kbdusx.xml, downloaded from https://kbdlayout.info/kbdusx/
which disassembles the real KBDUSX.DLL shipped with Windows.

Usage:  python3 tools/gen_layout.py
"""
import os
import xml.etree.ElementTree as ET

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
XML = os.path.join(HERE, "kbdusx.xml")
OUT = os.path.join(
    ROOT, "app/src/main/java/io/github/fperuzzo72/usintlime/UsIntlTables.kt"
)

# Windows virtual key -> Android KeyEvent keycode.
# Only the keys the US-International layout actually remaps are listed; every
# other key falls through to the host's default handling.
VK_TO_ANDROID = {
    "VK_1": (8, "KEYCODE_1"), "VK_2": (9, "KEYCODE_2"), "VK_3": (10, "KEYCODE_3"),
    "VK_4": (11, "KEYCODE_4"), "VK_5": (12, "KEYCODE_5"), "VK_6": (13, "KEYCODE_6"),
    "VK_7": (14, "KEYCODE_7"), "VK_8": (15, "KEYCODE_8"), "VK_9": (16, "KEYCODE_9"),
    "VK_0": (7, "KEYCODE_0"),
    "VK_OEM_MINUS": (69, "KEYCODE_MINUS"), "VK_OEM_PLUS": (70, "KEYCODE_EQUALS"),
    "VK_OEM_4": (71, "KEYCODE_LEFT_BRACKET"), "VK_OEM_6": (72, "KEYCODE_RIGHT_BRACKET"),
    "VK_OEM_5": (73, "KEYCODE_BACKSLASH"), "VK_OEM_1": (74, "KEYCODE_SEMICOLON"),
    "VK_OEM_7": (75, "KEYCODE_APOSTROPHE"), "VK_OEM_3": (68, "KEYCODE_GRAVE"),
    "VK_OEM_COMMA": (55, "KEYCODE_COMMA"), "VK_OEM_PERIOD": (56, "KEYCODE_PERIOD"),
    "VK_OEM_2": (76, "KEYCODE_SLASH"), "VK_SPACE": (62, "KEYCODE_SPACE"),
}
for i, letter in enumerate("ABCDEFGHIJKLMNOPQRSTUVWXYZ"):
    VK_TO_ANDROID["VK_" + letter] = (29 + i, "KEYCODE_" + letter)

# Dead key identity, recognised by the DeadKeyTable name in the dump.
DEAD_BY_NAME = {
    "ACUTE/CEDILLA": "ACUTE",
    "UMLAUT": "DIAERESIS",
    "GRAVE": "GRAVE",
    "TILDE": "TILDE",
    "CIRCUMFLEX": "CIRCUMFLEX",
}

LEVELS = [
    ("base", ""),
    ("shift", "VK_SHIFT"),
    ("altGr", "VK_CONTROL VK_MENU"),
    ("shiftAltGr", "VK_SHIFT VK_CONTROL VK_MENU"),
]


def text_of(res):
    """The literal string a <Result> produces, or None."""
    t = res.get("Text")
    if t is not None:
        return t
    cps = res.get("TextCodepoints")
    if cps:
        return "".join(chr(int(c, 16)) for c in cps.split())
    return None


def kt_string(s):
    """Kotlin string literal, escaped so the file stays pure ASCII."""
    out = []
    for c in s:
        if c == '"':
            out.append('\\"')
        elif c == "\\":
            out.append("\\\\")
        elif 0x20 <= ord(c) < 0x7F:
            out.append(c)
        else:
            out.append("\\u%04X" % ord(c))
    return '"' + "".join(out) + '"'


def kt_char(c):
    if c == "'":
        return "'\\''"
    if c == "\\":
        return "'\\\\'"
    if 0x20 <= ord(c) < 0x7F:
        return "'%s'" % c
    return "'\\u%04X'" % ord(c)


def comment(s):
    return s if all(0x20 <= ord(c) < 0x7F for c in s) else s


def main():
    root = ET.parse(XML).getroot()
    keys = []          # (android_code, name, {level: (kind, value)}, capsBase, capsAltGr)
    dead_tables = {}   # dead id -> {base char: composed string}

    for pk in root.find("PhysicalKeys"):
        vk = pk.get("VK") or ""
        if vk not in VK_TO_ANDROID:
            continue
        by_with = {(r.get("With") or ""): r for r in pk.findall("Result")}
        levels = {}
        for name, with_attr in LEVELS:
            res = by_with.get(with_attr)
            if res is None:
                continue
            dkt = res.find("DeadKeyTable")
            if dkt is not None:
                dead_id = DEAD_BY_NAME[dkt.get("Name")]
                levels[name] = ("dead", dead_id)
                table = dead_tables.setdefault(dead_id, {})
                for e in dkt.findall("Result"):
                    base = e.get("With")
                    composed = text_of(e)
                    if base is None or composed is None:
                        continue
                    table[base] = composed
                continue
            t = text_of(res)
            if t is None or t == "":
                continue
            levels[name] = ("text", t)
        if not levels:
            continue

        def literal(level):
            v = levels.get(level)
            return v[1] if v and v[0] == "text" else None

        base, shift = literal("base"), literal("shift")
        altgr, shift_altgr = literal("altGr"), literal("shiftAltGr")
        caps_base = bool(base and shift and base.upper() == shift and base != shift)
        caps_altgr = bool(
            altgr and shift_altgr and altgr.upper() == shift_altgr and altgr != shift_altgr
        )
        code, kname = VK_TO_ANDROID[vk]
        keys.append((code, kname, levels, caps_base, caps_altgr))

    keys.sort(key=lambda k: k[0])

    lines = []
    w = lines.append
    w("// GENERATED FILE - DO NOT EDIT BY HAND.")
    w("//")
    w("// Source of truth: tools/kbdusx.xml, a disassembly of the KBDUSX.DLL that")
    w("// ships with Windows, published by https://kbdlayout.info/kbdusx/")
    w("// Regenerate with:  python3 tools/gen_layout.py")
    w("//")
    w("// Keys are Android KeyEvent keycodes (plain Int literals so this table stays")
    w("// free of Android imports and can be unit tested on a plain JVM).")
    w("")
    w("package io.github.fperuzzo72.usintlime")
    w("")
    w("/** The five dead keys of the US-International layout. */")
    w("enum class Dead(val literal: Char) {")
    order = ["ACUTE", "GRAVE", "TILDE", "CIRCUMFLEX", "DIAERESIS"]
    dead_literal = {}
    for d in order:
        dead_literal[d] = dead_tables[d][" "]
    for i, d in enumerate(order):
        sep = "," if i < len(order) - 1 else ";"
        w("    %s(%s)%s" % (d, kt_char(dead_literal[d]), sep))
    w("}")
    w("")
    w("/** What a single key position emits at one modifier level. */")
    w("sealed class Out {")
    w("    /** A literal string to commit. */")
    w("    data class Text(val s: String) : Out()")
    w("")
    w("    /** A dead key: nothing is committed until the next character arrives. */")
    w("    data class DeadOut(val dead: Dead) : Out()")
    w("}")
    w("")
    w("/**")
    w(" * One physical key across the four modifier levels of the layout.")
    w(" *")
    w(" * [capsBase] and [capsAltGr] say whether Caps Lock swaps that level for its")
    w(" * shifted twin. They are true only where the pair really is a case pair, so")
    w(" * Caps Lock turns `a` into `A` and AltGr+q into A-diaeresis, but leaves `1`/`!`")
    w(" * and AltGr+1 alone.")
    w(" */")
    w("data class KeyMapping(")
    w("    val base: Out? = null,")
    w("    val shift: Out? = null,")
    w("    val altGr: Out? = null,")
    w("    val shiftAltGr: Out? = null,")
    w("    val capsBase: Boolean = false,")
    w("    val capsAltGr: Boolean = false,")
    w(")")
    w("")
    w("object UsIntlTables {")
    w("")
    w("    /** Android keycode -> what the US-International layout puts on that key. */")
    w("    val KEYS: Map<Int, KeyMapping> = mapOf(")
    for code, kname, levels, caps_base, caps_altgr in keys:
        parts = []
        note = []
        for name, _ in LEVELS:
            v = levels.get(name)
            if v is None:
                continue
            if v[0] == "dead":
                parts.append("%s = Out.DeadOut(Dead.%s)" % (name, v[1]))
                note.append("%s=dead %s" % (name, dead_literal[v[1]]))
            else:
                parts.append("%s = Out.Text(%s)" % (name, kt_string(v[1])))
                note.append("%s=%s" % (name, v[1] if v[1] != " " else "space"))
        if caps_base:
            parts.append("capsBase = true")
        if caps_altgr:
            parts.append("capsAltGr = true")
        w("        // %s : %s" % (kname, "  ".join(note)))
        w("        %d to KeyMapping(%s)," % (code, ", ".join(parts)))
    w("    )")
    w("")
    w("    /**")
    w("     * Dead key composition: COMPOSE[dead][base] is the composed character.")
    w("     * The space entry is what the layout emits for the accent on its own.")
    w("     */")
    w("    val COMPOSE: Map<Dead, Map<Char, String>> = mapOf(")
    for d in order:
        w("        Dead.%s to mapOf(" % d)
        items = sorted(dead_tables[d].items(), key=lambda kv: (kv[0] != " ", kv[0]))
        for base, composed in items:
            label = "space" if base == " " else base
            w("            %s to %s,%s"
              % (kt_char(base), kt_string(composed),
                 "  // %s -> %s" % (label, composed)))
        w("        ),")
    w("    )")
    w("}")
    w("")

    with open(OUT, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
    print("wrote %s (%d keys, %d dead key tables)" % (OUT, len(keys), len(dead_tables)))


if __name__ == "__main__":
    main()
