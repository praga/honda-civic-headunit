# 🇺🇸 English Documentation

Comprehensive documentation to understand, deploy, or adapt the project.

*Language:* 🇺🇸 **English** · [🇧🇷 Português](../pt-BR/README.md)

---

## Index

| Document | Description |
|---|---|
| [`00-overview.md`](00-overview.md) | What the project does, architecture, and threat model |
| [`01-attack-architecture.md`](01-attack-architecture.md) | How the EvilValet attack works |
| [`02-prerequisites.md`](02-prerequisites.md) | Hardware, software, keystores, and dependencies |
| [`03-finding-your-firmware.md`](03-finding-your-firmware.md) | How to adapt for other trims and firmware builds |
| [`04-building-the-package.md`](04-building-the-package.md) | Compiling the OTA update package step-by-step |
| [`05-deploying-in-car.md`](05-deploying-in-car.md) | Step-by-step in-car deployment protocol |
| [`06-troubleshooting.md`](06-troubleshooting.md) | Decision tree and diagnostic logs |
| [`07-rollback.md`](07-rollback.md) | Reverting everything back to 100% factory state |
| [`08-backups.md`](08-backups.md) | Complete backup catalog |
| [`09-fase10-remove-warning.md`](09-fase10-remove-warning.md) | 🎯 Phase 10 — permanent warning elimination via DaLauncher smali patch |
| [`MAP.md`](MAP.md) | 🗺️ Head unit navigable system atlas (partitions, APKs, IPC, bug hall of fame) |
| [`fase10-deploy-2026-10-29/README.md`](fase10-deploy-2026-10-29/README.md) | 📋 Phase 10 deployment timeline, gate logs, bytecode proof, Phase 10.1 |
| [`fase10-route-decision-2026-10-23.md`](fase10-route-decision-2026-10-23.md) | Analysis choosing Route B (AOSP re-sign) over Route A (odex patching) |
| [`fase10-execution-2026-10-27.md`](fase10-execution-2026-10-27.md) | Execution checklist and test protocol used on deploy day |
| [`fase10-pre-planning-2026-10-20.md`](fase10-pre-planning-2026-10-20.md) | Initial discovery of `sDisclamerFlg` static boolean gate |
| [`fase7-deploy-2026-10-16/README.md`](fase7-deploy-2026-10-16/README.md) | 🚀 Phase 7 — AutoOk APK + su initial deployment |
| [`whitelist-mitsubishi-2026-10-19.md`](whitelist-mitsubishi-2026-10-19.md) | 🔬 Full static analysis of the Mitsubishi installation whitelist |
| [`next-session.md`](next-session.md) | Quick operational reference for resuming work |
| [`raio-x/README.md`](raio-x/README.md) | 🔬 X-Ray Layer 1 overview (364 AIDLs, 445 broadcasts, trust map) |
| [`raio-x/01-apis-aidl.md`](raio-x/01-apis-aidl.md) | AIDL interfaces (364 unique contracts) |
| [`raio-x/02-broadcasts.md`](raio-x/02-broadcasts.md) | Broadcast actions (445 across 48 prefixes) |
| [`raio-x/03-content-providers.md`](raio-x/03-content-providers.md) | ContentProvider authorities and URIs |
| [`raio-x/04-system-properties.md`](raio-x/04-system-properties.md) | System properties referenced in bytecode |
| [`raio-x/05-trust-map.md`](raio-x/05-trust-map.md) | Trust map — sharedUserId & permissions per APK |
| [`raio-x/06-diag-apks.md`](raio-x/06-diag-apks.md) | Diagnostic APKs — 91 DealerDiag screens classified |
| [`raio-x/07-services-re.md`](raio-x/07-services-re.md) | Core services reverse engineering |
| [`raio-x/08-github-landscape-2026-10-27.md`](raio-x/08-github-landscape-2026-10-27.md) | Landscape of 33 adjacent GitHub repositories |
| [`raio-x/09-camera-re.md`](raio-x/09-camera-re.md) | Reverse camera IPC, AIDL, broadcast, & CAN reverse signal |

---

## Recommended Learning Path

### Getting Started & Deploying
1. [`00-overview.md`](00-overview.md) — high-level context.
2. [`../../DISCLAIMER.md`](../../DISCLAIMER.md) — understand the real risks.
3. If firmware `1.F187.16` BR: follow [`../../QUICKSTART.md`](../../QUICKSTART.md).
4. If other firmware: [`03-finding-your-firmware.md`](03-finding-your-firmware.md).

### Deep Technical Research
1. [`MAP.md`](MAP.md) — Navigable atlas of all partitions, binaries, and APKs.
2. [`fase7-deploy-2026-10-16/`](fase7-deploy-2026-10-16/) — 7 technical documents covering discovery narrative and technical recipes.
3. [`fase10-deploy-2026-10-29/README.md`](fase10-deploy-2026-10-29/README.md) — Smali patching of `DaLauncher.apk`.
4. [`raio-x/`](raio-x/) — Complete scan of all 364 AIDL interfaces, broadcasts, and permissions.

---

## Tested Firmware

- **Honda Civic 2017 EXL Brazil**, DA version `1.F187.16`, `rom_type` `0040`. ✅ Working in production.

---

## Contributing

See [`../../CONTRIBUTING.md`](../../CONTRIBUTING.md) to report new firmwares or contribute bug fixes.

## License

Code: MIT. Documentation: CC BY 4.0.
