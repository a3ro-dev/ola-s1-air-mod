package dev.a3ro.betterola.vehicle

import kotlin.math.max

/**
 * Read-only decoding of the scooter's own broadcast frames.
 *
 * Bit positions come from the signal table in itsmobsys/ola-s1-rea (originally Axiom, Apache-2.0),
 * see legal/THIRD-PARTY.md. Every signal here is Intel byte order, unsigned.
 * Nothing in this file builds or sends a frame.
 */
object Ids {
    const val DISPLAY_INFO = 0x353      // indicators, beams, side stand, range
    const val DISPLAY_SOC = 0x352
    const val DISPLAY_SPEED = 0x354
    const val DISCHARGE_MODE = 0x113    // drive mode word
    const val BRAKES = 0x15B
    const val SAFETY = 0x683            // regen availability
    const val LOCK_STATE = 0x144
    const val THROTTLE_STATUS = 0x210   // derate warning
    const val MCU_CURRENT = 0x223       // motor current
    const val CHARGER_PLUG = 0x62D
    const val BRAKE_ANALOG = 0x320      // front/rear brake lever voltage, for intensity
}

/** One 16-byte CANPacket.can_frame: id big-endian [0..4), dlc [4], data [8..16). */
class Frame(val id: Int, val data: ByteArray)

fun parseFrame(raw: ByteArray?): Frame? {
    if (raw == null || raw.size < 16) return null
    val id = ((raw[0].toInt() and 0xFF) shl 24) or ((raw[1].toInt() and 0xFF) shl 16) or
        ((raw[2].toInt() and 0xFF) shl 8) or (raw[3].toInt() and 0xFF)
    if (id < 0 || id > 0x1FFFFFFF) return null
    return Frame(id, raw.copyOfRange(8, 16))
}

/** Little-endian bit field: [start] is the LSB position counted from bit 0 of byte 0. */
fun bits(d: ByteArray, start: Int, len: Int): Long {
    var v = 0L
    for (i in 0 until len) {
        val bit = start + i
        val byte = bit ushr 3
        if (byte >= d.size) break
        if ((d[byte].toInt() ushr (bit and 7)) and 1 == 1) v = v or (1L shl i)
    }
    return v
}

enum class DriveMode(val label: String) {
    Parked("Park"), Normal("Normal"), Sport("Sport"), Custom("Custom"),
    Reverse("Easy Park"), Eco("Eco"), Hyper("Hyper"), Unknown("");

    companion object {
        fun from(word: Int) = when (word) {
            0 -> Parked; 2 -> Normal; 3 -> Sport; 4 -> Custom
            5, 10, 13 -> Reverse; 6 -> Eco; 7 -> Hyper
            else -> Unknown
        }
    }
}

/** Everything the HUD draws. Null / false until a frame carrying it arrives. */
data class HudState(
    val leftIndicator: Boolean = false,
    val rightIndicator: Boolean = false,
    val lowBeam: Boolean = false,
    val highBeam: Boolean = false,
    val speedKmh: Int? = null,
    val socPercent: Int? = null,
    val rangeKm: Int? = null,
    val mode: DriveMode = DriveMode.Unknown,
    val brake: Boolean = false,
    val sideStandDown: Boolean? = null,
    val regenAvailable: Boolean? = null,
    val brakeVolts: Double? = null,   // larger of front/rear lever voltage; scaled to 0..1 by the view
    val derate: Boolean = false,
    val unlocked: Boolean? = null,
    val bootOpen: Boolean? = null,
    val charging: Boolean = false,
    val motorAmps: Double? = null,
    val live: Boolean = false,      // the tap is connected and frames are flowing
    val lastFrameMs: Long = 0L,
) {
    val hazard get() = leftIndicator && rightIndicator

    /** A decoded frame returns a new state only when something changed, to keep allocation low. */
    fun apply(f: Frame, nowMs: Long): HudState {
        val d = f.data
        val next = when (f.id) {
            Ids.DISPLAY_INFO -> copy(
                rightIndicator = bits(d, 40, 1) == 1L,
                leftIndicator = bits(d, 41, 1) == 1L,
                highBeam = bits(d, 42, 1) == 1L,
                lowBeam = bits(d, 50, 1) == 1L,
                // Side_Stand_Status is 0 when the stand is up (see ola-s1-rea Live.sideStandUp).
                sideStandDown = bits(d, 0, 2) != 0L,
                // Seat_Lock_Status is 0 when the seat latch is released, same convention as the stand.
                bootOpen = bits(d, 2, 1) == 0L,
                rangeKm = bits(d, 24, 16).toInt(),
            )
            Ids.DISPLAY_SOC -> copy(socPercent = bits(d, 0, 16).toInt().coerceIn(0, 100))
            Ids.DISPLAY_SPEED -> copy(speedKmh = bits(d, 0, 8).toInt())
            Ids.DISCHARGE_MODE -> copy(mode = DriveMode.from(bits(d, 41, 4).toInt()))
            Ids.BRAKE_ANALOG -> copy(brakeVolts = max(bits(d, 0, 16), bits(d, 16, 16)) * 0.01)
            Ids.BRAKES -> copy(brake = bits(d, 4, 1) == 1L || bits(d, 5, 1) == 1L)
            Ids.SAFETY -> copy(regenAvailable = bits(d, 8, 4) == 1L)
            Ids.LOCK_STATE -> copy(unlocked = bits(d, 0, 3) != 0L)
            Ids.THROTTLE_STATUS -> copy(derate = bits(d, 1, 1) == 1L)
            Ids.MCU_CURRENT -> copy(motorAmps = bits(d, 40, 16) * 0.01 - 300.0)
            Ids.CHARGER_PLUG -> copy(charging = bits(d, 2, 1) == 1L)
            else -> return this
        }
        return if (next == this) this else next.copy(live = true, lastFrameMs = nowMs)
    }
}
