# How to Set Up the Project on Linux

*Language:* 🇺🇸 **English** · [🇧🇷 Português](COMO-USAR.md)

## 📦 Contents of `MIGRATION/`

- `setup.sh` — Script that recreates the full toolchain on Linux (Debian/Ubuntu/Fedora)
- `BRIEFING.md` — Context briefing for AI assistants
- `HOW-TO-USE.md` — This setup guide

---

## 🚚 Cloning & Setup

```bash
git clone https://github.com/praga/honda-civic-headunit.git
cd honda-civic-headunit/MIGRATION
chmod +x setup.sh
./setup.sh
```

### What `setup.sh` Configures:
- Installs Java JDK 17, ADB, Git, curl, unzip
- Downloads apktool and jadx
- Clones reference projects (`root-honda`, `2016PilotOneClick`, `ic1101`)
- Configures workspace under `tooling/`

---

## 🖥️ Verification

Verify that dependencies are installed and available:

```bash
cd honda-civic-headunit
ls tooling/references/         # shows: root-honda, 2016PilotOneClick, ic1101
java -jar tooling/bin/apktool.jar --version
adb --version
python3 -m unittest discover -s tooling/ota-build/tests
```

When all tests pass, your environment is ready to build OTA packages and modify APKs.
