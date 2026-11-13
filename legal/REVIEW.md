# Risk review for the maintainer

This is my own reading of the risks in publishing better-ola. It is not legal advice. I am not a lawyer, and none of this is a conclusion about what a court would do. Where I cite a section I am reasonably confident it exists and is on point. Where I am not, I say "check" and you should verify, ideally with an Indian lawyer who handles IP and telecom work.

Ranking at a glance, highest first:

1. Redistributing Anand's closed APKs (b)
2. "Ola" in the product name (a)
3. Road safety and distraction (e)
4. Reverse engineering and the IT Act (c)
5. The Jio SIM idea (d)
6. Warranty exposure (f)
7. GPL-3.0 if QuickBall code is copied (g)

The safety item is ranked third because the legal exposure is real but the harm is worst. Fixing the name and the APKs is cheap. Safety depends on the users, which is why it lives in the docs.

## (a) "Ola" in the name "better-ola"

What the risk is: a product name that leads with someone else's registered mark is the kind of use that draws objections. It can be framed as infringement under section 29 of the Trade Marks Act, 1999, or as passing off, which is preserved separately. The phrasing "better-ola" also reads as a claim about the official thing being improved, which is the part that worries me most. It suggests a relationship that does not exist.

What helps: section 30 of the same Act allows certain honest uses, including descriptive references to the goods a product works with, and there is case law supporting "for X" style references. That covers a sentence like "works with the Ola S1". It covers a product name far less well. Check the exact clauses of section 30 before relying on it.

Also relevant if this ever reaches an app store: Google Play and the App Store both have impersonation rules, and a name that looks official gets rejected or removed regardless of what a court would say.

Safer options, in order of how little they cost you:

- Rename the product to something that does not contain the mark, and keep the mark in the descriptive subtitle only. For example "better-s1" with "an unofficial toolkit for the Ola S1". Same code, same repo history, new name in the docs, app label, and site heading.
- Or drop the mark entirely: "S1 Dash Kit", "Dashmate", "S1 Companion", "better-scooter".
- Avoid leading with "MoveOS" for the same reason. It is Ola's platform name.
- Keep the GitHub repo path if you do not want to break links, but do not use it as the product name on the site, in the app label, or in release titles.
- Never use Ola's logo, wordmark, colours, or screenshots of their app as your icon or branding.
- Keep one clear line everywhere: "Unofficial. Not affiliated with or endorsed by Ola Electric."

My recommendation: rename now, while the project is small. Renaming later is worse and costs you the little recognition you have.

## (b) Redistributing Anand's closed APKs

What the risk is: three binaries with no public source, shipped inside your repository and your releases. His MIT license covers the code he published. It does not automatically cover binaries whose source he never published. If he did not grant redistribution, you are distributing someone else's work without a license, which is the plainest copyright problem in this project. Section 51 of the Copyright Act, 1957 defines infringement and section 63 makes some infringement a criminal offence. Nobody wants to test that over a floating ball app.

Second layer: even with his permission, you do not know what is inside those APKs. Third party libraries, ad SDKs, copied icons, or code he got from elsewhere could be in there. His permission cannot cover what he does not own.

Third layer: the APKs are granted overlay, accessibility, and write settings permissions by your setup tool. If one of them does something harmful, you shipped it and you granted the permissions. That is the fact pattern you least want.

Options:

- Get written permission from Anand that covers redistribution in this repo, in releases, and by forks. Keep it somewhere public, or at least keep it and quote it in the README.
- Better: do not bundle them. Point users at his release page. The setup flow already knows how to install an APK from a file.
- Or publish source for your own replacements and drop the closed ones entirely. That is the only version where the whole repo is actually open source.

Do not leave this as a "probably fine" item. It is the single biggest legal exposure here, and it is fixable in an afternoon.

## (c) Reverse engineering and the IT Act

What helps: section 52(1)(ab) of the Copyright Act, 1957 permits acts necessary to obtain information essential for interoperability of an independently created program with other programs. Reading a local gRPC service to make your own client talk to it, and reading CAN frames that the vehicle already broadcasts, is close to the centre of what that provision is for. The repo does not ship vendor code, keys, or firmware.

What still bites:

- The provision is a defence to specific acts. It does not authorize distributing someone else's code, and it does not answer contract questions.
- The head unit software is licensed, not sold. Its terms may prohibit reverse engineering, modification, or third party access. Breaching those terms is a contract problem even when copyright is fine. Nobody reads the EULA on a scooter. Someone should.
- Section 43 of the IT Act, 2000 covers access to a computer resource without the permission of the owner, and section 66 covers computer related offences. Your users own the scooter, which is the strongest fact you have. But the software is licensed to them, and "owner of the device" and "owner of the software" are not the same thing. Keep the argument about their own device front and centre, and do not build features that touch anyone else's vehicle.
- Check whether the terms of service for MoveOS or the Ola app restrict this, and whether an OTA update could be characterized as circumventing a technical measure. Sections 65A and 65B of the Copyright Act deal with circumvention. I am not confident they apply here. Check.
- Distribution risk: publishing the method is generally safer than publishing extracted vendor code. Do not publish firmware dumps, keys, or signing material, ever.
- The Axiom data you use came from someone else's reverse engineering. That is their risk to carry for the extraction, and yours to carry for redistributing the data without its Apache-2.0 notices. Keep the notices.

Keep the language in the docs factual: independent client, own hardware, read only on CAN, no vendor code redistributed. Do not overclaim a legal right. "Permitted by section 52(1)(ab)" is a claim you cannot make for every jurisdiction.

## (d) The Jio SIM idea

Not built, and it should stay that way. Reasons, in order:

- The SIM is a connectivity plan contracted by Ola, not by the rider. The subscriber is someone else. Using their plan for your traffic is use without the subscriber's consent, and almost certainly against the terms.
- It is a paid M2M plan sized for telematics, not for personal browsing. Routing personal traffic over it takes capacity someone is paying for, and could get the vehicle's connectivity suspended. That means losing remote features, diagnostics, and possibly the owner's ability to use the app.
- Telecom services in India are regulated under the Telecommunications Act, 2023, which replaced the older framework and gives the government broad powers over unauthorized use and interception. I am not going to cite a section I am not sure of. Check with a telecom lawyer before even documenting the idea.
- If someone did it, they could create KYC and liability problems for the registered subscriber, and expose traffic that is not theirs to a network they do not control.
- Publishing a working method would move this from "someone else's idea" to "maintainer distributed a tool for it". That is the difference between a footnote and a problem.

If the goal is internet in the scooter, the clean path is a separate SIM or hotspot you control. Say that in the docs once, and do not describe the other path. If the feature request comes up, close it with that sentence.

## (e) Road safety, distraction, and the Motor Vehicles Act

What the risk is: a dash overlay that shows speed, indicators, and warnings invites people to look at it while riding. That is the harm this project can actually cause, and no license text fixes it.

- Section 184 of the Motor Vehicles Act, 1988 covers dangerous driving. The 2019 amendment raised the penalties. Whether glancing at a screen meets that threshold depends on the facts, but a crash with an unofficial overlay on the dash is a bad set of facts.
- State rules and police practice on using screens and phones while riding vary. Check your state's rules.
- Insurance is a contract. An insurer looking at a modified head unit after a claim has an argument it would not otherwise have.

What to do:

- Keep the parked setup warning at the top of the README, not buried.
- Keep "the official cluster is the authority" in the app itself, on the HUD's first run and in its settings.
- Do not build anything that encourages interaction while moving: no scrolling menus, no touch targets, no notifications on the overlay.
- Do not add a speedometer as the headline feature in the marketing copy. Frame the HUD as a mirror for parking and diagnostics, and say plainly that it can be wrong.
- Do not claim it improves safety. It does not, and that claim would be used against you.

## (f) Warranty and service

Using ADB on the head unit, installing mod apps, and reading CAN frames are all things a dealer can point at. Expect arguments about warranty coverage even for unrelated faults. The bind mount is in RAM and a reboot clears it, which helps, but does not settle the contract question.

Say so in the disclaimer, do not promise service outcomes, and do not tell users it is "undetectable". It is not a claim you can support, and it makes the project look like it is selling evasion.

## (g) GPL-3.0 if QuickBall code is ever copied

Right now QuickBall is referenced as prior art only, so there is no obligation. If any code from it lands in this repo, the combined work must be distributed under GPL-3.0, with complete corresponding source, and the MIT parts cannot be the only license on the result. MIT code can be included in a GPL project, but the whole distributed work becomes GPL.

Two details worth knowing before that happens:

- GPL-3.0 has rules about "User Products" and installation information. A mod app that runs on a locked-down head unit is the kind of context where that clause gets argued about. Check section 6 of the license.
- Shipping a closed APK alongside GPL code does not make the APK GPL, but it does raise the question of whether they form one work. Combining them in one installer is the risky version.

Simplest rule: if you want to stay MIT, read QuickBall for ideas and write your own implementation from scratch, without copying structure, names, or code. If you copy, relicense the whole thing to GPL-3.0 and say so clearly.

## (h) Before publishing: checklist

- [ ] Decide the name. Remove "Ola" from the product name, app label, site title, and release titles. Keep it in descriptive text only.
- [ ] Add one clear "unofficial, not affiliated" line to the README, the app's about screen, and the site footer.
- [ ] Get written permission from Anand for the three APKs, or stop bundling them.
- [ ] Confirm the APKs contain nothing he does not own, in writing if possible.
- [ ] Keep Anand's copyright line and MIT text. Do not edit his license file.
- [ ] Add a NOTICE file with the Axiom attribution, and ship a copy of Apache-2.0.
- [ ] Add a "changes were made" line for the CAN data files.
- [ ] Generate the Android dependency license list for a release build and check for anything non permissive.
- [ ] Put the parked-only and "official cluster is the authority" warnings at the top of the README and in the HUD's first run.
- [ ] Remove any wording that promises safety, undetectability, or warranty outcomes.
- [ ] Confirm nothing in the repo contains firmware, keys, or vendor code.
- [ ] Confirm the Jio SIM path is not documented, not coded, and not hinted at.
- [ ] Add a contact address for takedown or licence complaints.
- [ ] Read the MoveOS and Ola app terms once, and note in the README that the user is responsible for them.
- [ ] Have an Indian lawyer look at (a), (b), and (d) before any public launch push. Not the whole repo. Those three.
