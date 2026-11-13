# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users
Ola S1 owners (the maintainer first) standing next to a parked scooter with a phone, setting up or checking the scooter, and later riding with the on-dash HUD in peripheral view. Inferred from the repo and README, not confirmed in an interview.

## Product Purpose
A phone app that finds the scooter on a hotspot, installs and looks after dash mods, and a HUD over Android Auto. Success: setup without a laptop, undone by a restart, and nothing in the way of the road.

## Positioning
Reversible, in-RAM ADB, listen-only CAN with the stock lock, unlock and boot commands on request, no laptop. A one-person hobby project, unofficial.

## Capabilities and Constraints
UI-only redesign: no backend, gRPC, ADB or CAN changes. HUD window never takes touch. Tested on S1 Pro Gen 2 and S1 Air. Cannot be built or screenshotted on the authoring machine (no Android SDK).

## Brand Commitments
Name "better-ola" (see legal/REVIEW.md on the trademark risk). Voice is the maintainer's own: plain, lower-case in public posts. Visual palette pinned by the user: dark, dither-ui's own named palette.

## Evidence on Hand
No real screenshots of the redesigned app exist. Every image on the site and README is a labelled design render with sample data.

## Product Principles
1. Unknown reads as unknown, never as zero.
2. One action per screen. The riskiest one is a hold.
3. The HUD adds, never obstructs.
4. Say what changes on the scooter before it changes.

## Accessibility & Inclusion
Contrast 4.5:1 body text, 48dp targets, reduced motion honoured, TalkBack falls back from hold to tap.
