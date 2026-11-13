# Changelog

## 1.2.0 - 2026-11-08

Android Auto and CarPlay mods, a safer nav ball, and a lot more confirmation before anything changes on the scooter.

**Projection mods**
- One-tap Android Auto install and setup for wireless projection. The app grants the permissions over ADB: microphone (`RECORD_AUDIO`), location (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`), overlay (`SYSTEM_ALERT_WINDOW`) and storage.
- Apple CarPlay through the OpenPlay mod, with a fast online install and Open and Uninstall buttons on the app manager.

**Nav ball**
- It notices the MoveOS lock and standby states and hides itself, so nobody can poke the dash while the scooter is parked.
- An auto-return timer brings the MoveOS speedometer back to the front while you ride.
- A native app drawer to jump between installed mods (Music, Maps, Settings, CarPlay, Browser) from any screen.

**Safety and polish**
- After install there's a reminder: "Important: Use mod apps with the scooter stand UP. Lowering the side stand triggers vehicle auto-lock."
- Install, uninstall and custom sideload now ask first, so you can't hit them by accident.
- An Open button next to Uninstall launches any detected app on the scooter display.
- Every action (download, ADB install, package removal, app launch, display wake, screenshot save) streams to the in-app terminal.
- The setup button now says **Install Mod**, and the duplicate console snippet at the bottom of Setup is gone.
- Dropped the old APK binaries from the repo in favour of direct CDN downloads.

## 1.1.0 - 2026-11-06

**Added**
- Live head unit RAM, read from `/proc/meminfo` over gRPC and ADB, shown as total, used and free in MB. The card is green at 200 MB free or more, orange from 150 to 200 and red below 150.
- Firefox Focus (ARM64) in the catalog, with download progress and a background install.
- Custom APK sideloading from phone storage straight onto the scooter display.
- App manager rows are now vertical cards, so status badges don't get cut off on small phones.

**Fixed**
- Multicast auto-scan redirected to the dashboard too early. It now checks the gRPC handshake first.
- Removed the Scope ID field from manual connection, since IP or MAC is enough.

## 1.0.0 - 2026-11-01

First public release of **S1Mods**.

- A Kotlin and Jetpack Compose companion app, so no PC is needed.
- Automatic discovery: multicast IPv6 link-local and subnet scanning for the MoveOS root diagnostics daemon on TCP port `50005`.
- ADB authorisation through an in-memory RAM bind mount over TCP port `5555`, without touching persistent flash.
- Balls touch navigation: an overlay with Back, Home and Settings on the dash.
- A lightweight music player for streaming and local media, no ads.
- Toolbox: screenshot, hardware info and a settings launcher.
- A diagnostic root shell that runs commands as `uid 0`.
