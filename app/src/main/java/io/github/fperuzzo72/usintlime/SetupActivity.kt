package io.github.fperuzzo72.usintlime

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * A setup screen, not a settings screen. It has nothing to configure.
 *
 * Turning on a keyboard is the one genuinely awkward part of installing an IME:
 * it lives across two different Android settings surfaces, and the second one is
 * a modal you cannot deep-link to. This walks the three steps in order, shows
 * which are already done, and gets out of the way afterwards.
 */
class SetupActivity : Activity() {

    private lateinit var steps: List<Step>

    private class Step(
        val number: Int,
        val titleRes: Int,
        val bodyRes: Int,
        val buttonRes: Int,
        val optional: Boolean = false,
        val isDone: (SetupActivity) -> Boolean,
        val onClick: (SetupActivity) -> Unit,
    ) {
        lateinit var heading: TextView
        lateinit var button: Button
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildView())
    }

    override fun onResume() {
        super.onResume()
        // Every step sends the person into a system screen and brings them back,
        // so the state is only ever right if it is re-read here.
        refresh()
    }

    // ---------------------------------------------------------------------
    // State
    // ---------------------------------------------------------------------

    /** The IME is switched on in the system list, but not necessarily in use. */
    private fun isEnabled(): Boolean {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.enabledInputMethodList.any { it.packageName == packageName }
    }

    /** The IME is the one currently receiving keys. */
    private fun isSelected(): Boolean {
        val current = Settings.Secure.getString(
            contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD
        )
        return current != null && current.startsWith("$packageName/")
    }

    private fun refresh() {
        for (step in steps) {
            val done = step.isDone(this)
            step.heading.text = getString(
                if (done) R.string.setup_step_done else R.string.setup_step,
                step.number,
                getString(step.titleRes),
            )
            // Steps stay tappable when done: the picker is also how you switch
            // back, and the layout screen is worth revisiting.
            step.button.isEnabled = true
        }
    }

    // ---------------------------------------------------------------------
    // Actions
    // ---------------------------------------------------------------------

    private fun openImeSettings() = open(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))

    private fun openPicker() {
        if (!isEnabled()) {
            Toast.makeText(this, R.string.setup_enable_first, Toast.LENGTH_LONG).show()
            return
        }
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showInputMethodPicker()
    }

    /**
     * The physical keyboard layout screen. Not every device ships it, e-ink
     * vendors especially, so this falls back to the input method settings.
     */
    private fun openHardKeyboardSettings() {
        val intent = Intent("android.settings.HARD_KEYBOARD_SETTINGS")
        if (intent.resolveActivity(packageManager) != null) {
            open(intent)
        } else {
            Toast.makeText(this, R.string.setup_no_hard_keyboard_screen, Toast.LENGTH_LONG).show()
            openImeSettings()
        }
    }

    private fun open(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            Toast.makeText(this, R.string.setup_screen_missing, Toast.LENGTH_LONG).show()
        }
    }

    // ---------------------------------------------------------------------
    // View
    // ---------------------------------------------------------------------

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun buildView(): View {
        steps = listOf(
            Step(
                number = 1,
                titleRes = R.string.setup_1_title,
                bodyRes = R.string.setup_1_body,
                buttonRes = R.string.setup_1_button,
                isDone = { it.isEnabled() },
                onClick = { it.openImeSettings() },
            ),
            Step(
                number = 2,
                titleRes = R.string.setup_2_title,
                bodyRes = R.string.setup_2_body,
                buttonRes = R.string.setup_2_button,
                isDone = { it.isSelected() },
                onClick = { it.openPicker() },
            ),
            Step(
                number = 3,
                titleRes = R.string.setup_3_title,
                bodyRes = R.string.setup_3_body,
                buttonRes = R.string.setup_3_button,
                optional = true,
                // Nothing readable tells us which physical layout is selected,
                // so this one never claims to be done.
                isDone = { false },
                onClick = { it.openHardKeyboardSettings() },
            ),
        )

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(28))
        }

        column.addView(text(R.string.app_name, sizeSp = 24f, bold = true))
        column.addView(text(R.string.setup_intro, sizeSp = 15f, topMargin = dp(8)))

        for (step in steps) {
            column.addView(stepView(step))
        }

        column.addView(
            text(R.string.setup_footer, sizeSp = 13f, topMargin = dp(28))
        )

        return ScrollView(this).apply {
            addView(
                column,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            )
        }
    }

    private fun stepView(step: Step): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(28) }
        }

        step.heading = text(step.titleRes, sizeSp = 17f, bold = true)
        box.addView(step.heading)

        val bodyText = if (step.optional) {
            getString(R.string.setup_optional, getString(step.bodyRes))
        } else {
            getString(step.bodyRes)
        }
        box.addView(
            TextView(this).apply {
                text = bodyText
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setPadding(0, dp(4), 0, 0)
            }
        )

        step.button = Button(this).apply {
            setText(step.buttonRes)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(8) }
            setOnClickListener { step.onClick(this@SetupActivity) }
        }
        box.addView(step.button)
        return box
    }

    private fun text(
        res: Int,
        sizeSp: Float,
        bold: Boolean = false,
        topMargin: Int = 0,
    ) = TextView(this).apply {
        setText(res)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        gravity = Gravity.START
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { this.topMargin = topMargin }
    }
}
