package io.github.fperuzzo72.usintlime

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager

/**
 * A single-purpose IME that reimplements the Windows US-International layout,
 * dead keys and AltGr layer included, for physical and Bluetooth keyboards.
 *
 * Only hardware key events are touched. Anything coming from the virtual
 * keyboard, and every key the layout does not define, falls through to the
 * framework untouched.
 *
 * The layout tables are generated from the real Windows layout: see
 * [UsIntlTables] and `tools/gen_layout.py`.
 */
class UsIntlIme : InputMethodService() {

    private val composer = Composer()

    /** Keycodes consumed on the way down, so the matching up is consumed too. */
    private val consumedKeys = mutableSetOf<Int>()

    // ---------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        composer.reset()
        consumedKeys.clear()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        composer.reset()
        consumedKeys.clear()
    }

    /**
     * The framework requires an input view. This one is deliberately not a
     * keyboard: this IME exists for hardware typing.
     *
     * Being the selected IME does mean this replaces the on-screen keyboard, so
     * the view offers a one-tap route to the system IME picker for the times
     * there is no hardware keyboard around. See the README for the trade-off.
     */
    override fun onCreateInputView(): View {
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        root.addView(
            TextView(this).apply {
                text = getString(R.string.input_view_hint)
                gravity = Gravity.CENTER
            }
        )
        root.addView(
            Button(this).apply {
                text = getString(R.string.input_view_switch)
                setOnClickListener {
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showInputMethodPicker()
                }
            }
        )
        return root
    }

    // ---------------------------------------------------------------------
    // Key handling
    // ---------------------------------------------------------------------

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (!isHardwareKey(event)) return super.onKeyDown(keyCode, event)

        // Modifier and lock keys are not characters and must not resolve a
        // pending accent. Without this, pressing Shift to type an uppercase
        // vowel would flush the accent before the vowel ever arrived.
        if (isModifierOrLock(keyCode)) return super.onKeyDown(keyCode, event)

        // Escape abandons a half-typed accent, the way it does on Windows.
        if (keyCode == KeyEvent.KEYCODE_ESCAPE && composer.hasPending) {
            composer.reset()
            return consume(keyCode)
        }

        // Backspace cancels the pending accent instead of deleting a character:
        // the accent was never committed, so deleting text as well would eat
        // something the person could still see on screen.
        if (keyCode == KeyEvent.KEYCODE_DEL && composer.hasPending) {
            composer.reset()
            return consume(keyCode)
        }

        val meta = event.metaState
        val altRight = meta and KeyEvent.META_ALT_RIGHT_ON != 0
        val ctrl = meta and KeyEvent.META_CTRL_ON != 0
        val alt = meta and KeyEvent.META_ALT_ON != 0
        val meta1 = meta and KeyEvent.META_META_ON != 0

        // AltGr is the right Alt key. Some keyboards and some Android builds
        // report it the Windows way instead, as Ctrl plus Alt together, so both
        // spellings are accepted.
        val altGr = altRight || (ctrl && alt)

        // Real Ctrl, left-Alt and Meta shortcuts belong to the app.
        if (!altGr && (ctrl || alt || meta1)) return passThrough(keyCode, event)

        val mapping = UsIntlTables.KEYS[keyCode] ?: return passThrough(keyCode, event)

        val shift = meta and KeyEvent.META_SHIFT_ON != 0
        val caps = meta and KeyEvent.META_CAPS_LOCK_ON != 0

        val out = UsIntlLayout.resolve(mapping, shift, caps, altGr)
            ?: return passThrough(keyCode, event)

        val text = when (out) {
            is Out.DeadOut -> composer.onDead(out.dead)
            is Out.Text -> composer.onText(out.s)
        }
        if (text != null && text.isNotEmpty()) {
            currentInputConnection?.commitText(text, 1)
        }
        return consume(keyCode)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (consumedKeys.remove(keyCode)) return true
        return super.onKeyUp(keyCode, event)
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /**
     * True only for events from a real alphabetic keyboard. Soft keyboard
     * events, D-pads, game controllers and remotes are left alone.
     */
    private fun isHardwareKey(event: KeyEvent): Boolean {
        if (event.deviceId == KeyCharacterMap.VIRTUAL_KEYBOARD) return false
        val device = InputDevice.getDevice(event.deviceId) ?: return false
        if (device.isVirtual) return false
        return device.keyboardType == InputDevice.KEYBOARD_TYPE_ALPHABETIC
    }

    private fun isModifierOrLock(keyCode: Int): Boolean =
        KeyEvent.isModifierKey(keyCode) ||
            keyCode == KeyEvent.KEYCODE_CAPS_LOCK ||
            keyCode == KeyEvent.KEYCODE_NUM_LOCK ||
            keyCode == KeyEvent.KEYCODE_SCROLL_LOCK

    /** Commit any pending accent, then hand the key back to the framework. */
    private fun passThrough(keyCode: Int, event: KeyEvent): Boolean {
        composer.flush()?.let { currentInputConnection?.commitText(it, 1) }
        return super.onKeyDown(keyCode, event)
    }

    private fun consume(keyCode: Int): Boolean {
        consumedKeys.add(keyCode)
        return true
    }
}
