# Technical Discoveries

> Navigable index of findings regarding the Mitsubishi Andromeda / Honda Civic G10 head unit, discovered across all project phases. Each entry links directly to the detailed documentation containing full evidence.
>
> *Language:* 🇺🇸 **English** · [🇧🇷 Português](DISCOVERIES.pt-BR.md)
>
> This is not a tutorial. To deploy to your head unit, see [`QUICKSTART.md`](QUICKSTART.md). To see current status + next steps, see [`ROADMAP.md`](ROADMAP.md). For the complete platform atlas (partitions, APKs, services, pipeline), see [`docs/MAPA.md`](docs/MAPA.md) / [`docs/en/MAP.md`](docs/en/MAP.md).

---

## The Platform (Internal Architecture)

Full static mapping performed via REA + JADX across 160 APKs/libs from dump 0040. These datasets are reusable for any Mitsubishi Andromeda head unit sharing the same framework.

| Metric | Quantity | Documentation |
|---|---|---|
| Internal AIDL Interfaces (Binder IPC) | 364 across 63 domains | [`docs/en/raio-x/01-apis-aidl.md`](docs/en/raio-x/01-apis-aidl.md) |
| Broadcasts (`com.mitsubishielectric.*` / `com.honda.*`) | 445 across 48 prefixes | [`docs/en/raio-x/02-broadcasts.md`](docs/en/raio-x/02-broadcasts.md) |
| ContentProviders (Authorities) | 27 (9 vendor + 18 AOSP) | [`docs/en/raio-x/03-content-providers.md`](docs/en/raio-x/03-content-providers.md) |
| System Properties Referenced | 48 across 4 prefixes | [`docs/en/raio-x/04-system-properties.md`](docs/en/raio-x/04-system-properties.md) |
| Shipping APKs (vendor + system) | 110 (11 running as `android.uid.system`) | [`docs/en/raio-x/05-trust-map.md`](docs/en/raio-x/05-trust-map.md) |

---

## PowerManager — How the Head Unit Recovers from LP0 Suspend

The Mitsubishi PowerManager **does not maintain an internal list** of active HMI services. It operates purely as pub/sub: it emits 16 broadcasts at key power cycle transitions, and any app with a registered receiver responds.

- **16 broadcasts from `PowerManagerBroadcastSender`** mapped (start, stop, pre-stop, restart + 10 phase-specific shutdown variants + camera resume + animation + VBUS).
- **33 shipping APKs** listen for `ACTION_HMI_SERVICE_START` — this is the universal contract.
- **Known startup trigger causes**: `"ColdBoot"`, `"ExitLP0"`, `"AccOnRestart"`.
- **AutoOk v0.4.0+** uses this exact pattern (`HmiServiceReceiver`) — it aligns with the official platform paradigm rather than a hacky polling loop.

→ See [`docs/MAPA.md §11.6`](docs/MAPA.md) and [`docs/en/whitelist-mitsubishi-2026-10-19.md §6`](docs/en/whitelist-mitsubishi-2026-10-19.md).

---

## Mitsubishi Whitelist — End-to-End Mechanism

The "Mitsubishi Whitelist" frequently appears in adjacent reverse engineering projects (e.g. `codeage/root-honda`) as an assumed security control mechanism. However, **it is purely an app installation UX filter**. Only `AppManager.apk` (official installer) and `FileManager.apk` query it via binder IPC. No HMI services or PowerManager routines consult it.

Detailed mapping:
- Shell APK: `com.mitsubishielectric.ada.appservice.whitelist` (runs as `android.uid.system`).
- Core logic: `WhitelistLib.jar` in `vendor-framework/` (15 classes).
- XML carrier APK: `com.honda.displayaudio.whitelist.apk` (14 KB, located at `/system/vendor/etc/honda/`).
- Runtime XML: `/data/data/whitelist-1.0.xml`.
- AIDL Interface exposing 7 methods: `checkInstallPackage`, `getPackageNames`, `getApplicationName`, `getAudioInfo`, `getAudioStreamType`, `getRegulation`, `getRevert`.
- Full XML schema mapped (`<package>`, `<keyStore>`, `<versionCode>`, `<regulation>`, `<revert>`, `<audioStreamType>`, `<withAudio>`).

→ See [`docs/en/whitelist-mitsubishi-2026-10-19.md`](docs/en/whitelist-mitsubishi-2026-10-19.md).

---

## Warning Screen Flow (`OpeningDivideActivity`) Internals

The warning disclaimer suppressed by this project is triggered by `com.mitsubishielectric.ada.app.dalauncher.Launcher$HMIServiceReceiver.onReceive()` upon receiving `ACTION_HMI_SERVICE_START` from the PowerManager.

- Main Activity: `com.mitsubishielectric.ada.app.dalauncher/.OpeningDivideActivity` (extends `HomeBaseActivity`).
- Layout: `R.layout.opening_divide`.
- 14 classes reference this Activity (confirmed via `trace-android-references`).
- 19 fields and 37 public methods mapped (including `startNextActivity`, `createDisclaimerOnHomeIntent`, `antiTheftUnlockScreenTransition`).
- Decompiled `onCreate` — the "accepted vs unaccepted" decision logic resides inside `DisclaimerFragment`.

→ See [`docs/MAPA.md §11.1.1`](docs/MAPA.md).

---

## The Disclaimer Gate — `sDisclamerFlg` is an In-Memory Static Boolean 🎯

Breakthrough discovered on 2026-10-20 that unlocked **Phase 10**. Inside `OpeningDivideActivity`:

- Field: `public static sDisclamerFlg:Z` (static boolean, initialized to `false` in `<clinit>`).
- **Never persisted** in SharedPrefs, SQLite, or system properties — it exists solely in class memory.
- Resets to `false` on every class load (every cold boot spawns a fresh Dalvik JVM) → explains why the warning appears every single time the car turns on.
- In `createDisclaimerOrEcoIntent()` (line 869 of deodexed smali): tests this flag and branches to either `createDisclaimerOnHomeIntent()` (`DisclaimerFragment`, the warning) or `createEcoIntent()` (`EcoFragment`, inert route in daily usage).
- Because the launcher is signed with Mitsubishi vendor keys, we cannot simply re-sign with platform keys unless re-signed with the AOSP testkey accepted by the recovery!
- **Phase 10 Smali Patch (1 instruction):** Change conditional jump `if-nez v0, :cond_9` to unconditional `goto :cond_9` at `OpeningDivideActivity.smali:671`.
- When bypassed via `goto :cond_9`, it emits `DISCLAIMER_COMPLETE`, satisfying all downstream dependencies cleanly.
- 4 other vendor APKs listen for `DISCLAIMER_COMPLETE`: `CameraAPService`, `SystemInfoService`, `VehicleCoordination`, `AcuraLinkApService`.

→ See [`docs/en/raio-x/07-services-re.md`](docs/en/raio-x/07-services-re.md) and [`docs/MAPA.md §11.1.1`](docs/MAPA.md).

---

## Hall of Fame: Resolved Bugs & Fixes

14 critical bugs with identified root causes and documented fixes — valuable for anyone doing Mitsubishi RE:

- **Logcat needle with `.` vs `/`**: ActivityManager prints `pkg/.ClassName`, not `pkg.ClassName`.
- **APK dies after first tap**: `stopSelf()` after tap killed the background service; fixed by implementing `rearmWatcher()`.
- **Rom_type `0040` discovery**: Discovered through trial-and-error across 352 OTA permutations until recovery accepted it.
- **BR recovery lacks `/sbin/sh`**: Required embedding statically compiled ARM `busybox`.
- **D8 dexer NullPointerException**: Anonymous inner classes caused crashes on older dexers; converted to explicit named inner classes.
- **EPERM on logcat `Process.destroy()`**: Non-root apps cannot kill root processes; fixed in v0.3.0.
- **APK unresponsive after LP0**: Logcat watcher child thread became zombie after system killed child; resolved in v0.4.0 with dedicated broadcast receiver.

→ See [`docs/MAPA.md §17`](docs/MAPA.md).

---

## Discarded Paths (and Why)

Paths investigated, ruled out, and documented to avoid reinventing the wheel:

| Route | Reason for Abandonment |
|---|---|
| **Whitelisting in Mitsubishi whitelist** | Whitelist is UX-only for installations. Does not affect LP0. AutoOk v0.4.0 already follows official platform standards. |
| **SuperSU as mediator** | Our `su` is a simple setuid binary. SuperSU introduces interactive UI prompts that block non-interactive `Runtime.exec("su")` calls, breaking automation. |
| **Xposed Framework** | Modifying `/system/bin/app_process` is destructive. Zero public precedent on Mitsubishi Andromeda. Extremely high bootloop risk. |
| ~~**Modifying `DaLauncher.apk` via smali**~~ | **Completed successfully as Phase 10 (2026-10-29)** following the discovery of the `sDisclamerFlg` gate. Single instruction patch, re-signed with AOSP testkey, deployed via OTA. |

→ See [`docs/MAPA.md §16`](docs/MAPA.md).

---

## Reusable Tools & Workflows

In addition to car-specific findings, this repository contains reusable tooling:

- **`tooling/deodex/deodex-apk.sh`**: Automates `.odex` → `classes.dex` → `.apk` reconstitution for any Mitsubishi Andromeda APK. Validated on 160 APKs.
- **REA + MCP Workflow**: Headless MCP integration of REA / JADX for deep binary querying.
- **EvilValet OTA with AOSP testkey**: OTA update construction signed with public AOSP testkeys accepted by the recovery (rooted in [juniperspring.org](https://juniperspring.org/posts/honda-evil-valet/)).
- **Deep-Scan OTA Package**: Dumps `/system`, `/data/system/packages.xml`, `/proc/*`, and partition layouts in a single key-on pass. Produces `scan.tar.gz` (897 KB).

→ See [`tooling/deodex/README.md`](tooling/deodex/README.md) and [`docs/MAPA.md §20.8`](docs/MAPA.md).
