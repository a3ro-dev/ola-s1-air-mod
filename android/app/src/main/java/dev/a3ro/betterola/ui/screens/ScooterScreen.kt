package dev.a3ro.betterola.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.a3ro.betterola.model.ModApp
import dev.a3ro.betterola.model.ScooterManager
import dev.a3ro.betterola.ui.components.*
import dev.a3ro.betterola.ui.theme.ArchivoWide
import dev.a3ro.betterola.ui.theme.tokens
import kotlinx.coroutines.launch

private const val HUD_PACKAGE = "dev.a3ro.betterola.hud"

@Composable
fun ScooterScreen(manager: ScooterManager, onOpenLog: () -> Unit, snack: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    val summary by manager.vehicleSummary.collectAsState()
    val live by manager.live.collectAsState()
    val ip by manager.scooterIp.collectAsState()
    val adbOpen by manager.adbOpen.collectAsState()
    val running by manager.isSetupRunning.collectAsState()
    val step by manager.currentStep.collectAsState()
    val success by manager.setupSuccess.collectAsState()
    val error by manager.errorMessage.collectAsState()
    var installed by remember { mutableStateOf<Set<String>>(emptySet()) }
    var confirmSetup by remember { mutableStateOf(false) }

    suspend fun refresh() {
        runCatching { manager.fetchVehicleSummary() }
        installed = runCatching { manager.fetchInstalledPackages() }.getOrDefault(emptySet())
    }
    LaunchedEffect(Unit) { refresh() }
    LaunchedEffect(success) { if (success) refresh() }

    val hasAuto = ModApp.ANDROID_AUTO.packageName in installed
    val hasBall = ModApp.BALLS.packageName in installed
    val hasHud = HUD_PACKAGE in installed
    val soc = live.socPercent

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenTitle(
            summary?.model ?: "Your scooter",
            listOfNotNull(summary?.moveOs, ip.takeIf { it.isNotBlank() }).joinToString("  ·  ")
        )

        // The one number. Unknown reads as unknown, never as zero.
        Column(Modifier.padding(horizontal = 20.dp).padding(top = 28.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    soc?.let { "%.0f".format(it) } ?: "--",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 104.sp, lineHeight = 92.sp)
                )
                Text("%", style = MaterialTheme.typography.headlineMedium.copy(fontFamily = ArchivoWide), modifier = Modifier.padding(bottom = 10.dp, start = 4.dp))
            }
            Spacer(Modifier.height(16.dp))
            DitherBar(
                fraction = soc?.let { (it / 100.0).toFloat().coerceIn(0f, 1f) },
                description = soc?.let { "Battery ${"%.0f".format(it)} percent" } ?: "Battery level not reported yet"
            )
            Spacer(Modifier.height(12.dp))
            Text(
                liveLine(live.rangeKm, live.driveMode, live.sideStandUp, live.charging),
                style = MaterialTheme.typography.bodyLarge, color = tokens.ink2
            )
        }

        Spacer(Modifier.height(32.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        InfoRow("Dash HUD", if (hasHud) "Installed" else "Not installed", valueColor = if (hasHud) tokens.ok else tokens.ink2)
        InfoRow("Android Auto", if (hasAuto) "Installed" else "Not installed", valueColor = if (hasAuto) tokens.ok else tokens.ink2)
        InfoRow("Floating nav ball", if (hasBall) "Installed" else "Not installed", valueColor = if (hasBall) tokens.ok else tokens.ink2)
        InfoRow(
            "Debug access (ADB)",
            if (adbOpen) "Open. Tap to close" else "Closed",
            valueColor = if (adbOpen) tokens.amber else tokens.ok,
            onClick = if (adbOpen) ({
                scope.launch {
                    val ok = runCatching { manager.closeAdb() }.getOrDefault(false)
                    snack.showSnackbar(if (ok) "ADB closed. The scooter is back in customer mode." else "Could not close ADB. A scooter restart does it.")
                }
            }) else null
        )

        Spacer(Modifier.height(28.dp))
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                running -> {
                    Text(step?.let { "Step ${it.stepNum} of 5: ${it.title}" } ?: "Working…", style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(
                        progress = { (step?.stepNum ?: 0) / 5f },
                        modifier = Modifier.fillMaxWidth(), color = tokens.amber, trackColor = tokens.hairline
                    )
                    QuietButton("Show details", onOpenLog)
                }
                !hasAuto || !hasBall -> {
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                    PrimaryButton("Set up Android Auto on the dash", onClick = { confirmSetup = true })
                    Text(
                        "Installs Android Auto and the floating buttons. Takes a minute or two.",
                        style = MaterialTheme.typography.bodyMedium, color = tokens.ink2
                    )
                }
                else -> {
                    HoldButton(
                        text = "Hold to open Android Auto on the dash",
                        doneText = "On the dash",
                        onConfirmed = {
                            scope.launch {
                                val ok = manager.openApp(ModApp.ANDROID_AUTO.packageName, ModApp.ANDROID_AUTO.displayName)
                                if (!ok) snack.showSnackbar("Could not open it. Is the scooter screen awake?")
                            }
                        }
                    )
                    QuietButton("Back to the scooter's own dashboard", onClick = { scope.launch { manager.openDashboardHome() } }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }

    if (confirmSetup) {
        AlertDialog(
            onDismissRequest = { confirmSetup = false },
            title = { Text("Before you start") },
            text = {
                Text(
                    "Side stand up, scooter screen awake, and the scooter parked. " +
                        "Setup opens debug access in memory only. Nothing is written to the scooter's system, and a restart undoes it."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmSetup = false
                    scope.launch { manager.runFullSetup(setOf(ModApp.BALLS, ModApp.ANDROID_AUTO)) }
                }) { Text("Start setup") }
            },
            dismissButton = { TextButton(onClick = { confirmSetup = false }) { Text("Not now") } }
        )
    }
}

private fun liveLine(range: Double?, mode: String?, standUp: Boolean?, charging: Boolean?): String {
    val parts = buildList {
        range?.let { add("${"%.0f".format(it)} km range") }
        mode?.let { add(it) }
        if (charging == true) add("Charging")
        when (standUp) {
            true -> add("Stand up")
            false -> add("Stand down")
            null -> {}
        }
    }
    return if (parts.isEmpty()) "Live battery and range appear once the scooter is awake." else parts.joinToString("  ·  ")
}
