package dev.a3ro.betterola.hud

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings

/** Brings the HUD back after the head unit reboots, if the overlay permission is still granted. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && Settings.canDrawOverlays(context)) {
            runCatching { context.startForegroundService(Intent(context, HudService::class.java)) }
        }
    }
}
