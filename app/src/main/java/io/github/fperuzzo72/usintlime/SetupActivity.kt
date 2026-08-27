package io.github.fperuzzo72.usintlime

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings

/**
 * Not a settings screen. This app has none.
 *
 * It exists so the launcher icon leads somewhere useful after a sideload: it
 * opens the system's own input method settings, where the IME is enabled and
 * selected, and then gets out of the way.
 */
class SetupActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(
            Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }
}
