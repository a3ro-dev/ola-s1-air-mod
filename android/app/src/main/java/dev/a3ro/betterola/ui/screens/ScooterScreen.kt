package dev.a3ro.betterola.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.a3ro.betterola.model.ModApp
import dev.a3ro.betterola.model.ScooterManager
import dev.a3ro.betterola.ui.components.*
import dev.a3ro.betterola.ui.theme.ArchivoWide
import dev.a3ro.betterola.ui.theme.tokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val HUD_PACKAGE = ModApp.HUD.packageName

@Composable
fun ScooterScreen(manager: ScooterManager, onOpenLog: () -> Unit, snack: SnackbarHostState) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val summary by manager.vehicleSummary.collectAsState()
    val live by manager.live.collectAsState()
    val signals by manager.signals.collectAsState()
    val ip by manager.scooterIp.collectAsState()
    val adbOpen by manager.adbOpen.collectAsState()
    val running by manager.isSetupRunning.collectAsState()
    val step by manager.currentStep.collectAsState()
    val success by manager.setupSuccess.collectAsState()
    val error by manager.errorMessage.collectAsState()
    var installed by remember { mutableStateOf<Set<String>>(emptySet()) }
    var confirmSetup by remember { mutableStateOf(false) }
    var confirmBoot by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf<String?>(null) }
    var aaHelp by remember { mutableStateOf(false) }
    var headunit by remember { mutableStateOf<Boolean?>(null) }

    suspend fun refresh() {
        runCatching { manager.fetchVehicleSummary() }
        installed = runCatching { manager.fetchInstalledPackages() }.getOrDefault(emptySet())
    }
    LaunchedEffect(Unit) { refresh() }
    LaunchedEffect(success) { if (success) refresh() }
    // Android Auto's headunit server lives on this phone; watch it so the dash link can say "ready".
    LaunchedEffect(Unit) {
        while (true) { headunit = manager.headunitServerUp(); delay(2000) }
    }

    fun openAndroidAuto() = scope.launch {
        if (manager.headunitServerUp()) {
            val ok = manager.openApp(ModApp.ANDROID_AUTO.packageName, ModApp.ANDROID_AUTO.displayName)
            if (!ok) snack.showSnackbar("Could not open it. Is the scooter screen awake?")
        } else {
            aaHelp = true
        }
    }

    fun vehicleCommand(what: String, call: suspend () -> Boolean) = scope.launch {
        busy = what
        val ok = runCatching { call() }.getOrDefault(false)
        busy = null
        snack.showSnackbar(
            if (ok) "$what sent." else "The scooter did not take it. Wake the screen and try again."
        )
    }

    val hasAuto = ModApp.ANDROID_AUTO.packageName in installed
    val hasBall = ModApp.BALLS.packageName in installed
    val hasHud = HUD_PACKAGE in installed
    val soc = live.socPercent

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenTitle(
            summary?.model ?: "Your scooter",
            listOfNotNull(summary?.moveOs, ip.takeIf { it.isNotBlank() }).joinToString("  ·  ")
        )

        // The one number, with the battery drawn as a column of cells beside it. Unknown reads as unknown, never zero.
        Row(Modifier.padding(horizontal = 20.dp).padding(top = 24.dp).height(232.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            DitherColumn(
                fraction = soc?.let { (it / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxHeight(),
                description = soc?.let { "Battery ${"%.0f".format(it)} percent" } ?: "Battery level not reported yet"
            )
            Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Bottom) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        soc?.let { "%.0f".format(it) } ?: "--",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 104.sp, lineHeight = 92.sp)
                    )
                    Text("%", style = MaterialTheme.typography.headlineMedium.copy(fontFamily = ArchivoWide), modifier = Modifier.padding(bottom = 10.dp, start = 4.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    liveLine(live.rangeKm, live.driveMode, live.sideStandUp, live.charging),
                    style = MaterialTheme.typography.bodyLarge, color = tokens.ink2
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        InfoRow("Dash HUD", if (hasHud) "Installed" else "Not installed", valueColor = if (hasHud) tokens.ok else tokens.ink2)
        InfoRow("Android Auto", if (hasAuto) "Installed" else "Not installed", valueColor = if (hasAuto) tokens.ok else tokens.ink2)
        InfoRow(
            "Android Auto link",
            when (headunit) {
                true -> "Headunit server on"
                false -> "Tap to start"
                null -> "Checking…"
            },
            valueColor = if (headunit == true) tokens.ok else tokens.warn,
            onClick = { openAndroidAuto() }
        )
        InfoRow("Floating nav ball", if (hasBall) "Installed" else "Not installed", valueColor = if (hasBall) tokens.ok else tokens.ink2)
        InfoRow(
            "Debug access (ADB)",
            if (adbOpen) "Open. Tap to close" else "Closed",
            valueColor = if (adbOpen) tokens.warn else tokens.ok,
            onClick = if (adbOpen) ({
                scope.launch {
                    val ok = runCatching { manager.closeAdb() }.getOrDefault(false)
                    snack.showSnackbar(if (ok) "ADB closed. The scooter is back in customer mode." else "Could not close ADB. Check the activity log.")
                }
            }) else null
        )

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Column(Modifier.padding(horizontal = 20.dp).padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Lock, unlock, boot", style = MaterialTheme.typography.titleMedium)
            Text(
                when {
                    signals.speedKmh == null -> "Waiting for the scooter's state."
                    signals.speedKmh != 0 -> "Stop the scooter to use these."
                    live.unlocked == true -> "Unlocked. The boot is ${if (live.bootOpen == true) "open" else "closed"}."
                    live.unlocked == false -> "Locked."
                    else -> "Waiting for the scooter's lock state."
                },
                style = MaterialTheme.typography.bodyMedium, color = tokens.ink2
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuietButton(
                    "Lock", onClick = { vehicleCommand("Lock") { manager.lockScooter() } },
                    modifier = Modifier.weight(1f),
                    enabled = busy == null && signals.speedKmh == 0 && live.unlocked == true
                )
                QuietButton(
                    "Unlock", onClick = { vehicleCommand("Unlock") { manager.unlockScooter() } },
                    modifier = Modifier.weight(1f),
                    enabled = busy == null && signals.speedKmh == 0 && live.unlocked == false
                )
                QuietButton(
                    "Boot", onClick = { confirmBoot = true },
                    modifier = Modifier.weight(1f),
                    enabled = busy == null && signals.speedKmh == 0 && live.unlocked == true && live.bootOpen != true
                )
            }
            if (busy != null) Text("$busy…", style = MaterialTheme.typography.bodyMedium, color = tokens.warn)
        }

        Spacer(Modifier.height(28.dp))
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                running -> {
                    Text(step?.let { "Step ${it.stepNum} of 5: ${it.title}" } ?: "Working…", style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(
                        progress = { (step?.stepNum ?: 0) / 5f },
                        modifier = Modifier.fillMaxWidth(), color = tokens.live, trackColor = tokens.hairline
                    )
                    QuietButton("Show details", onOpenLog)
                }
                !hasAuto || !hasBall || !hasHud -> {
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                    PrimaryButton(if (hasAuto && hasBall) "Add the dash HUD" else "Set up Android Auto and the HUD", onClick = { confirmSetup = true })
                    Text(
                        "Installs Android Auto, the floating buttons and the HUD. Takes a minute or two.",
                        style = MaterialTheme.typography.bodyMedium, color = tokens.ink2
                    )
                }
                else -> {
                    HoldButton(
                        text = "Hold to open Android Auto on the dash",
                        doneText = "On the dash",
                        onConfirmed = { openAndroidAuto() }
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
                    scope.launch { manager.runFullSetup(setOf(ModApp.BALLS, ModApp.ANDROID_AUTO, ModApp.HUD)) }
                }) { Text("Start setup") }
            },
            dismissButton = { TextButton(onClick = { confirmSetup = false }) { Text("Not now") } }
        )
    }

    if (aaHelp) {
        AlertDialog(
            onDismissRequest = { aaHelp = false },
            title = { Text("One step in Android Auto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Android Auto 17.4 and later stopped apps from starting Self-Mode for you. The headunit server is the way that still works.")
                    Text("1. Open Android Auto settings.")
                    Text("2. Tap ⋮ at the top right, then Start headunit server.")
                    Text("If the option is missing: scroll to the bottom, tap Version ten times, then reopen the menu.")
                    Text("Come back here. This app notices and opens Android Auto on the scooter.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    aaHelp = false
                    openAaSettings(ctx)
                    scope.launch {
                        val up = manager.awaitHeadunitServer()
                        if (up) {
                            val ok = manager.openApp(ModApp.ANDROID_AUTO.packageName, ModApp.ANDROID_AUTO.displayName)
                            if (!ok) snack.showSnackbar("Server is on, but the scooter did not open Android Auto.")
                        } else {
                            snack.showSnackbar("Headunit server is still off. Start it in Android Auto settings.")
                        }
                    }
                }) { Text("Open Android Auto settings") }
            },
            dismissButton = { TextButton(onClick = { aaHelp = false }) { Text("Not now") } }
        )
    }

    if (confirmBoot) {
        AlertDialog(
            onDismissRequest = { confirmBoot = false },
            title = { Text("Open the boot?") },
            text = { Text("The seat latch releases. The scooter has to be parked and unlocked.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmBoot = false
                    vehicleCommand("Boot release") { manager.openBoot() }
                }) { Text("Open") }
            },
            dismissButton = { TextButton(onClick = { confirmBoot = false }) { Text("Cancel") } }
        )
    }
}

private fun openAaSettings(ctx: Context) {
    val direct = Intent("com.google.android.projection.gearhead.SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { ctx.startActivity(direct) }.onFailure {
        runCatching {
            ctx.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:com.google.android.projection.gearhead"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
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
