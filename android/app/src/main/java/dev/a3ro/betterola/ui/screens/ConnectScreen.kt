package dev.a3ro.betterola.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.a3ro.betterola.discovery.NetworkScanner
import dev.a3ro.betterola.model.ScooterManager
import dev.a3ro.betterola.ui.components.*
import dev.a3ro.betterola.ui.theme.tokens
import kotlinx.coroutines.launch

private enum class Scan { Idle, Scanning, Found, Missing, Connecting }

/**
 * First run and every reconnect. One job: get from "nothing" to "connected".
 * The two hotspot steps are plain text because they happen on other screens.
 * Location is asked for here, at the moment Scan needs it, with the reason.
 */
@Composable
fun ConnectScreen(manager: ScooterManager, onOpenAbout: () -> Unit, onSkip: () -> Unit) {
    val scope = rememberCoroutineScope()
    val ip by manager.scooterIp.collectAsState()
    var scan by remember { mutableStateOf(Scan.Idle) }
    var manual by remember { mutableStateOf(false) }
    var manualText by remember { mutableStateOf("") }
    var problem by remember { mutableStateOf<String?>(null) }

    fun connectNow() = scope.launch {
        scan = Scan.Connecting
        problem = null
        try {
            manager.connect()
        } catch (e: Exception) {
            problem = "The scooter did not answer on port 50005. Wake its screen and put the side stand up, then try again."
            scan = Scan.Found
        }
    }

    fun runScan() = scope.launch {
        scan = Scan.Scanning
        problem = null
        val found = manager.autoDiscover()
        scan = if (found != null) Scan.Found else Scan.Missing
    }

    val permissions = remember {
        if (Build.VERSION.SDK_INT >= 33)
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES)
        else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    val askAndScan = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { runScan() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding()) {
        ScreenTitle("Connect to your scooter", "Takes about a minute. Nothing is changed yet.")

        Step(1, "Turn on your phone's hotspot", "Keep it on for the whole setup.")
        Step(2, "Join it from the scooter", "On the scooter: Settings, Wi-Fi, pick your phone.")
        Step(
            3, "Find the scooter",
            "Android needs location access to list nearby devices. It is not stored or sent anywhere.",
            active = true
        )

        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Spacer(Modifier.height(8.dp))
            when (scan) {
                Scan.Idle, Scan.Missing -> {
                    if (scan == Scan.Missing) {
                        Text(
                            "Not found. Check the scooter's screen is awake and joined to your hotspot, or enter its address below.",
                            style = MaterialTheme.typography.bodyMedium, color = tokens.ink2
                        )
                    }
                    PrimaryButton("Scan for the scooter", onClick = { askAndScan.launch(permissions) })
                }
                Scan.Scanning -> {
                    Text("Looking on your hotspot…", style = MaterialTheme.typography.bodyLarge)
                    LinearProgressIndicator(Modifier.fillMaxWidth(), color = tokens.amber, trackColor = tokens.hairline)
                }
                Scan.Found -> {
                    Text("Found a scooter", style = MaterialTheme.typography.titleMedium)
                    Text(ip, style = MaterialTheme.typography.labelMedium, fontFamily = Mono, color = tokens.ink2)
                    problem?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
                    PrimaryButton("Connect", onClick = { connectNow() })
                }
                Scan.Connecting -> {
                    Text("Connecting…", style = MaterialTheme.typography.bodyLarge)
                    LinearProgressIndicator(Modifier.fillMaxWidth(), color = tokens.amber, trackColor = tokens.hairline)
                }
            }

            if (scan == Scan.Idle || scan == Scan.Missing || scan == Scan.Found) {
                TextButton(onClick = { manual = !manual }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (manual) "Hide manual entry" else "Enter an address instead")
                }
                if (manual) {
                    OutlinedTextField(
                        value = manualText,
                        onValueChange = { manualText = it },
                        label = { Text("IP address or MAC address") },
                        supportingText = { Text("From your hotspot's connected devices list") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        modifier = Modifier.fillMaxWidth()
                    )
                    QuietButton("Use this address", onClick = {
                        val t = manualText.trim()
                        val host = if (t.count { it == ':' } == 5 || t.count { it == '-' } == 5)
                            NetworkScanner.macToIpv6(t) else t
                        if (host.isNullOrBlank()) {
                            problem = "That is not an IP or MAC address."
                        } else {
                            manager.scooterIp.value = host
                            manager.scooterScope.value = null
                            scan = Scan.Found
                            problem = null
                        }
                    }, enabled = manualText.isNotBlank())
                }
            }
        }

        Spacer(Modifier.weight(1f, fill = false).heightIn(min = 32.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onOpenAbout, modifier = Modifier.heightIn(min = 48.dp)) { Text("About and legal") }
            TextButton(onClick = onSkip, modifier = Modifier.heightIn(min = 48.dp)) { Text("Look around first") }
        }
    }
}

@Composable
private fun Step(n: Int, title: String, body: String, active: Boolean = false) {
    Row(Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.Top) {
        Text(
            n.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = if (active) tokens.amber else tokens.ink2,
            modifier = Modifier.width(32.dp)
        )
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = tokens.ink2)
        }
    }
}
