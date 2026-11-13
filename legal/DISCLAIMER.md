# Disclaimer

better-ola is a hobby project. It is not a product, not a service, and not supported by any company. It is published as open source for people who want to experiment with their own scooter.

This document is not legal advice. I am not a lawyer. See a qualified professional in your jurisdiction if you need advice.

## Risk to you and to others

You install and run this at your own risk. It changes software on a vehicle. A vehicle is heavy, fast, and can hurt people.

Do not use any part of this project while the scooter is moving. Set it up while the scooter is parked, on a stand, with the motor off. If you need to look at the dash screen, pull over and stop first. A glance at the wrong moment is enough to cause a crash.

## The dash screen is not the instrument cluster

The official Ola interface and the physical warnings on the vehicle remain the authority at all times. The HUD in better-ola is a mirror of data it reads from the CAN bus. It can be wrong. It can be late. It can show a stale value, a value from the wrong frame, or nothing at all. It can fail to start, freeze, or stop updating without warning.

Never use the HUD to decide whether it is safe to brake, turn, charge, or ride. Use the official display, the vehicle's own warning lights and sounds, and your own eyes.

## What this project does and does not touch

The setup path enables wireless ADB through a temporary bind mount that exists only in RAM. Nothing is written to system, vendor, or boot partitions. A reboot removes it.

The HUD reads CAN frames. It does not write CAN frames. It does not send commands to the motor controller, the battery management system, the brakes, or any safety system.

That design reduces risk. It does not remove it. A mistake in the setup scripts, an unexpected firmware change, or a mod app behaving badly can still leave the head unit in a state you did not intend.

## Warranty and service

Modifying the head unit may affect your warranty, your service eligibility, or your insurance. It may also affect how Ola or a dealer treats the vehicle during diagnosis or repair. Those are commercial and contractual matters between you and them. I cannot predict the outcome and I am not responsible for it.

Some dealers may refuse service, reset the unit, or charge for work they would otherwise cover. Assume that is possible before you start.

## Reversibility is not a guarantee

The setup is meant to be reversible by rebooting. Treat that as a design goal, not a promise. Firmware updates, failed installs, or a half finished setup can leave changes behind.

Before you start, make sure you can recover. Know how to reset the head unit, and know what you would do if ADB access stopped working.

## No affiliation

better-ola is not affiliated with, endorsed by, sponsored by, or approved by:

- Ola Electric Mobility Ltd.
- ANI Technologies Pvt. Ltd. (Ola cabs)
- Jio Platforms or Reliance Jio
- Google, for Android or Android Auto
- Apple, for CarPlay

Any of those names appear only to describe what the software talks to or runs on.

## Trademarks

"Ola", "Ola Electric", "S1", "MoveOS", "Android", "Android Auto", "CarPlay", and "Jio" are trademarks of their owners. This project uses them in a descriptive, nominative way: to say which vehicle, which head unit, and which platform the code works with. It does not use them as a brand for this project, and it does not claim any right in them.

In India, section 30 of the Trade Marks Act, 1999 deals with permitted use of a registered mark, including honest descriptive use. That section is the reason this kind of reference is normally acceptable, but the exact scope depends on facts I cannot assess. If a rights holder objects, I will rename or remove references.

If you fork this project, do not add Ola logos, do not use Ola brand assets, and do not present your fork as official or approved.

## Third party code and apps

Parts of this project come from other people. Their licenses apply. See [THIRD-PARTY.md](THIRD-PARTY.md).

Some bundled mod APKs were built by the original author and their source is not public. Redistribution of those files is an open item. Do not assume permission exists until the maintainer confirms it in writing.

## No warranty

The software is provided "AS IS", without warranty of any kind, express or implied, including but not limited to the warranties of merchantability, fitness for a particular purpose, and non infringement.

To the maximum extent permitted by law, the authors and contributors are not liable for any claim, damage, loss, injury, or other liability arising from the use of this software or from anything done with it.

## If you are not the owner

Do not use this on a leased, rented, financed, shared, or employer owned vehicle without the owner's written permission. Do not use it on a vehicle you do not have the right to modify.

## Terms of use

By downloading, installing or running any part of better-ola you agree to the following. If you do not agree, do not use it.

1. **Your vehicle, your decision.** You alone decide to modify your head unit. You confirm you own it or have the owner's written permission, and that you are legally allowed to do this where you live.
2. **You accept the risk.** You accept all risk of loss, damage, injury, death, voided warranty, refused service, bricked hardware, lost data, fines and any other consequence, to you or to anyone else, that comes from using this software. To the fullest extent the law allows, you release the author and contributors from claims arising from that use.
3. **You cover what you cause.** If you share, fork, resell or bundle this software, or use it on someone else's vehicle, and someone makes a claim because of that, it is your responsibility and not the author's.
4. **Follow the law.** Road, vehicle, telecom and consumer rules differ by place. Using a screen while riding may be illegal where you are. Following those rules is on you.
5. **"Tested" is not a guarantee.** A model marked "Tested" worked once on the maintainer's own scooter, on the firmware it had at the time. Firmware updates can break it with no warning.
6. **No support, no uptime.** There is no service, no support obligation and no promise of updates, fixes or availability. The over-the-air updater may stop working at any time.
7. **Some rights cannot be waived.** Where a law gives you rights that cannot be excluded, such as certain consumer protections, those rights stay. Everything else in this document still applies as far as the law allows.
8. **Takedown requests.** A rights holder who objects to anything in this repository can open an issue or contact the maintainer through the GitHub profile `a3ro-dev`. It will be looked at promptly and removed or changed where the objection is valid.

I am not a lawyer and this is not legal advice. Have a qualified professional in your jurisdiction look at this before relying on it.
