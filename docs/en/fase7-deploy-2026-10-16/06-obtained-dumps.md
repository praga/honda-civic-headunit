# Extracted Dumps — Complete Catalog

During Phase 7, we built an OTA dump package that copied 197MB of `/system` partitions directly to a USB flash drive without requiring ADB. This dump represents the true baseline state of Brazilian Civic 2017 `1.F187.16` firmware prior to any modifications.

*Language:* 🇺🇸 **English** · [🇧🇷 Português](../../../docs/fase7-deploy-2026-10-16/06-dumps-obtidos.md)

---

## Directory Location

```
tooling/apks-dumped/dump-0040/
```

Total size: ~197 MB.

---

## How It Was Collected

A diagnostic OTA update script was executed in recovery mode:

```sh
# Via busybox (extracted to /tmp/busybox)
mount -o ro /dev/block/platform/sdhci-tegra.3/by-name/APP /tmp/sys
mount -o ro /dev/block/platform/sdhci-tegra.3/by-name/CAP /tmp/ven

cp /tmp/sys/build.prop /mnt/usbdrive1/dump-0040/system-build.prop
cp /tmp/ven/build.prop /mnt/usbdrive1/dump-0040/vendor-build.prop

cp -a /tmp/sys/app/.         /mnt/usbdrive1/dump-0040/system-app/
cp -a /tmp/sys/priv-app/.    /mnt/usbdrive1/dump-0040/system-priv-app/
cp -a /tmp/sys/framework/.   /mnt/usbdrive1/dump-0040/system-framework/
cp -a /tmp/ven/app/.         /mnt/usbdrive1/dump-0040/vendor-app/
cp -a /tmp/sys/etc/.         /mnt/usbdrive1/dump-0040/system-etc/
```

---

## Catalog Contents

- **`system-build.prop`**: Full system properties file for `/system` (`APP`).
- **`vendor-build.prop`**: Vendor properties file containing `custom_rom.type=0040`.
- **`system-app/`**: 42 factory AOSP and core system APKs and `.odex` files.
- **`vendor-app/`**: 78 Mitsubishi / Clarion automotive APKs and `.odex` files.
- **`system-framework/`**: Framework JARs and bootclasspath dependencies.
- **`system-etc/`**: System configuration files and permission manifests.
