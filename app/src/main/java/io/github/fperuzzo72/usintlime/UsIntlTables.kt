// GENERATED FILE - DO NOT EDIT BY HAND.
//
// Source of truth: tools/kbdusx.xml, a disassembly of the KBDUSX.DLL that
// ships with Windows, published by https://kbdlayout.info/kbdusx/
// Regenerate with:  python3 tools/gen_layout.py
//
// Keys are Android KeyEvent keycodes (plain Int literals so this table stays
// free of Android imports and can be unit tested on a plain JVM).

package io.github.fperuzzo72.usintlime

/** The five dead keys of the US-International layout. */
enum class Dead(val literal: Char) {
    ACUTE('\''),
    GRAVE('`'),
    TILDE('~'),
    CIRCUMFLEX('^'),
    DIAERESIS('"');
}

/** What a single key position emits at one modifier level. */
sealed class Out {
    /** A literal string to commit. */
    data class Text(val s: String) : Out()

    /** A dead key: nothing is committed until the next character arrives. */
    data class DeadOut(val dead: Dead) : Out()
}

/**
 * One physical key across the four modifier levels of the layout.
 *
 * [capsBase] and [capsAltGr] say whether Caps Lock swaps that level for its
 * shifted twin. They are true only where the pair really is a case pair, so
 * Caps Lock turns `a` into `A` and AltGr+q into A-diaeresis, but leaves `1`/`!`
 * and AltGr+1 alone.
 */
data class KeyMapping(
    val base: Out? = null,
    val shift: Out? = null,
    val altGr: Out? = null,
    val shiftAltGr: Out? = null,
    val capsBase: Boolean = false,
    val capsAltGr: Boolean = false,
)

object UsIntlTables {

    /** Android keycode -> what the US-International layout puts on that key. */
    val KEYS: Map<Int, KeyMapping> = mapOf(
        // KEYCODE_0 : base=0  shift=)  altGr=’
        7 to KeyMapping(base = Out.Text("0"), shift = Out.Text(")"), altGr = Out.Text("\u2019")),
        // KEYCODE_1 : base=1  shift=!  altGr=¡  shiftAltGr=¹
        8 to KeyMapping(base = Out.Text("1"), shift = Out.Text("!"), altGr = Out.Text("\u00A1"), shiftAltGr = Out.Text("\u00B9")),
        // KEYCODE_2 : base=2  shift=@  altGr=²
        9 to KeyMapping(base = Out.Text("2"), shift = Out.Text("@"), altGr = Out.Text("\u00B2")),
        // KEYCODE_3 : base=3  shift=#  altGr=³
        10 to KeyMapping(base = Out.Text("3"), shift = Out.Text("#"), altGr = Out.Text("\u00B3")),
        // KEYCODE_4 : base=4  shift=$  altGr=¤  shiftAltGr=£
        11 to KeyMapping(base = Out.Text("4"), shift = Out.Text("$"), altGr = Out.Text("\u00A4"), shiftAltGr = Out.Text("\u00A3")),
        // KEYCODE_5 : base=5  shift=%  altGr=€
        12 to KeyMapping(base = Out.Text("5"), shift = Out.Text("%"), altGr = Out.Text("\u20AC")),
        // KEYCODE_6 : base=6  shift=dead ^  altGr=¼
        13 to KeyMapping(base = Out.Text("6"), shift = Out.DeadOut(Dead.CIRCUMFLEX), altGr = Out.Text("\u00BC")),
        // KEYCODE_7 : base=7  shift=&  altGr=½
        14 to KeyMapping(base = Out.Text("7"), shift = Out.Text("&"), altGr = Out.Text("\u00BD")),
        // KEYCODE_8 : base=8  shift=*  altGr=¾
        15 to KeyMapping(base = Out.Text("8"), shift = Out.Text("*"), altGr = Out.Text("\u00BE")),
        // KEYCODE_9 : base=9  shift=(  altGr=‘
        16 to KeyMapping(base = Out.Text("9"), shift = Out.Text("("), altGr = Out.Text("\u2018")),
        // KEYCODE_A : base=a  shift=A  altGr=á  shiftAltGr=Á
        29 to KeyMapping(base = Out.Text("a"), shift = Out.Text("A"), altGr = Out.Text("\u00E1"), shiftAltGr = Out.Text("\u00C1"), capsBase = true, capsAltGr = true),
        // KEYCODE_B : base=b  shift=B
        30 to KeyMapping(base = Out.Text("b"), shift = Out.Text("B"), capsBase = true),
        // KEYCODE_C : base=c  shift=C  altGr=©  shiftAltGr=¢
        31 to KeyMapping(base = Out.Text("c"), shift = Out.Text("C"), altGr = Out.Text("\u00A9"), shiftAltGr = Out.Text("\u00A2"), capsBase = true),
        // KEYCODE_D : base=d  shift=D  altGr=ð  shiftAltGr=Ð
        32 to KeyMapping(base = Out.Text("d"), shift = Out.Text("D"), altGr = Out.Text("\u00F0"), shiftAltGr = Out.Text("\u00D0"), capsBase = true, capsAltGr = true),
        // KEYCODE_E : base=e  shift=E  altGr=é  shiftAltGr=É
        33 to KeyMapping(base = Out.Text("e"), shift = Out.Text("E"), altGr = Out.Text("\u00E9"), shiftAltGr = Out.Text("\u00C9"), capsBase = true, capsAltGr = true),
        // KEYCODE_F : base=f  shift=F
        34 to KeyMapping(base = Out.Text("f"), shift = Out.Text("F"), capsBase = true),
        // KEYCODE_G : base=g  shift=G
        35 to KeyMapping(base = Out.Text("g"), shift = Out.Text("G"), capsBase = true),
        // KEYCODE_H : base=h  shift=H
        36 to KeyMapping(base = Out.Text("h"), shift = Out.Text("H"), capsBase = true),
        // KEYCODE_I : base=i  shift=I  altGr=í  shiftAltGr=Í
        37 to KeyMapping(base = Out.Text("i"), shift = Out.Text("I"), altGr = Out.Text("\u00ED"), shiftAltGr = Out.Text("\u00CD"), capsBase = true, capsAltGr = true),
        // KEYCODE_J : base=j  shift=J
        38 to KeyMapping(base = Out.Text("j"), shift = Out.Text("J"), capsBase = true),
        // KEYCODE_K : base=k  shift=K
        39 to KeyMapping(base = Out.Text("k"), shift = Out.Text("K"), capsBase = true),
        // KEYCODE_L : base=l  shift=L  altGr=ø  shiftAltGr=Ø
        40 to KeyMapping(base = Out.Text("l"), shift = Out.Text("L"), altGr = Out.Text("\u00F8"), shiftAltGr = Out.Text("\u00D8"), capsBase = true, capsAltGr = true),
        // KEYCODE_M : base=m  shift=M  altGr=µ
        41 to KeyMapping(base = Out.Text("m"), shift = Out.Text("M"), altGr = Out.Text("\u00B5"), capsBase = true),
        // KEYCODE_N : base=n  shift=N  altGr=ñ  shiftAltGr=Ñ
        42 to KeyMapping(base = Out.Text("n"), shift = Out.Text("N"), altGr = Out.Text("\u00F1"), shiftAltGr = Out.Text("\u00D1"), capsBase = true, capsAltGr = true),
        // KEYCODE_O : base=o  shift=O  altGr=ó  shiftAltGr=Ó
        43 to KeyMapping(base = Out.Text("o"), shift = Out.Text("O"), altGr = Out.Text("\u00F3"), shiftAltGr = Out.Text("\u00D3"), capsBase = true, capsAltGr = true),
        // KEYCODE_P : base=p  shift=P  altGr=ö  shiftAltGr=Ö
        44 to KeyMapping(base = Out.Text("p"), shift = Out.Text("P"), altGr = Out.Text("\u00F6"), shiftAltGr = Out.Text("\u00D6"), capsBase = true, capsAltGr = true),
        // KEYCODE_Q : base=q  shift=Q  altGr=ä  shiftAltGr=Ä
        45 to KeyMapping(base = Out.Text("q"), shift = Out.Text("Q"), altGr = Out.Text("\u00E4"), shiftAltGr = Out.Text("\u00C4"), capsBase = true, capsAltGr = true),
        // KEYCODE_R : base=r  shift=R  altGr=®
        46 to KeyMapping(base = Out.Text("r"), shift = Out.Text("R"), altGr = Out.Text("\u00AE"), capsBase = true),
        // KEYCODE_S : base=s  shift=S  altGr=ß  shiftAltGr=§
        47 to KeyMapping(base = Out.Text("s"), shift = Out.Text("S"), altGr = Out.Text("\u00DF"), shiftAltGr = Out.Text("\u00A7"), capsBase = true),
        // KEYCODE_T : base=t  shift=T  altGr=þ  shiftAltGr=Þ
        48 to KeyMapping(base = Out.Text("t"), shift = Out.Text("T"), altGr = Out.Text("\u00FE"), shiftAltGr = Out.Text("\u00DE"), capsBase = true, capsAltGr = true),
        // KEYCODE_U : base=u  shift=U  altGr=ú  shiftAltGr=Ú
        49 to KeyMapping(base = Out.Text("u"), shift = Out.Text("U"), altGr = Out.Text("\u00FA"), shiftAltGr = Out.Text("\u00DA"), capsBase = true, capsAltGr = true),
        // KEYCODE_V : base=v  shift=V
        50 to KeyMapping(base = Out.Text("v"), shift = Out.Text("V"), capsBase = true),
        // KEYCODE_W : base=w  shift=W  altGr=å  shiftAltGr=Å
        51 to KeyMapping(base = Out.Text("w"), shift = Out.Text("W"), altGr = Out.Text("\u00E5"), shiftAltGr = Out.Text("\u00C5"), capsBase = true, capsAltGr = true),
        // KEYCODE_X : base=x  shift=X
        52 to KeyMapping(base = Out.Text("x"), shift = Out.Text("X"), capsBase = true),
        // KEYCODE_Y : base=y  shift=Y  altGr=ü  shiftAltGr=Ü
        53 to KeyMapping(base = Out.Text("y"), shift = Out.Text("Y"), altGr = Out.Text("\u00FC"), shiftAltGr = Out.Text("\u00DC"), capsBase = true, capsAltGr = true),
        // KEYCODE_Z : base=z  shift=Z  altGr=æ  shiftAltGr=Æ
        54 to KeyMapping(base = Out.Text("z"), shift = Out.Text("Z"), altGr = Out.Text("\u00E6"), shiftAltGr = Out.Text("\u00C6"), capsBase = true, capsAltGr = true),
        // KEYCODE_COMMA : base=,  shift=<  altGr=ç  shiftAltGr=Ç
        55 to KeyMapping(base = Out.Text(","), shift = Out.Text("<"), altGr = Out.Text("\u00E7"), shiftAltGr = Out.Text("\u00C7"), capsAltGr = true),
        // KEYCODE_PERIOD : base=.  shift=>
        56 to KeyMapping(base = Out.Text("."), shift = Out.Text(">")),
        // KEYCODE_SPACE : base=space  shift=space
        62 to KeyMapping(base = Out.Text(" "), shift = Out.Text(" ")),
        // KEYCODE_GRAVE : base=dead `  shift=dead ~
        68 to KeyMapping(base = Out.DeadOut(Dead.GRAVE), shift = Out.DeadOut(Dead.TILDE)),
        // KEYCODE_MINUS : base=-  shift=_  altGr=¥
        69 to KeyMapping(base = Out.Text("-"), shift = Out.Text("_"), altGr = Out.Text("\u00A5")),
        // KEYCODE_EQUALS : base==  shift=+  altGr=×  shiftAltGr=÷
        70 to KeyMapping(base = Out.Text("="), shift = Out.Text("+"), altGr = Out.Text("\u00D7"), shiftAltGr = Out.Text("\u00F7")),
        // KEYCODE_LEFT_BRACKET : base=[  shift={  altGr=«
        71 to KeyMapping(base = Out.Text("["), shift = Out.Text("{"), altGr = Out.Text("\u00AB")),
        // KEYCODE_RIGHT_BRACKET : base=]  shift=}  altGr=»
        72 to KeyMapping(base = Out.Text("]"), shift = Out.Text("}"), altGr = Out.Text("\u00BB")),
        // KEYCODE_BACKSLASH : base=\  shift=|  altGr=¬  shiftAltGr=¦
        73 to KeyMapping(base = Out.Text("\\"), shift = Out.Text("|"), altGr = Out.Text("\u00AC"), shiftAltGr = Out.Text("\u00A6")),
        // KEYCODE_SEMICOLON : base=;  shift=:  altGr=¶  shiftAltGr=°
        74 to KeyMapping(base = Out.Text(";"), shift = Out.Text(":"), altGr = Out.Text("\u00B6"), shiftAltGr = Out.Text("\u00B0")),
        // KEYCODE_APOSTROPHE : base=dead '  shift=dead "  altGr=´  shiftAltGr=¨
        75 to KeyMapping(base = Out.DeadOut(Dead.ACUTE), shift = Out.DeadOut(Dead.DIAERESIS), altGr = Out.Text("\u00B4"), shiftAltGr = Out.Text("\u00A8")),
        // KEYCODE_SLASH : base=/  shift=?  altGr=¿
        76 to KeyMapping(base = Out.Text("/"), shift = Out.Text("?"), altGr = Out.Text("\u00BF")),
    )

    /**
     * Dead key composition: COMPOSE[dead][base] is the composed character.
     * The space entry is what the layout emits for the accent on its own.
     */
    val COMPOSE: Map<Dead, Map<Char, String>> = mapOf(
        Dead.ACUTE to mapOf(
            ' ' to "'",  // space -> '
            'A' to "\u00C1",  // A -> Á
            'C' to "\u00C7",  // C -> Ç
            'E' to "\u00C9",  // E -> É
            'I' to "\u00CD",  // I -> Í
            'O' to "\u00D3",  // O -> Ó
            'U' to "\u00DA",  // U -> Ú
            'Y' to "\u00DD",  // Y -> Ý
            'a' to "\u00E1",  // a -> á
            'c' to "\u00E7",  // c -> ç
            'e' to "\u00E9",  // e -> é
            'i' to "\u00ED",  // i -> í
            'o' to "\u00F3",  // o -> ó
            'u' to "\u00FA",  // u -> ú
            'y' to "\u00FD",  // y -> ý
        ),
        Dead.GRAVE to mapOf(
            ' ' to "`",  // space -> `
            'A' to "\u00C0",  // A -> À
            'E' to "\u00C8",  // E -> È
            'I' to "\u00CC",  // I -> Ì
            'O' to "\u00D2",  // O -> Ò
            'U' to "\u00D9",  // U -> Ù
            'a' to "\u00E0",  // a -> à
            'e' to "\u00E8",  // e -> è
            'i' to "\u00EC",  // i -> ì
            'o' to "\u00F2",  // o -> ò
            'u' to "\u00F9",  // u -> ù
        ),
        Dead.TILDE to mapOf(
            ' ' to "~",  // space -> ~
            'A' to "\u00C3",  // A -> Ã
            'N' to "\u00D1",  // N -> Ñ
            'O' to "\u00D5",  // O -> Õ
            'a' to "\u00E3",  // a -> ã
            'n' to "\u00F1",  // n -> ñ
            'o' to "\u00F5",  // o -> õ
        ),
        Dead.CIRCUMFLEX to mapOf(
            ' ' to "^",  // space -> ^
            'A' to "\u00C2",  // A -> Â
            'E' to "\u00CA",  // E -> Ê
            'I' to "\u00CE",  // I -> Î
            'O' to "\u00D4",  // O -> Ô
            'U' to "\u00DB",  // U -> Û
            'a' to "\u00E2",  // a -> â
            'e' to "\u00EA",  // e -> ê
            'i' to "\u00EE",  // i -> î
            'o' to "\u00F4",  // o -> ô
            'u' to "\u00FB",  // u -> û
        ),
        Dead.DIAERESIS to mapOf(
            ' ' to "\"",  // space -> "
            'A' to "\u00C4",  // A -> Ä
            'E' to "\u00CB",  // E -> Ë
            'I' to "\u00CF",  // I -> Ï
            'O' to "\u00D6",  // O -> Ö
            'U' to "\u00DC",  // U -> Ü
            'a' to "\u00E4",  // a -> ä
            'e' to "\u00EB",  // e -> ë
            'i' to "\u00EF",  // i -> ï
            'o' to "\u00F6",  // o -> ö
            'u' to "\u00FC",  // u -> ü
            'y' to "\u00FF",  // y -> ÿ
        ),
    )
}
