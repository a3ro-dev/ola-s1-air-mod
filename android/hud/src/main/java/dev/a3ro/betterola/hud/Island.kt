package dev.a3ro.betterola.hud

enum class Level { Info, Warn, Fault }

data class Message(val text: String, val level: Level)

/**
 * The one line the island expands to, or null to stay compact.
 * Highest priority first. These mirror what the scooter's own cluster would tell the rider;
 * the real cluster is still the authority.
 */
fun message(s: HudState, stale: Boolean): Message? = when {
    stale && s.speedKmh == null -> Message("Waiting for the scooter", Level.Info)
    stale -> Message("Scooter data stopped", Level.Warn)
    s.derate -> Message("Power reduced", Level.Warn)
    s.sideStandDown == true && (s.speedKmh ?: 0) > 3 -> Message("Side stand down", Level.Fault)
    s.sideStandDown == true && s.mode != DriveMode.Parked -> Message("Raise the side stand", Level.Warn)
    s.charging -> Message("Charging", Level.Info)
    // Inferred from the lock word; verify against the real cluster text on your unit.
    s.unlocked == false -> Message("Press the power button and brake to unlock", Level.Info)
    s.hazard -> Message("Hazard lights", Level.Info)
    else -> null
}
