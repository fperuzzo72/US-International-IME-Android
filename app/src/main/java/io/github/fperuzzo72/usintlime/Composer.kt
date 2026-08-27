package io.github.fperuzzo72.usintlime

/**
 * The US-International dead key state machine.
 *
 * Pure Kotlin on purpose: no Android types, so the whole composition behaviour
 * can be exercised by plain JVM unit tests. [UsIntlIme] is the only thing that
 * knows about [android.view.KeyEvent].
 *
 * Windows semantics, as implemented here:
 *
 *  - A dead key commits nothing. It waits.
 *  - The next character either composes with it (`'` then `a` gives a-acute) or,
 *    if the pair is not in the table, the dead key is emitted as its own literal
 *    followed by that character (`'` then `q` gives `'q`).
 *  - Space is a normal table entry: `'` then space gives `'`.
 *  - A dead key pressed while another one is pending emits both literals and
 *    clears the state, so `''` gives two apostrophes and `''a` gives `''a`,
 *    not `'a-acute`. See docs/DEADKEY_TABLE_UPDATE.md for why this case is
 *    called out separately.
 */
class Composer {

    private var pending: Dead? = null

    val hasPending: Boolean
        get() = pending != null

    /** Drop any half-finished composition, e.g. on focus change or Escape. */
    fun reset() {
        pending = null
    }

    /**
     * Feed a literal string from the layout.
     *
     * @return the text to commit, never null: composition always resolves.
     */
    fun onText(text: String): String {
        val dead = pending ?: return text
        pending = null
        if (text.length == 1) {
            val composed = UsIntlTables.COMPOSE[dead]?.get(text[0])
            if (composed != null) return composed
        }
        return dead.literal + text
    }

    /**
     * Feed a dead key from the layout.
     *
     * @return text to commit right now, or null when the dead key was simply
     *         stored and nothing should be inserted yet.
     */
    fun onDead(dead: Dead): String? {
        val previous = pending
        if (previous != null) {
            pending = null
            return "" + previous.literal + dead.literal
        }
        pending = dead
        return null
    }

    /**
     * Resolve a pending dead key against a key this layout does not produce a
     * character for, such as Enter or an arrow key.
     *
     * @return the dead key's literal to commit before the other key is handled,
     *         or null when nothing was pending.
     */
    fun flush(): String? {
        val dead = pending ?: return null
        pending = null
        return dead.literal.toString()
    }
}
