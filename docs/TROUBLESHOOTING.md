# Troubleshooting

Most problems are one of four things: the phone and scooter are not on the same network, the scooter is asleep, ADB is holding an old session, or a permission is missing. Start with the section that matches what you see. When you need detail, the activity log in Tools is more useful than guessing.

## The app cannot find the scooter

The app scans the phone's hotspot or Wi-Fi for the scooter's root daemon on port 50005. It tries an IPv6 multicast ping, the kernel neighbour table, and an IPv4 sweep of the local subnet, in that order.

Check these first:
- The phone's hotspot is on and stays on for the whole setup. The scooter joins it from Settings, Wi-Fi on the dash.
- The scooter's screen is awake and the side stand is up. A locked scooter drops off the network.
- The scooter appears in the hotspot's connected devices list. If it is not there, fix the Wi-Fi join before touching the app.

The scan asks for location, and for nearby Wi-Fi devices on Android 13 and later. If you deny it, the scan can come back empty on phones that hide nearby network details without it. The permission is only used for discovery.

If the scan still finds nothing, open "Enter an address instead" and type the scooter's IPv4 address from the hotspot client list. A MAC address with five colons or dashes is converted to a link-local IPv6 address. If that does not connect, use the IPv4 address instead. Link-local IPv6 needs an interface scope, and the manual field does not ask for one.

The app remembers the last address in `betterola_prefs` and tries it on launch. If the scooter's IP changed, the connect screen still shows the old address and there is no scan button in that state. Type the new address in the manual field, or clear the app's storage to get the first-run scan back.

## Port 50005 is unreachable

This is the daemon the whole app depends on. If it does not answer, nothing else works.

Wake the scooter first. Press the power button or plug in the charger, wait for the dash, and put the side stand up. A sleeping or locked scooter has no network, and a lowered stand triggers auto-lock.

Then check the network. The phone must be on the same hotspot or Wi-Fi as the scooter, not on mobile data. Guest networks and client isolation can block phone-to-scooter traffic.

From a computer on the same network, this is the quickest test:

```bash
python -m pip install -r requirements.txt
python ola.py info --ipv6 fe80::1234:5678:9abc:def0%6
```

Replace the address and scope with yours. If `ola.py` cannot connect, the app will not either. A scooter restart is the next thing to try; the daemon comes back with MoveOS.

The app's own error is direct: "The scooter did not answer on port 50005. Wake its screen and put the side stand up, then try again."

## Setup stops at the ADB step

Setup unlocks ADB in step 2. It generates an RSA key in the app's files, pushes the public key to `/data/local/tmp/adb_keys_mine`, sets `persist.ola.security.scootermode` to `serv`, and bind-mounts the key over the daemon's authorized keys file. The mount lives in RAM, so a reboot clears it. The `serv` property is persistent, so a reboot does not put the scooter back in customer mode.

If the step fails:
- Reboot the scooter. This clears the old in-memory mount, then run setup again. The `serv` property stays set, and setup sets it again anyway.
- Close every other ADB client. A laptop with `adb`, scrcpy, or Android Studio open can hold the only ADB connection. Run `adb kill-server` or unplug it, then retry.
- Check the log line. "bind mount script returned non-zero" means the mount did not take. A failure after "Connecting Kotlin ADB client" means the gRPC side worked and the ADB connect failed. Check the key mount and any other ADB client.
- Wake the screen and keep the stand up through the whole run.

If setup was interrupted after it set `serv` mode, open the Scooter tab and tap "Debug access (ADB)". That calls `ScooterManager.closeAdb()`, which unmounts the key and sets `persist.ola.security.scootermode` to `cust`. If the row fails, use the Tools root shell:

```bash
umount /data/misc/adb/adbkeymoveos3_pub_prod
umount /data/misc/adb/adb_keys
umount /adbkeymoveos3_pub_prod
umount /adb_keys
setprop persist.ola.security.scootermode cust
```

Some of these paths may not exist on your unit. The errors are harmless, and the `setprop` is the part that restores customer mode.

## The HUD is missing or shows "--"

The HUD is a separate app on the scooter, package `dev.a3ro.betterola.hud`, label "Dash HUD". It does not run on the phone.

Check that it is installed. In the Tools root shell, or over ADB if it is open:

```bash
pm list packages | grep betterola
```

You should see `package:dev.a3ro.betterola.hud`. The phone app is `dev.a3ro.betterola` and will not appear on the scooter.

Then check the overlay permission. Without "Display over other apps", `HudService` stops itself and draws nothing. Setup grants it with `appops set dev.a3ro.betterola.hud SYSTEM_ALERT_WINDOW allow`. If you installed the HUD by hand, open the Dash HUD app once; it shows a toast and opens the permission screen. Grant it, then open the app again.

The HUD runs as a foreground service. Setup runs `pm grant dev.a3ro.betterola.hud android.permission.POST_NOTIFICATIONS`. On Android 13 and later, grant notifications for Dash HUD if you installed it yourself, so the ongoing notification is visible and the service is easy to find.

`BootReceiver` brings the HUD back after a reboot, but only if the overlay permission is still granted. If it does not return, open the app once.

A fresh HUD shows `--` for speed and `--%` for charge because every signal starts as null. That is normal for the first few seconds. The CAN tap reconnects with a backoff of up to 8 seconds, and the HUD marks the data stale after 3 seconds without a frame.

If it stays on "Waiting for the scooter", the tap has not delivered a speed frame yet. If it says "Scooter data stopped", it had frames and lost them. In both cases, wake the scooter and check that the HUD is running. The Tools tab's Live signals section shows a frame count; if that stays on "Nothing yet", the tap is the problem, not the drawing.

The HUD is not touchable and not focusable. It will never take a tap from Android Auto, and it will never appear as the focused window. The Lock, Unlock and Boot bar is a separate window and only exists while the scooter is stopped.

## Lock, unlock and boot do nothing

The app and the HUD send the same command the stock dash sends: one byte on CAN `0x6BC`, written with the head unit's own `cansend`.

```bash
cansend vcan0 6BC#5400000000000000   # lock now
cansend vcan0 6BC#1900000000000000   # passcode auth, the stock unlock loop sends this 30x
cansend vcan0 6BC#0300000000000000   # seat latch release
```

The MCU only bridges `vcan0` to the vehicle while the scooter is awake. When it is asleep or locked, the command is dropped silently: the gRPC call still returns success and nothing happens. Wake the scooter (power button and brake), then try again.

The buttons also refuse to run unless the speed is zero. Lock and boot need the scooter unlocked; unlock needs it locked. Boot asks for confirmation because the seat latch releases.

If the buttons stay greyed out, the live lock state is missing. Check the Live signals section for a climbing frame count. If the frames are there but the state stays unknown, the lock word on your build may differ; send the `0x54` frame by hand from the Tools root shell to confirm the path before blaming the app.

## Android Auto has no "Start headunit server"

Android Auto hides its developer menu by default. Open Android Auto settings, scroll to the bottom, tap Version ten times, then reopen the three-dot menu at the top right. "Start headunit server" appears there.

Android Auto 17.4 and later stopped apps from starting Self-Mode for you. That removed the old automatic path, so the headunit server is the only one left. The app checks `127.0.0.1:5277` on the phone, opens Android Auto settings, waits up to 120 seconds for the server, then launches the receiver app on the scooter.

The Scooter tab has an "Android Auto link" row that watches port 5277 every 2 seconds. "Tap to start" means the server is off. Tap it and follow the dialog. "Headunit server on" means the phone side is ready.

The server stops when Android Auto is force-stopped or the phone reboots. Start it again from the same menu.

## Updates

### It says you are on the latest version, but a newer release exists

The app asks GitHub for `https://api.github.com/repos/a3ro-dev/ola-s1-air-mod/releases/latest`. That endpoint ignores drafts and prereleases. If the newer build is marked as a prerelease, the app cannot see it. Publish it as a normal release, or download the APK from the releases page by hand.

Two other things can hide an update. "Skip this version" stores the tag in the app's preferences and suppresses that tag until you clear the app's data. And the version comparison is numeric: a tag with no numbers, such as `release-two`, is never newer than `2.1.0`.

The check is also silent when GitHub is unreachable or rate-limited. The Tools button then says you are on the latest version even though it never got an answer.

The checker takes the first `.apk` asset whose name does not contain "hud". If the release has no APK asset at all, "Update now" opens the release page instead of downloading.

### The download finishes but the installer will not open

Android blocks installs from apps that are not allowed to install unknown apps. The app opens the "Install unknown apps" screen for better-ola and stops there. Turn the permission on, come back, and tap Update now again. The app keeps the downloaded APK, so the second tap installs it without downloading again.

### The installer refuses with a signature mismatch

Android will not replace an app with one signed by a different key. Release builds here use the debug signing key, so a build from another machine or a fork has a different signature. Uninstall the phone app, then install the new APK. Uninstalling clears the app's data, so expect to connect to the scooter again. The scooter and the HUD are not touched.

## Reading the activity log and the Tools tab

The Tools tab is the diagnostic side of the app:
- Dash screenshot takes a PNG from the scooter's display and can save it to Pictures/better-ola.
- Live signals shows the decoded CAN values and a frame count, so you can compare the HUD against the dash.
- Open on the scooter opens Wi-Fi, Bluetooth, Display, Sound, Apps, and Date and time settings on the dash.
- About this scooter reads Android version, build, model, security mode, and RAM.
- Root shell runs commands as root on the scooter. Read with it. Writes can break the dash.
- Check for updates and Activity log are at the bottom.

The activity log is a bottom sheet with "Copy all". It is kept in memory only, and starting setup clears it. Copy it before you re-run setup if you want to keep the failure.

Live signals is the fastest way to tell a HUD drawing problem from a data problem. A climbing frame count with `--` values means frames are arriving but those signals are not in them. "Nothing yet" means the tap is not receiving at all.

## Commands worth knowing

`adb logcat -s BetterOla` on the phone, with USB debugging on. The phone app's logcat tag is `BetterOla`. The CAN tap writes connect and disconnect lines there; most setup detail is in the in-app log instead.

`pm list packages | grep betterola` on the scooter, in the Tools root shell or through `adb shell`. It should list `dev.a3ro.betterola.hud`.

`dumpsys window | grep mCurrentFocus` on the scooter. It tells you which window has focus. `com.olaelectric.hmiclient` is the dash home. The HUD never takes focus, so it will not be listed.

`getprop ro.build.version.release` on the scooter. This is the Android version, which matters for notification and nearby-device permissions.

`python ola.py --help` from a computer. Install `grpcio` first with `python -m pip install -r requirements.txt`. For a link-local address, pass the scope as `fe80::...%6` or with `--scope 6`. `python ola.py info --ipv6 ...` prints uid, build, and scooter mode, which is the quickest check that port 50005 is alive.

The Tools root shell is the same root channel without a cable. It has presets for `id`, `uptime`, `getprop ro.build.display.id`, `pm list packages -3`, and `df -h /data`. Commands time out after 30 seconds.

## What to put in a bug report

- The app version from About, and the HUD version if the problem involves the overlay.
- Scooter model, MoveOS build (`getprop ro.build.display.id`), and Android version (`getprop ro.build.version.release`).
- Phone model and Android version.
- What you did, in order, and what you expected.
- The exact error text or snackbar, not a paraphrase.
- The activity log copied from Tools. Redact the IP addresses if you post it publicly.
- Whether the scooter screen was awake and the side stand was up.
- Whether it happens every time or only once.
- `adb logcat -s BetterOla` if the app crashed or the CAN tap is involved.
