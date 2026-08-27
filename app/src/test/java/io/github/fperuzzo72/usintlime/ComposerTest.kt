package io.github.fperuzzo72.usintlime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposerTest {

    private val composer = Composer()

    /** Types a sequence and returns everything that got committed. */
    private fun type(vararg steps: Any): String {
        val out = StringBuilder()
        for (step in steps) {
            val text = when (step) {
                is Dead -> composer.onDead(step)
                is String -> composer.onText(step)
                else -> throw IllegalArgumentException("bad step: $step")
            }
            if (text != null) out.append(text)
        }
        return out.toString()
    }

    @Test
    fun plainTextPassesThrough() {
        assertEquals("hello", type("h", "e", "l", "l", "o"))
    }

    @Test
    fun deadKeyAloneCommitsNothing() {
        assertNull(composer.onDead(Dead.ACUTE))
        assertTrue(composer.hasPending)
    }

    @Test
    fun acuteComposesVowels() {
        assertEquals("á", type(Dead.ACUTE, "a"))   // a-acute
        assertEquals("é", type(Dead.ACUTE, "e"))   // e-acute
        assertEquals("Í", type(Dead.ACUTE, "I"))   // I-acute
    }

    @Test
    fun acuteComposesCedillaAndYAcute() {
        assertEquals("ç", type(Dead.ACUTE, "c"))   // c-cedilla
        assertEquals("Ç", type(Dead.ACUTE, "C"))   // C-cedilla
        assertEquals("ý", type(Dead.ACUTE, "y"))   // y-acute
        assertEquals("Ý", type(Dead.ACUTE, "Y"))   // Y-acute
    }

    @Test
    fun tildeComposesNAndVowels() {
        assertEquals("ñ", type(Dead.TILDE, "n"))   // n-tilde
        assertEquals("ã", type(Dead.TILDE, "a"))   // a-tilde
        assertEquals("Õ", type(Dead.TILDE, "O"))   // O-tilde
    }

    @Test
    fun diaeresisHasLowercaseYButNoUppercaseY() {
        assertEquals("ÿ", type(Dead.DIAERESIS, "y"))   // y-diaeresis
        // The Windows layout has no Y-diaeresis, so the pair does not compose.
        assertEquals("\"Y", type(Dead.DIAERESIS, "Y"))
    }

    @Test
    fun deadKeyPlusSpaceGivesTheAccentAlone() {
        assertEquals("'", type(Dead.ACUTE, " "))
        assertEquals("`", type(Dead.GRAVE, " "))
        assertEquals("~", type(Dead.TILDE, " "))
        assertEquals("^", type(Dead.CIRCUMFLEX, " "))
        assertEquals("\"", type(Dead.DIAERESIS, " "))
    }

    @Test
    fun uncomposablePairEmitsBothCharacters() {
        assertEquals("'q", type(Dead.ACUTE, "q"))
        assertEquals("~b", type(Dead.TILDE, "b"))
        assertEquals("^1", type(Dead.CIRCUMFLEX, "1"))
    }

    @Test
    fun doubledDeadKeyEmitsOneLiteralAndArmsTheSecond() {
        // Observed behaviour: ' ' a gives 'a-acute, not ''a. The first
        // apostrophe resolves to a literal, the second stays armed.
        assertEquals("'", type(Dead.ACUTE, Dead.ACUTE))
        assertTrue(composer.hasPending)
        assertEquals("á", type("a"))
    }

    @Test
    fun deadKeysDoNotStackOnEachOther() {
        assertEquals("'", type(Dead.ACUTE, Dead.TILDE))
        assertTrue(composer.hasPending)
        // the tilde is what survived, so it is the tilde that composes
        assertEquals("ñ", type("n"))
    }

    @Test
    fun doubledDeadKeyThenSpaceGivesBothLiterals() {
        assertEquals("''", type(Dead.ACUTE, Dead.ACUTE, " "))
        assertFalse(composer.hasPending)
    }

    @Test
    fun flushEmitsPendingLiteralOnce() {
        composer.onDead(Dead.CIRCUMFLEX)
        assertEquals("^", composer.flush())
        assertNull(composer.flush())
    }

    @Test
    fun resetDropsPendingAccent() {
        composer.onDead(Dead.GRAVE)
        composer.reset()
        assertFalse(composer.hasPending)
        assertEquals("a", type("a"))
    }

    @Test
    fun accentThenAccentedWordTypesCleanly() {
        // "não é possível" the way it is actually typed
        assertEquals(
            "não é possível",
            type("n", Dead.TILDE, "a", "o", " ", Dead.ACUTE, "e", " ",
                 "p", "o", "s", "s", Dead.ACUTE, "i", "v", "e", "l"),
        )
    }
}
