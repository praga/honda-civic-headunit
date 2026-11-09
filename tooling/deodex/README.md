# deodex-apk.sh

Converts an **odex-only** APK (such as all `/system/app/` and `/system/vendor/app/` APKs in dump 0040) into a standalone APK with injected `classes.dex`, ready for static analysis via JADX, apktool, REA, or any standard Android reverse engineering tool.

*Language:* 🇺🇸 **English**

---

## Why This Is Needed

The Mitsubishi head unit ROM (Andromeda / Tegra 3 / Android 4.2.2) **pre-dexes** all system APKs at build time:

- `App.apk` — contains manifest + resources, but no bytecode.
- `App.odex` — optimized Dalvik bytecode, linked against the boot framework (`BOOTCLASSPATH`).

Opening `App.apk` in JADX directly reports `class_count: 1` because there is no `classes.dex` inside. To decompile and analyze the bytecode, it must be deodexed.

---

## What the Script Does

1. Locates `<APK>.odex` adjacent to the input APK.
2. Builds a temporary bootclasspath combining `/system/framework/*.odex` + `/system/vendor/framework/*.odex` from the system dump.
3. Runs `baksmali deodex` → produces `.smali` tree.
4. Runs `smali assemble` → produces `classes.dex`.
5. Re-packages the APK with injected `classes.dex` via `zip -j`.
6. Cleans up temporary `/tmp` files upon exit.

Output is saved to `tooling/apks-decoded/<APKName>-deodexed.apk`.

---

## Usage

```bash
./tooling/deodex/deodex-apk.sh tooling/apks-dumped/dump-0040/vendor-app/DaLauncher.apk
```

### Configuration Options

| Variable | Default | Purpose |
|---|---|---|
| `API_LEVEL` | `17` | Target API level (Android 4.2.2 = 17) |
| `OUT_DIR` | `tooling/apks-decoded` | Directory where output APK is stored |
| `BOOTCLASSPATH_SRC_DIRS` | `dump-0040/{system-framework,vendor-framework}` | Framework directories with `.odex` files, separated by `:` |
| `BAKSMALI_JAR` | `tooling/references/ic1101/apk-rebuilder/input/baksmali-2.5.2.jar` | Baksmali jar path |
| `SMALI_JAR` | `tooling/references/ic1101/apk-rebuilder/input/smali-2.5.2.jar` | Smali jar path |

---

## Prerequisites

- JDK 17+ in `PATH`
- `zip` utility in `PATH`
- Baksmali/smali 2.5.2 jars
- System framework dump in `tooling/apks-dumped/dump-0040/{system-framework,vendor-framework}`
