# better-ola

A phone companion and an on-dash HUD for the Ola S1's Android head unit. It finds the scooter on your hotspot, installs and looks after the mods, and keeps the numbers that matter on screen while you use Android Auto.

No laptop. No flashing. A restart undoes the debug access.

> Not affiliated with, endorsed by, or connected to Ola Electric Mobility Ltd. "Ola", "MoveOS" and "S1" belong to their owners and are used only to say what this works with. Read [legal/DISCLAIMER.md](legal/DISCLAIMER.md).
>
> **Use at your own risk.** This changes software on a vehicle, it can void your warranty, and by using it you accept the [terms of use](legal/DISCLAIMER.md#terms-of-use). Provided as is, no warranty, no support. Never use it while riding.

---

## What you get

**better-ola** (`android/app`) is the phone app. It connects to the scooter, installs mods, shows live battery and range, and updates itself from GitHub Releases.

**better-ola HUD** (`android/hud`) is a transparent overlay for the scooter's display. It sits above Android Auto and never takes a touch. The phone app carries it and installs it during setup, so you do not install it by hand.

---

## The HUD

The HUD is not a replacement dashboard. It is a thin layer over whatever is already on the screen.

Top left: speed, drive mode, state of charge and remaining range in one pill. It sits left of Android Auto's search bar and tabs on purpose.

Screen edges: an amber ordered-dither glow for the indicators, and a red strip for the brake. Along the bottom edge, a dithered power meter. Amber when drawing power, green when regenerating, red under braking. Easy Park hatches it.

The pill widens for the one thing worth saying: side stand down, power reduced, charging, hazard lights, data stopped.

It stays out of the way. The window uses `FLAG_NOT_TOUCHABLE | FLAG_NOT_FOCUSABLE`, so every tap goes to Android Auto. It draws with ordered dither, no blur and no gradients, at about 30 fps only while something is moving. If the system animation scale is 0, it stops animating.

Signals are decoded read-only from the scooter's own CAN broadcasts. Nothing in the HUD writes to the bus.

---

## The phone app

1. Turn on your phone's hotspot and join it from the scooter (Settings → Wi-Fi).
2. Open **better-ola**. It remembers the last scooter and reconnects on its own. The scan screen only appears when nothing is saved or the scooter is unreachable.
3. Tap **Set up Android Auto and the HUD**. The app unlocks ADB in memory, installs the selected APKs, grants the permissions they need, and returns the dash to its home screen.
4. From then on, **Hold to open Android Auto on the dash** does exactly that.

Also in the app:

- Live battery, range, mode, side stand and charging, read from a listen-only CAN tap.
- Apps: install, reinstall or remove the bundled mods, or sideload any APK.
- Tools: dash screenshot, live signals, system info, settings shortcuts, a root shell, and a check for updates button.
- **OTA updates:** on launch, and on demand from Tools, the app asks GitHub for the latest release. If it is newer, it downloads the APK and hands it to the system installer. Your saved scooter and settings stay.

### Android Auto in Self-Mode (AA 17.4 and later)

Android Auto 17.4 removed the automated Self-Mode launch that OpenHeadUnit and similar receivers used. The headunit server is the path that still works.

The app handles it:

1. It checks port `5277` on the phone.
2. If the server is off, it opens Android Auto settings and shows the one manual step: **⋮ → Start headunit server**. If that item is missing, tap **Version** ten times at the bottom of the settings screen first.
3. It waits for the server, then opens Android Auto on the scooter by itself.

The **Android Auto link** row on the Scooter tab shows whether the server is up.

---

## Install

Download the latest `better-ola-*.apk` from [Releases](https://github.com/a3ro-dev/ola-s1-air-mod/releases), open it on the phone, and allow installs from your browser or file manager when asked. The app carries the HUD APK and installs it on the scooter during setup.

Requirements:

- An Ola S1 with the 7-inch Android touchscreen (see the matrix below).
- The scooter on the same hotspot as the phone.
- Side stand **up** and the screen awake while setup runs.

---

## Compatibility

| Model | Display | Head unit | MoveOS | Status |
|---|---|---|---|---|
| S1 Pro (Gen 2) | 7" 800×480 touch | Snapdragon 625 (`msm8953`) | 3, 4, 5 | Tested |
| S1 Pro (Gen 1) | 7" touch | Snapdragon 212 (`APQ8009`) | 2, 3, 4, 5 | Expected to work |
| S1 (Gen 1) | 7" touch | Snapdragon 212 (`APQ8009`) | 2, 3, 4, 5 | Expected to work |
| S1 Air | 7" 800×480 touch | Octa-core | 3, 4, 5 | Tested |

Segmented-LCD and non-Android clusters are not supported.

---

## Questions people actually ask

**Do I need to root the scooter?**
No. The scooter ships with a diagnostics daemon that runs as root on port 50005. The app uses that. It never flashes a partition.

**Does this void my warranty?**
I cannot answer for Ola. What I can say is that nothing is written to a partition. The ADB key is bind-mounted in RAM, and a reboot removes it.

**Can it unlock the scooter?**
No, and the app does not pretend to. The head unit can ask the vehicle to unlock, but the VCU decides and answers on the CAN bus. The only remote unlock that works is Ola's own app through the cloud. See [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md).

**Will the HUD block Android Auto?**
No. The overlay window is not touchable and not focusable. Taps pass through to Android Auto. It also has no full-screen scrim.

**Why is the pill on the left?**
Because Android Auto puts the Maps search bar and its app tabs in the top centre. The pill would cover them.

**Does the HUD show a warning if the data goes stale?**
Yes. If CAN frames stop, the pill says the data stopped instead of leaving an old speed on screen.

**Does it work on the S1 X or the S1 Air with a different cluster?**
Only the Android touchscreen is supported. If the cluster is not Android, this does nothing.

---

## When something is wrong

The short list:

- **Scan finds nothing.** Allow location and nearby-devices permission, keep the scooter awake, and check the phone's hotspot client list. You can type the scooter's IP by hand.
- **Port 50005 is closed.** Wake the scooter screen and put the side stand up. A sleeping scooter has no network.
- **Setup stops at ADB.** Reboot the scooter and run setup again. The key mount lives in RAM, and a stale mount blocks the new key.
- **HUD is invisible.** Check that `dev.a3ro.betterola.hud` is installed on the scooter and has the overlay permission. Open the app once after a fresh install.
- **Android Auto has no headunit server option.** Open Android Auto settings, tap Version ten times, then reopen the ⋮ menu.
- **Update downloads but will not install.** Allow installs from better-ola when Android asks, then tap Update now again.

The full FAQ with commands and log tags is in [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md).

---

## Safety and reversibility

- ADB is authorised with an RSA key bind-mounted **in RAM**. No partition is written, and a restart removes it.
- The app does not touch the power manager or the auto-lock. The scooter still locks itself as designed.
- The HUD only listens to CAN frames. It sends nothing.
- The dash and its physical warnings remain the authority. Treat anything here as a convenience that can be late, wrong or blank, and glance only while riding.

`ola.py` is the same root channel from a computer, for when you want a shell:

```bash
python ola.py --help
```

---

## Build

```bash
cd android
./gradlew :app:assembleDebug
```

The HUD is built first and copied into the app's assets automatically, so the phone app can install it offline. Outputs:

- `android/app/build/outputs/apk/debug/app-debug.apk`
- `android/hud/build/outputs/apk/debug/hud-debug.apk`

Release builds use the debug signing key, which is fine for sideloading and OTA from these Releases. If you fork this for wider distribution, use your own key.

---

## Layout

```
android/
  app/       phone companion (Compose)
  hud/       dash overlay (custom View, no Compose)
  vehicle/   CAN decoding, tap, warning priority, shared by both
design/      the app and HUD design notes and probe captures
docs/        troubleshooting and FAQ
legal/       disclaimer, privacy, third-party notices
ola.py       root gRPC CLI for the scooter
setup.py     legacy PC setup script
```

---

## Licence

MIT. See [LICENSE](LICENSE). Third-party signal tables and bundled mods are credited in [legal/THIRD-PARTY.md](legal/THIRD-PARTY.md).
