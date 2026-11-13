package dev.a3ro.betterola.hud

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import dev.a3ro.betterola.vehicle.HmiCommand
import dev.a3ro.betterola.vehicle.HudState

/**
 * Three stock HMI commands in a small touchable window of their own.
 * The main HUD window stays untouchable, so Android Auto keeps every tap except
 * inside this bar. It only appears while the scooter is stopped and the data is live.
 */
class ControlsBar(context: Context, private val onCommand: (Int) -> Unit) : LinearLayout(context) {

    private var busy = false
    private var state = HudState()
    private var stale = true

    private val lock = button("Lock", HmiCommand.LOCK)
    private val unlock = button("Unlock", HmiCommand.UNLOCK)
    private val boot = button("Boot", HmiCommand.BOOT)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val d = resources.displayMetrics.density
        background = GradientDrawable().apply {
            cornerRadius = 22f * d
            setColor(Color.argb(235, 0x0B, 0x0C, 0x0E))
            setStroke((1f * d).toInt(), Color.argb(60, 0xFF, 0xFF, 0xFF))
        }
        setPadding(dp(6), dp(4), dp(6), dp(4))
        addView(lock)
        addView(unlock)
        addView(boot)
        visibility = GONE
    }

    fun update(s: HudState, isStale: Boolean) {
        state = s
        stale = isStale
        apply()
    }

    fun sending() {
        busy = true
        apply()
    }

    fun done() {
        busy = false
        apply()
    }

    private fun apply() {
        val parked = !stale && state.speedKmh == 0
        visibility = if (parked) VISIBLE else GONE
        lock.isEnabled = parked && !busy && state.unlocked == true
        unlock.isEnabled = parked && !busy && state.unlocked == false
        boot.isEnabled = parked && !busy && state.unlocked == true && state.bootOpen != true
        lock.alpha = if (lock.isEnabled) 1f else 0.35f
        unlock.alpha = if (unlock.isEnabled) 1f else 0.35f
        boot.alpha = if (boot.isEnabled) 1f else 0.35f
    }

    private fun button(label: String, cmd: Int): TextView = TextView(context).apply {
        text = label.uppercase()
        setTextColor(Color.rgb(0xEC, 0xEE, 0xF0))
        textSize = 12f
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        gravity = Gravity.CENTER
        setPadding(dp(14), dp(12), dp(14), dp(12))
        isClickable = true
        isFocusable = false
        setOnClickListener {
            if (!busy) {
                busy = true
                apply()
                onCommand(cmd)
            }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
