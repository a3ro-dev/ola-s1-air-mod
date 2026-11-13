# Third party components

This file lists what better-ola uses from other people, under what license, and what that license requires. It is not legal advice, and I am not a lawyer.

better-ola is a fork of Anand's project. His MIT license text and copyright line stay in place.

## 1. Anand's original project (MIT)

- Project: https://github.com/Asjohn720/ola-s1-mods
- Author: Anand (@Asjohn720)
- License: MIT. Copyright (c) 2026 Anand (@Asjohn720) and Contributors.
- Used for: the original setup flow, the ADB and gRPC approach, the scripts, and the basis of the fork.

Obligations:

- Keep the copyright notice and the permission notice. Do not remove Anand's name from the license or from the credits.
- Keep the "AS IS" and liability text.
- If you ship a modified version, you may add your own notice, but you may not remove his.

Anything I add is my own work and is distributed under the same MIT terms unless a file says otherwise.

## 2. CAN signal data from Axiom (Apache-2.0)

- Origin: Axiom, https://codeberg.org/pitzel/axiom
- License: Apache License 2.0
- Redistributed by: https://github.com/itsmobsys/ola-s1-rea (MIT, reverse engineering notes)
- Files used: `can_database.json` (signal table) and `faults.json` (fault codes). These describe CAN IDs, bit offsets, scaling, and fault text. They are data, not code, but they come from an Apache-2.0 project.

Obligations, and these apply because the data originates under Apache-2.0:

- Include a copy of the Apache License 2.0 with the distribution.
- Keep any NOTICE file the origin project ships, and reproduce its attributions. Check the Axiom repo for a NOTICE file before publishing.
- State clearly that the files were modified, if they were. A short line in the file or next to it is enough, for example "derived from Axiom's can_database.json, values adjusted for readability".
- Do not imply the Axiom authors endorse this project.
- Do not use their names or marks in a way that suggests endorsement.

Note on the license chain: the MIT repo that republished the files cannot loosen the Apache-2.0 terms on them. Treat the data as Apache-2.0. The MIT license on the notes applies to the notes, not to the underlying data.

## 3. QuickBall (GPL-3.0), reference only

- Project: https://github.com/chayanforyou/QuickBall
- License: GNU General Public License v3.0
- Used for: prior art and idea reference only. No code is copied.

If code from QuickBall is ever copied in, the situation changes. See item (g) in [REVIEW.md](REVIEW.md). Until then there is no GPL obligation from this project.

## 4. Bundled prebuilt mod APKs

These are installed by the setup tool. Their source is not public.

| Package | Author | Source | License |
| --- | --- | --- | --- |
| com.asjohn720.ball | Anand | not public | not stated |
| com.asjohn720.musicmod | Anand | not public | not stated |
| com.asjohn720.androidauto | Anand | not public | not stated |

Status: OPEN ITEM, must be resolved before publishing.

Redistributing these binaries is only acceptable if Anand has given permission. His MIT license on the repository covers the code he published, and that code is not the source of these APKs. A closed binary is not automatically covered by the repository's license.

The maintainer must get a clear, written statement from Anand that covers:

- redistribution of the exact APK files in this repository, and in release artifacts;
- redistribution by forks and by anyone who mirrors the repo;
- whether the permission can be passed on, or ends with this project;
- whether he wants an attribution line, and in what wording.

Until that exists, the safe options are: do not bundle the APKs, or link to his release page and let users download from him.

Even with permission, note that a permission from the author does not settle whether the APK itself contains third party code, ad SDKs, or copied assets from someone else. Ask him to confirm the APKs are his own work.

## 5. Platform and framework pieces

The Android app is built with the standard Android toolchain and its libraries. Those carry their own licenses, mostly Apache-2.0. A release build should include the license notices the toolchain generates.

Status: check. Generate the dependency list for an actual release build and add anything that is not Apache-2.0 or MIT. If a dependency is GPL or has a non commercial term, it needs its own note here.

## 6. What is not included

- No Ola firmware, no vendor binaries, no private keys, no signing keys, no account credentials.
- No Ola logos or brand assets.
- No code from QuickBall.

## 7. Attribution wording used in the project

Suggested credit line for the README and for release notes:

> Based on ola-s1-mods by Anand (@Asjohn720), MIT. CAN signal data derived from Axiom (Apache-2.0), via ola-s1-rea. Mod APKs by Anand, used with permission. [confirm before publishing]

Adjust the last clause once the permission in item 4 exists, or remove the APKs.

## 8. Files to ship with a release

- `LICENSE` with Anand's copyright line.
- `NOTICE` with the Axiom attribution, once checked.
- A copy of the Apache License 2.0.
- This file, or its content, inside the distributed archive.
- License text for any dependency that requires it.

Keep the text of each license unmodified. Do not paraphrase a license inside a file that claims to be the license.
