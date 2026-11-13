# Design

World: dark instrument face printed in ordered dither, built on dither-ui's named palette. One hue per meaning, and no hue means two things. Replaces the earlier amber "tell-tale" world.

## Colour (dark primary, light follows system)
| role | dark | use |
|---|---|---|
| ground | `#0B0C0E` | screens |
| surface | `#15171A` | nav, sheets |
| hairline | `#262A2F` | rules, empty cells |
| ink / ink-2 | `#ECEEF0` / `#A6ABB1` | text (ink-2 is 8.1:1 on ground) |
| live | `#3DA5FF` | connected, selected, the one action per screen |
| warn | `#FFD23D` | needs attention, HUD indicators |
| ember | `#FF8A3D` | HUD power draw |
| ok | `#7CFF67` | done, regeneration |
| fault | `#FF5A4E` | errors, brake |

Light scheme tokens are in `ui/theme/Color.kt`.

## Type
Archivo (variable, OFL). Numbers and titles in the expanded cut (wdth 125, wght 650 to 700, tracking -2 to -3%). Body 15sp at wdth 100. Mono only for addresses and logs.

## Shape and depth
Controls and panels 10dp radius, sheets 20dp. No shadows in the app; elevation is tonal. Status is a 6dp square, never a coloured word.

## Signature
- **Cell column:** the battery is 20 stacked cells filled from the bottom, the top cell dithered, so the dither edge is the number.
- **Hold to open Android Auto on the dash:** blue dither sweeps in over 600ms, drains back in 300ms if released, a haptic tick at full. Reduced motion or TalkBack: plain tap.
- **HUD:** pill top left, yellow dither glow on the edges for indicators, orange to the right and green to the left on the bottom edge for draw and regen, red for brake.

## Motion
Reveal and hold only. Ease-out, under 400ms on the web, none when the system animation scale is 0.

## Provenance
Code in `android/app/.../ui` and `android/hud/.../HudView.kt` was changed without being compiled. Renders in `docs/assets` come from `docs/src/*.html`.
