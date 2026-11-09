# ⚠️ Disclaimer — Read Before Executing Anything

🇺🇸 **English** · [🇧🇷 Português abaixo](#-português)

---

This repository documents reverse engineering work done strictly for **personal, research, and educational** purposes on the author's own vehicle. Everything provided here is used **entirely at your own risk**.

By using this code or documentation, you explicitly acknowledge that:

1. You are modifying the software/firmware of your own vehicle.
2. You understand this carries a risk of bricking your head unit, and you will bear all replacement costs.
3. You may void manufacturer warranty and insurance coverage.
4. The project authors and contributors accept **NO LIABILITY** for any direct or indirect damage.

## Real Technical Risks

### Head Unit Brick
Modifying `/system` on the head unit can render it inoperable. Community findings report:
- **Soft brick** (bootloop, black screen) — sometimes recoverable via recovery mode.
- **Hard brick** (no response, no power) — requires **hardware replacement** ($200–$400 in used markets, significantly higher at dealerships).

### Loss of Backup / Reverse Camera
The factory head unit screen functions as the display for the vehicle's rear backup camera. If the unit bricks, the camera display will not function.

### Reversion by Dealership Updates
Any modification made to `/system` **will be wiped or overwritten** if an official software update is applied by a dealership or OTA.

### Potential Secondary Effects
The head unit communicates with vehicle CAN buses (F-CAN and B-CAN). While this repository only interacts with high-level Android HMI/launcher components, caution is advised.

## Legal Notices

### Trademarks and Proprietary Software
Honda®, Civic®, Mitsubishi®, Clarion®, Tegra®, and other marks belong to their respective owners. This project is **not affiliated with, endorsed by, or sponsored by** any of these entities.

### Referenced Software
Certain third-party software packages are referenced for educational and research comparison (e.g. the commercial app "Honda Hack" / `cn.autohack.hondahack`). Such software belongs to its original authors and is NOT redistributed by this repository.

### Warranty and Compliance
Modifying automotive electronics may void your vehicle's warranty. Verify local traffic and vehicle regulations in your jurisdiction.

## Limitation of Liability

> **The author(s) and contributor(s) of this repository assume NO RESPONSIBILITY for any damages — property, vehicular, personal, or third-party — resulting from the use of the information contained herein.**
>
> All information is provided **"AS IS"**, without warranty of any kind, express or implied.
>
> If you do not accept these terms in full, **do not execute any code or procedures from this repository.**

---

## 🇧🇷 Português

Este repositório documenta trabalho de engenharia reversa feito em caráter
**pessoal e educativo** no veículo próprio do autor. Qualquer coisa aqui é
usada **inteiramente por sua conta e risco**.

Ao usar este código ou documentação, você reconhece que:

1. Está modificando o firmware do seu próprio veículo.
2. Entende que isso pode brickar a central e você vai arcar com os custos de troca.
3. Pode perder garantia e cobertura de seguro.
4. Os autores do projeto NÃO se responsabilizam por nenhum dano causado.

### Riscos técnicos reais

- **Brick da central**: Modificar `/system` pode torná-la inoperante. Troca de central: R$ 1.000 a 2.000 em mercado de usados.
- **Perda da câmera de ré**: A central exibe a imagem da ré. Sem central = sem câmera de ré.
- **Reversão por update**: Qualquer update oficial na concessionária desfaz modificações em `/system`.

### Isenção de responsabilidade

Toda informação é fornecida **"como está"**, sem garantia de qualquer tipo. Se você não aceita estes termos, **não execute o conteúdo deste repositório**.
