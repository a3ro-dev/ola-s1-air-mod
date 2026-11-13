> Superseded 2026-11-13 by [DESIGN.md](../../DESIGN.md): the amber tell-tale world below was replaced by a dither-ui palette world. Kept as history.

# better-ola · phone app direction

## Brief (from the owner, 13 Nov 2026)

- **What / who:** a phone companion for one Ola S1 owner (the maintainer) and anyone like him. It finds the scooter on a phone hotspot, installs and looks after the dash mods (Android Auto receiver, nav ball, music, the new HUD), and shows what the scooter is doing.
- **Core action, first ten seconds:** see whether the scooter is reachable and how much battery is left, then do one thing: set up, fix, or put Android Auto on the dash.
- **Feel:** functional, sensible, "unapologetically works". Not slop. Must sit in the same world as the on-dash HUD, which is pinned to the dither-ui look (ordered dither).
- **Exists:** an old Compose app (Material 3, dark slate + neon blue cards, "1-CLICK AUTOMATED SETUP", log console as the main surface). Nothing in it is kept visually.
- **Needs:** a rebuilt Compose app that works on any Android phone size, plus an onboarding that doesn't feel sloppy.
- **Image generation:** not used.

## Current app

Hits the "Dark Fintech" face almost exactly: near-black slate, one neon accent, a stack of rounded cards with tinted icon chips, all-caps buttons, a terminal log as the hero, tabs Setup / Toolbox / Custom APK / Terminal.

Keep (contract):
- auto scan (IPv6 neighbours, multicast, IPv4 sweep) and manual MAC/IP entry with the MAC to IPv6 converter
- one-run setup: unlock ADB, install chosen apps, grant permissions, return to the dash
- per-app install / update / open / uninstall, including online CarPlay (OpenPlay) and custom APK sideload
- screenshot of the dash with save, settings shortcuts (Wi-Fi, Bluetooth, display, sound, apps, date)
- vehicle summary (model, MoveOS), specs (Android, build, mode), RAM
- root shell with presets, copyable output
- update check against GitHub releases
- the side-stand-up warning

Lose: the log console as a primary surface (it moves to a detail sheet), all-caps copy, "1-CLICK AUTOMATED", the ball/heart/coffee chrome.

New: live battery %, range and mode from CAN on the phone; HUD install and status; "Close ADB" (puts the scooter back in customer mode); a vehicle probe dump.

Decisions for the owner: tabs go from 4 to 3 (Scooter, Apps, Tools); Terminal moves into Tools; the log becomes a sheet.

## Three concepts

1. **better-ola is the dash's amber tell-tale.** (object) A warm-black instrument face; amber only when something is live or acting; the battery is drawn with an ordered-dither fill, the same engine as the HUD. Archivo, expanded for numbers.
2. **better-ola is a service-bay job card.** (printed) A cool grey card stock, mono type in ruled fields, a red stamp for state.
3. **better-ola is a monsoon street at dusk.** (nature) A saturated teal field, rounded type, flat rain shapes, a bougainvillea pink accent.

Rendered in `explore.html` → `shots/explore`.

**Won: 1, the tell-tale.** It is the only one that belongs to the same object as the HUD: the rider sees the same amber and the same dither on the dash and on the phone, so the two read as one product. Amber is the scooter's own indicator colour, and here it means only one thing: something is live or happening.
- 2 lost: a job card describes paperwork after the fact, but this app is used live, standing next to the scooter. The ruled form also drifts toward this process's known overcorrection (grey paper, mono, one red).
- 3 lost: it is pretty but says nothing about the machine. Teal plus pink is mood, not meaning, and the rain illustration would be decoration on a tool screen.

History check vs `presence-guard | nature | colour-field | expanded | blue | colour-material`: object ≠ nature, dark ≠ colour-field, orange ≠ blue, shape ≠ colour-material. Differs on 4 of 5.

## The category default we refuse

Scooter and EV companion apps (Ola Electric, Ather, TVS iQube) share: a hero render of the scooter on a gradient, a big battery ring, a 2x2 grid of tiles (range / odometer / lock / find), and a brand-colour gradient button. The EV cliché is **electric green or cyan glow on black**. We do none of it: no scooter render, no ring, no tile grid, no glow, no green-for-electric. Battery is a dithered bar you can read at arm's length.

## Tokens

Colour roles (dark is primary; light follows system):

| role | dark | light |
|---|---|---|
| ground | `#0F0E0C` | `#F3F1EC` |
| surface (sheets, nav) | `#1A1815` | `#FFFFFF` |
| ink | `#F2EEE6` | `#17150F` |
| ink-2 (secondary) | `#B5AE9F` (8.4:1 on ground) | `#5C564A` (6.6:1) |
| hairline | `#2C2924` | `#DDD8CD` |
| amber (live / acting) | `#FFB021` | `#B86E00` (4.6:1 on ground) |
| on-amber | `#1A1206` | `#FFFFFF` |
| fault (semantic) | `#FF5A4E` | `#C2261B` |
| ok (semantic, sparse) | `#9BD17A` | `#3F7A1F` |

Type: **Archivo** (variable, wdth 62 to 125, wght 100 to 900, OFL). Display numbers at wdth 125 (expanded), wght 700, tracking -2%. Body at wdth 100.

| step | size/line | weight | use |
|---|---|---|---|
| display | 88/84 sp | 700 expanded | battery % |
| headline | 28/32 | 650 expanded | screen title, scooter name |
| title | 18/24 | 600 | row titles, sheet titles |
| body | 15/22 | 400 | copy |
| label | 13/18 | 550 | buttons, chips, metadata |
| caption | 12/16 | 450 | timestamps, addresses (tabular nums) |

Margin 20dp. Radius: 4dp (controls), 12dp (sheets top). Spacing 4, 8, 12, 20, 32, 56.

## Richness source

**Ordered dither as a shape language.** 4x4 Bayer threshold patterns, drawn at 2dp cells, in amber or ink. Used for: the battery fill (the dither density is the charge), the "searching" sweep, the HUD preview, and the empty state. Nothing else is decorated. No gradients anywhere; when a gradient would be needed, dither it.

## Signature interaction

**Hold to put Android Auto on the dash.** The primary button fills with a dither pattern from left to right while you hold (600 ms). Release early and it drains back. At full, a single 40 ms haptic tick, the pattern snaps solid amber, the label changes to "On the dash", and a short click sound (real-sfx) plays. It is a hold because it changes what the rider sees on the road screen.
- Reduced motion: no fill animation; a normal tap opens a confirm snackbar.

## Content (core screen, connected, mid-use)

- Top: `S1 Pro Gen 2` · `MoveOS 5.1.0` · `On your hotspot`
- Hero: `64%` battery, `71 km in Normal`, `Parked · stand down`
- Dash row: `HUD running` · `Android Auto open`
- Safety row: `ADB closed` (ok) — or `ADB open since 18:42 · Close` (amber)
- Primary: `Hold to open Android Auto on the dash`
- Rows: `Screenshot the dash` · `Apps — 5 installed, 1 update` · `Vehicle probe`
- Nav: Scooter · Apps · Tools
