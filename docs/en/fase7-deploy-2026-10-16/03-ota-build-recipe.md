# OTA Package Build Recipe

How to build an OTA update package accepted by the Brazilian 2017 Civic head unit. Covers the installation package (`AutoOk.apk` + `su`) and rollback package (clean removal of both) for firmware `1.F187.16`, `rom_type` `0040`.

*Language:* 🇺🇸 **English** · [🇧🇷 Português](../../../docs/fase7-deploy-2026-10-16/03-build-ota-receita.md)

---

## EvilValet Fundamentals

### Upstream Sources

1. **Vulnerability Disclosure:** [juniperspring.org/posts/honda-evil-valet](https://juniperspring.org/posts/honda-evil-valet/) by librick / Eric McDonald (2026-06-13).
2. **Technical Implementation Spec:** [`tooling/references/ic1101/docs/updates.md`](../../../tooling/references/ic1101/docs/updates.md) by the same author.

### Technical Mechanics

1. The public AOSP test key certificate (`testkey.x509.pem`) is baked into `res/keys` inside the head unit's recovery partition.
2. The Honda/Mitsubishi recovery binary relies on stock AOSP `verify_file()` logic.
3. Any OTA update package formatted as `SwUpdate.mdt` and signed using the public AOSP `testkey.pk8` passes signature verification.
4. When `daupdater` detects a valid update for its `custom_rom.type` on cold boot, it copies the payload and triggers recovery flashing automatically.

---

## The Build Pipeline (`tooling/ota-build/build.py`)

Our Python build script monkey-patches `ic1101/ota-builder` to adapt it to Brazilian firmware:

```python
# build.py core logic summary:
# 1. Target rom_type = 0040
# 2. Package busybox in /tmp/busybox
# 3. Mount /system (ext4 /dev/block/platform/sdhci-tegra.3/by-name/APP)
# 4. Copy AutoOk.apk to /system/app/
# 5. Copy su to /system/xbin/ and chmod 06755
# 6. Break updater loop: mv SwUpdate2.txt .done && rm /cache/.copy_complete
# 7. Sign output SwUpdate.mdt with AOSP testkey
```

### Running the Build

```bash
# Build installation package
python3 tooling/ota-build/build.py

# Build rollback package
python3 tooling/ota-build/build-rollback.py
```

Output:
```
INFO: rom_type=0040 -> tooling/ota-build/output/<timestamp>/0040/SwUpdate.mdt
OK: tooling/ota-build/output/<timestamp>
```

---

## USB Flash Drive Layout

A FAT32 MSDOS formatted drive must have the following structure:

```
/Volumes/HONDA_OTA/
├── SwUpdate2.txt                # Copy trigger containing build metadata
└── 0040/
    └── SwUpdate.mdt             # Signed OTA archive containing updater-script & payload
```

Flash drive writing is automated via:

```bash
sudo ./tooling/scripts/prepare-pendrive-install.sh /dev/sdX
```
