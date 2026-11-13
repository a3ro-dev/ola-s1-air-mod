# Privacy

better-ola collects nothing.

This document is not legal advice. I am not a lawyer.

## Short version

- No account. No sign in.
- No analytics, no telemetry, no crash reporting.
- No ads, no trackers, no third party SDKs that phone home.
- No server run by this project.
- No database of users, vehicles, or rides.
- Nothing is uploaded anywhere by the project's own code.

Your scooter data stays on your phone and on your local network.

## What the phone app stores

The app keeps a small amount of settings on your device: the last scooter address you used, and your preferences. That is stored by the app in its own private storage on your phone. It is not sent anywhere. Uninstalling the app removes it.

Screenshots or files you choose to save go wherever you save them, under your control.

## What the PC scripts store

The Python scripts write only local log output and any files you ask them to write. They do not send data to the project.

## Permissions the Android app asks for, and why

| Permission | Why it is needed |
| --- | --- |
| Internet | To reach the scooter on your local network, and to check GitHub for updates. |
| Access network state | To see whether Wi-Fi is on and which network you are connected to. |
| Access Wi-Fi state | To read the current connection so the app can pick the right network. |
| Change Wi-Fi multicast state | To receive multicast discovery replies from devices on the local network. |
| Fine location | Android requires location permission before an app may scan for nearby Wi-Fi devices or read Wi-Fi details. The app does not use your location for anything else, and does not store or send it. |
| Coarse location | Same reason, for older Android versions. |
| Nearby Wi-Fi devices | The modern Android permission for finding devices on the local network. Declared with the flag that says it is not used to derive location. |
| Read external storage (Android 12 and older) | Only to let you pick a custom APK file from your storage. Android 13 and newer use the system file picker instead, which needs no storage permission. |

The app does not request contacts, camera, microphone, SMS, call logs, or background location. If a build ever asks for one of those, treat it as a red flag and check the source.

Location permission on Android is a platform requirement for Wi-Fi scanning, not a feature of this project. Nothing about your position is recorded.

## Network calls the project makes

There are only three:

1. Local network to the scooter, over the address and port you point it at. This is the point of the tool.
2. GitHub Releases API, to check whether a newer release exists. This sends a normal HTTPS request. GitHub sees your IP address, as it would for any website visit. The project does not add identifiers, and does not send your vehicle data.
3. Downloads of optional APKs from GitHub release URLs, when you choose to install one. Same as above: GitHub sees the request.

You can avoid 2 and 3 by not using the update check and by installing files yourself.

## The scooter side

The tool talks to a gRPC service the head unit already exposes. Requests stay on your local network. Traffic is not routed through any server owned by this project, because there is no such server.

If you use the scooter's built in cellular connection for anything, that traffic is handled by the vehicle's own connectivity provider under their terms. That is outside this project, and this project does not use it.

## Children

This is a tool for vehicle owners. It is not directed at children.

## Data requests

There is no data to request, correct, or delete on any server, because none is collected. If you believe otherwise, open an issue with details and I will look.

## Changes

If this ever changes, for example if an update check starts sending more than a plain request, this file will say so before the change ships.
