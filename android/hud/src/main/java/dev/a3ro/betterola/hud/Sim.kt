package dev.a3ro.betterola.hud

import dev.a3ro.betterola.vehicle.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** A scripted ride for checking the HUD without a scooter. Started only by an explicit intent extra. */
object Sim {
    fun run(scope: CoroutineScope, push: (HudState) -> Unit) = scope.launch {
        var s = HudState(live = true, socPercent = 64, rangeKm = 71, mode = DriveMode.Normal,
            sideStandDown = false, unlocked = true, speedKmh = 0)
        var t = 0
        while (isActive) {
            val sec = t / 10.0
            val phase = (sec % 24).toInt()
            val blink = (t / 5) % 2 == 0   // 1 Hz
            s = s.copy(
                speedKmh = when (phase) { in 0..3 -> 0; in 4..9 -> 18 + (phase - 4) * 6; in 10..15 -> 48; in 16..19 -> 30; else -> 0 },
                leftIndicator = phase in 6..9 && blink,
                rightIndicator = phase in 14..17 && blink,
                brake = phase in 18..19,
                motorAmps = when (phase) { in 4..9 -> 60.0; in 10..15 -> 22.0; in 16..17 -> -25.0; else -> 0.0 },
                mode = when (phase) { in 0..3 -> DriveMode.Parked; in 20..23 -> DriveMode.Reverse; in 12..19 -> DriveMode.Sport; else -> DriveMode.Normal },
                lowBeam = phase in 4..9 || phase in 16..23,
                highBeam = phase in 10..15,
                derate = false,
                sideStandDown = phase in 21..23,
                charging = false,
            )
            push(s)
            t++
            delay(100)
        }
    }
}
