package dev.a3ro.betterola.ui.screens

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import dev.a3ro.betterola.model.ScooterManager
import dev.a3ro.betterola.ui.components.*
import dev.a3ro.betterola.ui.theme.tokens
import dev.a3ro.betterola.vehicle.HudState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Shortcuts = listOf(
    "Wi-Fi" to "android.settings.WIFI_SETTINGS",
    "Bluetooth" to "android.settings.BLUETOOTH_SETTINGS",
    "Display" to "android.settings.DISPLAY_SETTINGS",
    "Sound" to "android.settings.SOUND_SETTINGS",
    "Apps" to "android.settings.APPLICATION_SETTINGS",
    "Date and time" to "android.settings.DATE_SETTINGS",
)

private val Presets = listOf("id", "uptime", "getprop ro.build.display.id", "pm list packages -3", "df -h /data")

@Composable
fun ToolsScreen(manager: ScooterManager, snack: SnackbarHostState, onOpenLog: () -> Unit, onCheckUpdates: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val connected by manager.isConnected.collectAsState()
    var shot by remember { mutableStateOf<Bitmap?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var command by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenTitle("Tools", "For checking on the scooter. Nothing here changes how it rides.")

        Section("Dash screenshot")
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            QuietButton("Take a screenshot", enabled = connected, onClick = {
                scope.launch {
                    shot = runCatching { manager.captureScreenshot() }.onFailure {
                        snack.showSnackbar("Screenshot failed: ${it.message ?: "unknown error"}")
                    }.getOrNull()
                }
            })
            shot?.let { bmp ->
                Image(bmp.asImageBitmap(), contentDescription = "Screenshot of the scooter's display", modifier = Modifier.fillMaxWidth())
                QuietButton("Save to Pictures", onClick = {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) { savePng(ctx, bmp) }
                        snack.showSnackbar(if (ok) "Saved to Pictures/better-ola" else "Could not save it")
                    }
                })
            }
        }

        Section("Live signals")
        LiveSignals(manager)

        Section("Open on the scooter")
        FlowShortcuts(connected) { action ->
            scope.launch {
                if (!manager.openSettings(action)) snack.showSnackbar("Could not open that screen")
            }
        }

        Section("About this scooter")
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            QuietButton("Read system info", enabled = connected, onClick = {
                scope.launch {
                    info = runCatching {
                        val i = manager.fetchScooterInfo()
                        val ram = manager.fetchRamInfo()
                        buildString {
                            appendLine("Android ${i.androidVersion}")
                            appendLine("Build ${i.buildId}")
                            appendLine("Model ${i.model}")
                            appendLine("Security mode ${i.scooterMode}")
                            if (ram != null) append("Memory ${ram.usedMb} of ${ram.totalMb} MB in use")
                        }
                    }.getOrElse { "Could not read: ${it.message}" }
                }
            })
            info?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = tokens.ink2) }
        }

        Section("Root shell")
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Runs as root on the scooter's head unit. Read things here. Anything that writes can break the dash.",
                style = MaterialTheme.typography.bodyMedium, color = tokens.ink2
            )
            OutlinedTextField(
                value = command, onValueChange = { command = it }, label = { Text("Command") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuietButton("Run", enabled = connected && command.isNotBlank(), onClick = {
                    scope.launch { output = runCatching { manager.runRootShell(command) }.getOrElse { "Error: ${it.message}" } }
                })
                if (output.isNotEmpty()) QuietButton("Copy output", onClick = { clipboard.setText(AnnotatedString(output)) })
            }
            FlowPresets { command = it }
            if (output.isNotEmpty()) {
                SelectionContainer {
                    Text(
                        output, fontFamily = Mono, style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(12.dp)
                    )
                }
            }
        }

        Section("This app")
        InfoRow("Check for updates", "Now", onClick = onCheckUpdates)
        InfoRow("Activity log", "Open", onClick = onOpenLog)
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title, style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 12.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowShortcuts(enabled: Boolean, onClick: (String) -> Unit) {
    FlowRow(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Shortcuts.forEach { (label, action) -> QuietButton(label, enabled = enabled, onClick = { onClick(action) }) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowPresets(onPick: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Presets.forEach { p -> AssistChip(onClick = { onPick(p) }, label = { Text(p, fontFamily = Mono) }) }
    }
}

private fun savePng(ctx: android.content.Context, bmp: Bitmap): Boolean = try {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "better-ola-${System.currentTimeMillis()}.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/better-ola")
    }
    val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    uri?.let { ctx.contentResolver.openOutputStream(it)?.use { o -> bmp.compress(Bitmap.CompressFormat.PNG, 100, o) } }
    uri != null
} catch (_: Exception) { false }

/** The log used to be the main screen. It is now a sheet for when something goes wrong. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogSheet(manager: ScooterManager, onDismiss: () -> Unit) {
    val logs by manager.logs.collectAsState()
    val clipboard = LocalClipboardManager.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Activity log", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { clipboard.setText(AnnotatedString(logs.joinToString("\n"))) }) { Text("Copy all") }
        }
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp).padding(horizontal = 20.dp)) {
            items(logs) { Text(it, fontFamily = Mono, style = MaterialTheme.typography.labelSmall, color = tokens.ink2) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Raw decoded values, so you can check each one against the scooter's own dash. */
@Composable
private fun LiveSignals(manager: ScooterManager) {
    val s by manager.signals.collectAsState()
    val frames by manager.tapFrames.collectAsState()
    fun yn(b: Boolean?) = when (b) { true -> "yes"; false -> "no"; null -> "--" }
    Column {
        InfoRow("Data from the scooter", if (frames > 0) "$frames frames" else "Nothing yet")
        InfoRow("Speed", s.speedKmh?.let { "$it km/h" } ?: "--")
        InfoRow("Battery", s.socPercent?.let { "$it %" } ?: "--")
        InfoRow("Range", s.rangeKm?.let { "$it km" } ?: "--")
        InfoRow("Mode", s.mode.label.ifEmpty { "--" })
        InfoRow("Left indicator", yn(s.leftIndicator))
        InfoRow("Right indicator", yn(s.rightIndicator))
        InfoRow("Brake", if (s.brake) "pulled" else "released")
        InfoRow("Brake lever voltage", s.brakeVolts?.let { "%.2f V".format(it) } ?: "--")
        InfoRow("Side stand down", yn(s.sideStandDown))
        InfoRow("Regen available", yn(s.regenAvailable))
        InfoRow("Unlocked", yn(s.unlocked))
        InfoRow("Charging", yn(s.charging))
        InfoRow("Motor current", s.motorAmps?.let { "%.1f A".format(it) } ?: "--")
    }
}
