package io.github.fperuzzo72.usintlime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Spot checks against the generated tables. Keycode constants are spelled out
 * as literals so this test needs no Android runtime.
 */
class UsIntlLayoutTest {

    private companion object {
        const val KEYCODE_1 = 8
        const val KEYCODE_4 = 11
        const val KEYCODE_5 = 12
        const val KEYCODE_6 = 13
        const val KEYCODE_A = 29
        const val KEYCODE_C = 31
        const val KEYCODE_E = 33
        const val KEYCODE_F = 34
        const val KEYCODE_N = 42
        const val KEYCODE_Q = 45
        const val KEYCODE_S = 47
        const val KEYCODE_COMMA = 55
        const val KEYCODE_GRAVE = 68
        const val KEYCODE_APOSTROPHE = 75
        const val KEYCODE_SLASH = 76
    }

    private fun text(keyCode: Int, shift: Boolean = false, caps: Boolean = false, altGr: Boolean = false) =
        (UsIntlLayout.resolve(keyCode, shift, caps, altGr) as? Out.Text)?.s

    private fun dead(keyCode: Int, shift: Boolean = false, altGr: Boolean = false) =
        (UsIntlLayout.resolve(keyCode, shift, false, altGr) as? Out.DeadOut)?.dead

    @Test
    fun baseLayerIsPlainUs() {
        assertEquals("a", text(KEYCODE_A))
        assertEquals("A", text(KEYCODE_A, shift = true))
        assertEquals("1", text(KEYCODE_1))
        assertEquals("!", text(KEYCODE_1, shift = true))
        assertEquals("/", text(KEYCODE_SLASH))
        assertEquals("?", text(KEYCODE_SLASH, shift = true))
    }

    @Test
    fun theFiveDeadKeysAreWhereWindowsPutsThem() {
        assertEquals(Dead.ACUTE, dead(KEYCODE_APOSTROPHE))
        assertEquals(Dead.DIAERESIS, dead(KEYCODE_APOSTROPHE, shift = true))
        assertEquals(Dead.GRAVE, dead(KEYCODE_GRAVE))
        assertEquals(Dead.TILDE, dead(KEYCODE_GRAVE, shift = true))
        assertEquals(Dead.CIRCUMFLEX, dead(KEYCODE_6, shift = true))
    }

    @Test
    fun altGrLayerCarriesTheSymbols() {
        assertEquals("¡", text(KEYCODE_1, altGr = true))                    // inverted !
        assertEquals("¹", text(KEYCODE_1, shift = true, altGr = true))      // superscript 1
        assertEquals("¤", text(KEYCODE_4, altGr = true))                    // currency sign
        assertEquals("£", text(KEYCODE_4, shift = true, altGr = true))      // pound
        assertEquals("€", text(KEYCODE_5, altGr = true))                    // euro
        assertEquals("¿", text(KEYCODE_SLASH, altGr = true))                // inverted ?
        assertEquals("ß", text(KEYCODE_S, altGr = true))                    // sharp s
        assertEquals("§", text(KEYCODE_S, shift = true, altGr = true))      // section
        assertEquals("©", text(KEYCODE_C, altGr = true))                    // copyright
        assertEquals("¢", text(KEYCODE_C, shift = true, altGr = true))      // cent
    }

    @Test
    fun altGrGivesPrecomposedLettersWithoutADeadKey() {
        assertEquals("ä", text(KEYCODE_Q, altGr = true))                    // a-diaeresis
        assertEquals("Ä", text(KEYCODE_Q, shift = true, altGr = true))
        assertEquals("é", text(KEYCODE_E, altGr = true))                    // e-acute
        assertEquals("ñ", text(KEYCODE_N, altGr = true))                    // n-tilde
        assertEquals("ç", text(KEYCODE_COMMA, altGr = true))                // c-cedilla
        assertEquals("Ç", text(KEYCODE_COMMA, shift = true, altGr = true))
    }

    @Test
    fun altGrOnTheApostropheGivesSpacingAccentsNotDeadKeys() {
        assertEquals("´", text(KEYCODE_APOSTROPHE, altGr = true))           // acute accent
        assertEquals("¨", text(KEYCODE_APOSTROPHE, shift = true, altGr = true))
    }

    @Test
    fun capsLockOnlyFlipsRealCasePairs() {
        assertEquals("A", text(KEYCODE_A, caps = true))
        assertEquals("a", text(KEYCODE_A, shift = true, caps = true))
        // AltGr letters are case pairs too
        assertEquals("Ä", text(KEYCODE_Q, caps = true, altGr = true))
        assertEquals("Ç", text(KEYCODE_COMMA, caps = true, altGr = true))
        // but digits and symbols are not
        assertEquals("1", text(KEYCODE_1, caps = true))
        assertEquals("¡", text(KEYCODE_1, caps = true, altGr = true))
        assertEquals(",", text(KEYCODE_COMMA, caps = true))
    }

    @Test
    fun undefinedPositionsResolveToNull() {
        assertNull(UsIntlLayout.resolve(KEYCODE_F, shift = false, capsLock = false, altGr = true))
        assertNull(UsIntlLayout.resolve(/* KEYCODE_ENTER */ 66, shift = false, capsLock = false, altGr = false))
    }

    @Test
    fun everyDeadKeyHasACompositionTable() {
        for (dead in Dead.entries) {
            val table = UsIntlTables.COMPOSE[dead]
            requireNotNull(table) { "no table for $dead" }
            assertEquals(dead.literal.toString(), table[' '])
        }
    }
}
