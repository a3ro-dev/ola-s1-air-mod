package dev.a3ro.betterola.hud

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast

/** Launcher entry: makes sure the overlay is allowed, starts the service, and closes. No UI of its own. */
class HudActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Allow \"Display over other apps\" for Dash HUD, then open it again.", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } else {
            val i = Intent(this, HudService::class.java).putExtra(HudService.EXTRA_PREVIEW, intent.getBooleanExtra(HudService.EXTRA_PREVIEW, false))
            startForegroundService(i)
        }
        finish()
    }
}
