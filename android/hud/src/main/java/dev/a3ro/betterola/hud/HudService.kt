package dev.a3ro.betterola.hud

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Holds the overlay window over whatever is on screen (Android Auto included).
 * The window takes no touches and no focus, so it can never get in the way of driving the phone-projection UI.
 */
class HudService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var view: HudView? = null
    private var tap: Job? = null
    private var watchdog: Job? = null
    @Volatile private var lastFrame = 0L

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        startForeground()
        if (view == null) attach(preview = intent?.getBooleanExtra(EXTRA_PREVIEW, false) == true)
        return START_STICKY
    }

    private fun startForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("hud", "Dash HUD", NotificationManager.IMPORTANCE_MIN))
        val n = Notification.Builder(this, "hud").setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("Dash HUD running").build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
    }

    private fun attach(preview: Boolean) {
        if (!android.provider.Settings.canDrawOverlays(this)) { stopSelf(); return }
        val v = HudView(this)
        view = v
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }
        getSystemService(WindowManager::class.java).addView(v, lp)

        if (preview) {
            v.stale = false
            tap = Sim.run(scope) { v.state = it }
            return
        }
        var state = HudState()
        tap = CanTap().start(scope, onFrame = { f ->
            lastFrame = System.currentTimeMillis()
            val next = state.apply(f, lastFrame)
            if (next !== state) { state = next; v.post { v.state = next } }
        }, onConnected = { up -> if (!up) v.post { v.stale = true } })
        // "stale" = no useful frame for 3 s: the scooter is asleep, or the tap dropped.
        watchdog = scope.launch {
            while (isActive) {
                val s = System.currentTimeMillis() - lastFrame > 3_000
                if (v.stale != s) v.stale = s
                delay(500)
            }
        }
    }

    override fun onDestroy() {
        tap?.cancel(); watchdog?.cancel(); scope.cancel()
        view?.let { runCatching { getSystemService(WindowManager::class.java).removeView(it) } }
        view = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "dev.a3ro.betterola.hud.STOP"
        const val EXTRA_PREVIEW = "preview"
    }
}
