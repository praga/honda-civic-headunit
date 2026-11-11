#!/usr/bin/env bash
# ============================================================
# prepare-pendrive-dump-apks.sh
# ============================================================
# Carrega o pacote OTA READ-ONLY que extrai apenas:
#   - system-build.prop e vendor-build.prop
#   - vendor-app/DaLauncher.apk + DaLauncher.odex
#   - system-framework/ (e vendor-framework/ se existir)
#
# Ideal antes da Fase 10 em unidades de outras regiões (ex: ROM_TYPE=1115)
# sem precisar dumpar 2.8 GB inteiros de APP/CAP.
# Zero modificação na unidade. Pendrive precisa de >= 100 MB.
#
# Uso:
#   ROM_TYPE=0040 ./tooling/scripts/prepare-pendrive-dump-apks.sh <device> [diretorio-de-build]
#
# Guardas, limpeza, cópia e conferência ficam em lib-pendrive.sh.
# ============================================================
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib-pendrive.sh"

prepare_pendrive "DUMP-APKS" "$OTA_BUILD_DIR/output-dump-apks" "python3 tooling/ota-build/build-dump-apks.py" 100 "$@"
echo "Protocolo: porta frontal + AUDIO 5s + confirmar reiniciar."
echo "Esperar ~1-2 min pelo reboot. Quando terminar:"
echo "  - Confira DUMP-APKS.OK na raiz do pendrive"
echo "  - Copie dump-$ROM_TYPE/ para tooling/apks-dumped/dump-$ROM_TYPE/"
