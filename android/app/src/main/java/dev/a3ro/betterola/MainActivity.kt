package dev.a3ro.betterola

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.ElectricScooter
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.a3ro.betterola.model.ScooterManager
import dev.a3ro.betterola.ui.screens.*
import dev.a3ro.betterola.ui.theme.BetterOlaTheme
import dev.a3ro.betterola.ui.theme.tokens
import dev.a3ro.betterola.util.ApkInstaller
import dev.a3ro.betterola.util.AppRelease
import dev.a3ro.betterola.util.UpdateChecker
import kotlinx.coroutines.launch
import java.io.File

private enum class Tab(val title: String, val icon: ImageVector) {
    Scooter("Scooter", Icons.Outlined.ElectricScooter),
    Apps("Apps", Icons.Outlined.Apps),
    Tools("Tools", Icons.Outlined.Build),
}

class MainActivity : ComponentActivity() {
    private lateinit var manager: ScooterManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        manager = ScooterManager(applicationContext)
        setContent { BetterOlaTheme { App(manager) } }
    }

    override fun onDestroy() {
        super.onDestroy()
        manager.disconnect()
    }
}

@Composable
private fun App(manager: ScooterManager) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { ctx.getSharedPreferences("betterola_prefs", android.content.Context.MODE_PRIVATE) }
    val connected by manager.isConnected.collectAsState()
    var tab by rememberSaveable { mutableStateOf(Tab.Scooter) }
    var skipped by rememberSaveable { mutableStateOf(false) }
    var booting by remember { mutableStateOf(manager.scooterIp.value.isNotBlank()) }
    var showAbout by remember { mutableStateOf(false) }
    var showLog by remember { mutableStateOf(false) }
    var update by remember { mutableStateOf<AppRelease?>(null) }
    var updating by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<Float?>(null) }
    var apkFile by remember { mutableStateOf<File?>(null) }
    var needInstallPermission by remember { mutableStateOf(false) }
    val snack = remember { SnackbarHostState() }

    // The last scooter is remembered, so opening the app just reconnects.
    LaunchedEffect(Unit) {
        if (booting) {
            manager.autoReconnect()
            booting = false
        }
    }

    suspend fun checkForUpdate() {
        val rel = UpdateChecker.fetchLatest()
        if (rel != null && UpdateChecker.isNewer(rel.tag) && prefs.getString("update_skip_tag", null) != rel.tag) {
            apkFile = null
            needInstallPermission = false
            update = rel
        } else {
            snack.showSnackbar("You're on the latest version (${BuildConfig.VERSION_NAME}).")
        }
    }

    LaunchedEffect(Unit) {
        val rel = UpdateChecker.fetchLatest()
        if (rel != null && UpdateChecker.isNewer(rel.tag) && prefs.getString("update_skip_tag", null) != rel.tag) {
            apkFile = null
            needInstallPermission = false
            update = rel
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            booting -> Reconnecting(manager.scooterIp.value)
            !connected && !skipped -> ConnectScreen(manager, onOpenAbout = { showAbout = true }, onSkip = { skipped = true })
            else -> Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snack) },
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                        Tab.values().forEach { t ->
                            NavigationBarItem(
                                selected = tab == t, onClick = { tab = t },
                                icon = { Icon(t.icon, contentDescription = null) },
                                label = { Text(t.title) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onBackground,
                                    selectedTextColor = MaterialTheme.colorScheme.onBackground,
                                    indicatorColor = tokens.hairline,
                                    unselectedIconColor = tokens.ink2, unselectedTextColor = tokens.ink2,
                                )
                            )
                        }
                    }
                }
            ) { pad ->
                Box(Modifier.fillMaxSize().padding(pad).statusBarsPadding()) {
                    Column(Modifier.fillMaxSize()) {
                        if (!connected) {
                            Banner("Not connected. Browsing only.", "Connect") { skipped = false }
                        }
                        Box(Modifier.weight(1f)) {
                            when (tab) {
                                Tab.Scooter -> ScooterScreen(manager, onOpenLog = { showLog = true }, snack = snack)
                                Tab.Apps -> AppsScreen(manager, snack)
                                Tab.Tools -> ToolsScreen(manager, snack, onOpenLog = { showLog = true }, onCheckUpdates = { scope.launch { checkForUpdate() } })
                            }
                        }
                        TextButton(onClick = { showAbout = true }, modifier = Modifier.align(androidx.compose.ui.Alignment.End).heightIn(min = 48.dp)) {
                            Text("About and legal", color = tokens.ink2)
                        }
                    }
                }
            }
        }
    }

    if (showLog) LogSheet(manager) { showLog = false }
    if (showAbout) AboutDialog { showAbout = false }

    update?.let { rel ->
        AlertDialog(
            onDismissRequest = { if (!updating) update = null },
            title = { Text(if (updating) "Downloading update" else "Update available") },
            text = {
                if (updating) {
                    Column {
                        Text("Version ${rel.tag} is downloading.")
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { progress ?: 0f },
                            modifier = Modifier.fillMaxWidth(), color = tokens.live, trackColor = tokens.hairline
                        )
                    }
                } else {
                    Text("Version ${rel.tag} is out. You have ${BuildConfig.VERSION_NAME}. It installs over this app; your saved scooter and settings stay.")
                    if (needInstallPermission) {
                        Spacer(Modifier.height(10.dp))
                        Text("Android needs permission to install apps from better-ola. Allow it on the screen that just opened, come back, and tap Update now again.", color = tokens.warn)
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = !updating, onClick = {
                    val url = rel.apkUrl
                    if (url == null) {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(rel.pageUrl)))
                        update = null
                        return@TextButton
                    }
                    scope.launch {
                        var file = apkFile
                        if (file == null) {
                            updating = true
                            file = ApkInstaller.fetch(ctx, url, rel.tag) { progress = it }
                            updating = false
                            progress = null
                            if (file == null) {
                                snack.showSnackbar("Download failed. Try again on a better connection.")
                                return@launch
                            }
                            apkFile = file
                        }
                        if (ApkInstaller.install(ctx, file)) {
                            update = null
                        } else {
                            needInstallPermission = true
                        }
                    }
                }) { Text(if (updating) "Downloading…" else "Update now") }
            },
            dismissButton = {
                if (!updating) {
                    TextButton(onClick = { prefs.edit().putString("update_skip_tag", rel.tag).apply(); update = null }) { Text("Skip this version") }
                }
            }
        )
    }
}

@Composable
private fun Reconnecting(ip: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = tokens.warn)
            Spacer(Modifier.height(16.dp))
            Text("Reconnecting to $ip", color = tokens.ink2, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun Banner(text: String, action: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(start = 20.dp, end = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = tokens.ink2)
        TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) { Text(action) }
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    fun open(url: String) = ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("better-ola ${BuildConfig.VERSION_NAME}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Made by Akshat Singh Kushwaha (a3ro-dev). Open source, MIT licence.")
                Text(
                    "Started from S1Mods by Anand (@Asjohn720), whose MIT copyright is kept. " +
                        "Signal tables come from Axiom (Apache-2.0) via itsmobsys/ola-s1-rea.",
                    color = tokens.ink2
                )
                Text(
                    "Read this before you ride. The dash and its physical warnings stay the authority. " +
                        "Anything shown here may be late, wrong or blank. Use mods while parked, or glance only.",
                )
                Text(
                    "Not affiliated with or endorsed by Ola Electric Mobility Ltd. \"Ola\", \"MoveOS\" and \"S1\" belong to their owners " +
                        "and are used only to say what this works with. Changing your scooter may affect its warranty. " +
                        "Provided as is, with no warranty. Nothing here is legal advice.",
                    color = tokens.ink2, style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { open("https://github.com/a3ro-dev/ola-s1-air-mod") }) { Text("Source") }
                    TextButton(onClick = { open("https://github.com/a3ro-dev/ola-s1-air-mod/tree/main/legal") }) { Text("Legal") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
