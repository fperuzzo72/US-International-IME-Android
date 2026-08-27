package io.github.fperuzzo72.usintlime

/**
 * Level selection for the layout: which of a key's four positions applies for a
 * given Shift / Caps Lock / AltGr state.
 *
 * Pure Kotlin, like [Composer], so it can be unit tested without a device.
 */
object UsIntlLayout {

    /**
     * @param keyCode an Android `KeyEvent` keycode.
     * @return what to emit, or null when this layout does not define that key at
     *         that level, in which case the caller should let the framework
     *         handle the key.
     */
    fun resolve(keyCode: Int, shift: Boolean, capsLock: Boolean, altGr: Boolean): Out? {
        val mapping = UsIntlTables.KEYS[keyCode] ?: return null
        return resolve(mapping, shift, capsLock, altGr)
    }

    fun resolve(mapping: KeyMapping, shift: Boolean, capsLock: Boolean, altGr: Boolean): Out? {
        // Caps Lock inverts Shift only where the pair is genuinely a case pair.
        // That is what capsBase and capsAltGr record, so Caps Lock turns `a` into
        // `A` and AltGr+q into A-diaeresis, and leaves `1`/`!` and AltGr+1 alone.
        return if (altGr) {
            val shifted = shift xor (capsLock && mapping.capsAltGr)
            if (shifted) mapping.shiftAltGr ?: mapping.altGr else mapping.altGr
        } else {
            val shifted = shift xor (capsLock && mapping.capsBase)
            if (shifted) mapping.shift ?: mapping.base else mapping.base
        }
    }
}
