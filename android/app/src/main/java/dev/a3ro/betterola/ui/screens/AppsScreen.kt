package dev.a3ro.betterola.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.a3ro.betterola.model.ModApp
import dev.a3ro.betterola.model.OnlineAppCatalog
import dev.a3ro.betterola.model.ScooterManager
import dev.a3ro.betterola.ui.components.*
import dev.a3ro.betterola.ui.theme.tokens
import dev.a3ro.betterola.util.ApkDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private data class Row(
    val key: String, val name: String, val note: String, val pkg: String,
    val install: suspend () -> Unit,
)

private sealed interface Pending {
    data class Uninstall(val row: Row) : Pending
    data class Reinstall(val row: Row) : Pending
}

/** Every mod in one list. A row is: name, what it is, and one action that fits its state. */
@Composable
fun AppsScreen(manager: ScooterManager, snack: SnackbarHostState) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val connected by manager.isConnected.collectAsState()
    var installed by remember { mutableStateOf<Set<String>>(emptySet()) }
    var busy by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf<Float?>(null) }
    var pending by remember { mutableStateOf<Pending?>(null) }

    fun refresh() = scope.launch { installed = runCatching { manager.fetchInstalledPackages() }.getOrDefault(emptySet()) }
    LaunchedEffect(connected) { if (connected) refresh() }

    suspend fun downloadAndInstall(url: String, file: String, name: String) {
        val dest = File(ctx.cacheDir, file)
        val ok = ApkDownloader.download(url, dest) { f, _, _ -> progress = f }
        progress = null
        if (!ok) error("Download failed. Check your phone's internet connection.")
        try { manager.installApkFile(dest, name) } finally { dest.delete() }
    }

    val rows = remember {
        listOf(
            ModApp.ANDROID_AUTO, ModApp.HUD, ModApp.BALLS, ModApp.MUSIC
        ).map { app ->
            Row(app.id, app.displayName, app.description, app.packageName) { manager.installSingleModApp(app) }
        } + OnlineAppCatalog.apps.map { a ->
            Row(a.id, a.displayName, a.description, a.packageName) { downloadAndInstall(a.downloadUrl, a.fileName, a.displayName) }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = "custom"
            try {
                val name = ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
                    val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (i >= 0 && c.moveToFirst()) c.getString(i) else null
                } ?: "app.apk"
                val out = File(ctx.cacheDir, "custom.apk")
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(uri)?.use { it.copyTo(out.outputStream()) }
                }
                manager.installApkFile(out, name)
                out.delete()
                snack.showSnackbar("Installed $name")
            } catch (e: Exception) {
                snack.showSnackbar("Install failed: ${e.message ?: "unknown error"}")
            }
            busy = null
            refresh()
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenTitle("Apps", if (connected) "${installed.size} packages on the scooter" else "Connect to the scooter to install")
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        rows.forEach { row ->
            val isIn = row.pkg in installed
            val working = busy == row.key
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
                Text(row.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(row.note, style = MaterialTheme.typography.bodyMedium, color = tokens.ink2)
                Spacer(Modifier.height(12.dp))
                if (working) {
                    LinearProgressIndicator(
                        progress = { progress ?: 0f }.takeIf { progress != null } ?: { 0f },
                        modifier = Modifier.fillMaxWidth(), color = tokens.amber, trackColor = tokens.hairline
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (isIn) {
                            QuietButton("Open", onClick = { scope.launch { manager.openApp(row.pkg, row.name) } })
                            QuietButton("Reinstall", onClick = { pending = Pending.Reinstall(row) }, enabled = busy == null)
                            TextButton(onClick = { pending = Pending.Uninstall(row) }, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text("Remove", color = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            QuietButton("Install", enabled = connected && busy == null, onClick = {
                                scope.launch {
                                    busy = row.key
                                    try {
                                        row.install()
                                        snack.showSnackbar("${row.name} installed. Keep the side stand up while you use it.")
                                    } catch (e: Exception) {
                                        snack.showSnackbar("Could not install ${row.name}: ${e.message ?: "unknown error"}")
                                    }
                                    busy = null
                                    refresh()
                                }
                            })
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }

        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Your own APK", style = MaterialTheme.typography.titleMedium)
            Text("Pick any .apk from your phone to install on the scooter.", style = MaterialTheme.typography.bodyMedium, color = tokens.ink2)
            QuietButton("Choose a file", onClick = { picker.launch("application/vnd.android.package-archive") }, enabled = connected && busy == null)
        }
        Spacer(Modifier.height(24.dp))
    }

    pending?.let { p ->
        val row = when (p) { is Pending.Uninstall -> p.row; is Pending.Reinstall -> p.row }
        val removing = p is Pending.Uninstall
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(if (removing) "Remove ${row.name}?" else "Reinstall ${row.name}?") },
            text = { Text(if (removing) "It will be uninstalled from the scooter." else "The current copy is removed first, then installed again.") },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    scope.launch {
                        busy = row.key
                        try {
                            if (removing) manager.uninstallPackage(row.pkg) else row.install()
                        } catch (e: Exception) {
                            snack.showSnackbar("Failed: ${e.message ?: "unknown error"}")
                        }
                        busy = null
                        refresh()
                    }
                }) { Text(if (removing) "Remove" else "Reinstall") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancel") } }
        )
    }
}
