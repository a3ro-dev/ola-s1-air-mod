# Android modules

Three Gradle modules. `app` runs on the phone, `hud` runs on the scooter's dash, and `vehicle` is shared code. The root `README.md` is for users; this one is for building and changing the Android side.

## Modules

### app

The phone companion. Compose UI, scooter discovery, the root gRPC client, the embedded ADB client, the setup wizard, and OTA updates. Package `dev.a3ro.betterola`. minSdk 26, targetSdk 35.

### hud

The dash overlay. A foreground service with one custom `View`, no Compose. Package `dev.a3ro.betterola.hud`. It draws over Android Auto and other apps, and it never takes a touch or focus.

### vehicle

An Android library used by both. `CanTap` opens the read-only CAN stream, `Decode` turns frames into `HudState`, and `Island` picks the one message the HUD shows. There is no code path that writes a CAN frame.

## Ports

| Port | Where | What |
| --- | --- | --- |
| 50005 | scooter | Root gRPC, service `wifican.WifiCanRpc`. `ExecuteADBCommand`, `GetSwVersion`, and the `CanDumpStream` CAN feed. |
| 5555 | scooter | ADB. Available after setup bind-mounts the phone's public key in RAM. |
| 5277 | phone | Android Auto headunit server. Loopback only. |

Port 50005 is the one the app is built around. The phone connects to it over the hotspot. The HUD connects to `127.0.0.1:50005` on the scooter itself.

Port 5555 is used for installs and shell commands. The key mount is in memory, so a scooter reboot clears it and closes ADB. A reboot does not restore customer mode, because `persist.ola.security.scootermode` stays `serv`. The app's "Debug access (ADB)" row calls `ScooterManager.closeAdb()` to unmount the key and set the property back to `cust`.

Port 5277 belongs to Android Auto on the phone. The app only checks whether it is listening and waits for you to start the server in Android Auto settings. It does not start the server.

## Build

```bash
cd android
./gradlew :app:assembleDebug
```

`:app:assembleDebug` builds the HUD first. The `copyHudApk` task depends on `:hud:assembleDebug`, copies `hud-debug.apk` to `app/build/generated/hudAssets/betterola-hud.apk`, and adds that folder as an app asset source. The merge-assets tasks depend on it, so the HUD APK is always inside the phone app. Setup installs it from assets, with no network.

Outputs:
- `app/build/outputs/apk/debug/app-debug.apk`
- `hud/build/outputs/apk/debug/hud-debug.apk`

To work on the HUD alone, `./gradlew :hud:assembleDebug` is enough.

## Release and OTA

The app and the HUD are both versioned `2.1.0` (app versionCode 7, HUD versionCode 2). The release tag is `v2.1.0`.

The phone app reads `https://api.github.com/repos/a3ro-dev/ola-s1-air-mod/releases/latest` on launch and from Tools. It picks the first `.apk` asset whose name does not contain `hud`, so both APKs can live in one release. If there is no APK asset, the update button opens the release page.

Release builds are signed with the debug key. That is fine for sideloading and for OTA from these releases, but a build from another machine has a different key. For wider distribution, use your own signing key.

## HUD constraints

- The window uses `FLAG_NOT_TOUCHABLE | FLAG_NOT_FOCUSABLE`, plus the layout flags. Taps pass through to Android Auto.
- Glows and meters use a 4x4 ordered dither. No blur and no gradients.
- It targets about 30 fps for the slow breathing animation and the indicator and message motion. When the system animator scale is 0, it stops posting frames and snaps state changes.
- The CAN tap is read-only. `CanTap` opens `CanDumpStream` and has no write path.
- The pill sits at the top left because Android Auto owns the top centre.
- The overlay needs `SYSTEM_ALERT_WINDOW`. `BootReceiver` restarts the service after a reboot only if that permission is still granted.

## Troubleshooting

See [../docs/TROUBLESHOOTING.md](../docs/TROUBLESHOOTING.md) for discovery, ADB, HUD, Android Auto, and update problems. It also lists the log tag and the commands worth knowing.
