# PROTOCOLO — Decisões de classificação da frota

Registro das decisões tomadas para classificar os 30 aparelhos de `app/src/main/assets/frota.json`
(frota `SELDON-BR-01`, exportada em 2026-10-20). Cada decisão segue o formato
**decisão → alternativas → consequência**. A regra é declarada antes dos resultados.

---

## 1. Regra adotada

**Decisão.** Usar os limiares de referência da especificação, sem regra alternativa.

| Ordem | Estado | Condição (basta uma) |
|---|---|---|
| 1 | INDETERMINADO | capacidade ≤ 0 ou ausente · tela ligada ≤ 0 ou ausente · energia ausente ou não convertível · falhas ausentes · quadros perdidos ausentes |
| 2 | CRÍTICO | dreno relativo ≥ 12 %/h · falhas (7 d) ≥ 3 · quadros perdidos ≥ 15 % |
| 3 | ATENÇÃO | dreno relativo ≥ 7 %/h · falhas (7 d) ≥ 1 · quadros perdidos ≥ 8 % |
| 4 | OK | nenhuma das anteriores |

- **Precedência:** o primeiro estado da tabela cuja condição é satisfeita vence. Dentro de um mesmo
  nível, o sinal reportado é o primeiro na ordem **dreno → falhas → quadros perdidos**.
- **Comparações com `>=`:** um valor exatamente no limiar promove o estado. Testes de fronteira
  (ex.: 11,99 → ATENÇÃO, 12,0 → CRÍTICO) prendem esse comportamento; trocar `>=` por `>` faz a suíte falhar.
- **Fórmulas** (calculadas só depois de normalizar a energia para mAh):
  - dreno absoluto (mAh/h) = energia (mAh) × 60 ÷ tela ligada (min)
  - dreno relativo (%/h) = dreno absoluto × 100 ÷ capacidade (mAh)
  - A multiplicação vem antes da divisão para evitar erro de arredondamento em ponto flutuante
    exatamente na fronteira (720 mAh em 60 min com 6000 mAh resulta em 12,0, não em 11,999…).
- **Por que dreno relativo e não absoluto:** 900 mAh/h esgota uma bateria de 3000 mAh em ~3,3 h,
  mas uma de 5000 mAh em ~5,6 h. O relativo mede o que importa ao usuário — quanto da bateria some
  por hora — e torna comparáveis aparelhos de capacidades diferentes (3900 a 6000 mAh nesta frota).

**Onde está:** `domain/ClassifyDevice.kt` (Kotlin puro, sem dependência de Android), com limiares
em `domain/Classification.kt` (`Thresholds`). Testes: `ClassifyDeviceTest` (32 casos).

---

## 2. Defeito 1 — unidade de energia inconsistente

**Problema.** `energyUnit` é `mAh` em 20 registros e `mWh` em 10 (DEV-0204, 0522, 0630, 0854,
1142, 1214, 1422, 1546, 1582, 1924). Comparar os dois diretamente mistura carga com energia.

**Decisão.** Converter na camada de dados, ao mapear o DTO para o modelo de domínio
(`data/DeviceMapper.kt`, função `energyInMah`):

> mAh = mWh ÷ tensão nominal (V) — de E = Q × V. Ex.: 3773 mWh ÷ 3,85 V = 980 mAh.

O domínio recebe apenas `energyConsumedMah`: a unidade está no nome do campo, e o domínio não sabe
que mWh existe. Unidade não declarada na fronteira entre módulos é o erro que destruiu o
Mars Climate Orbiter (1999).

**Casos de borda.**
- mWh com tensão ausente, zero ou negativa → energia `null` → INDETERMINADO.
- Unidade desconhecida (nem `mAh` nem `mWh`) → energia `null` → INDETERMINADO.
- A comparação da unidade é exata, sensível a maiúsculas: `MWh` (megawatt-hora) não é aceito como `mWh`.

**Alternativas descartadas.**
- Converter dentro da regra: obrigaria o domínio a conhecer unidades e tensão, misturando
  normalização de entrada com regra de negócio.
- Converter na UI: a classificação já teria sido feita com o valor errado.

**Consequência de não converter.** Todos os 10 registros em mWh passariam de 12 %/h:

| Aparelho | Convertido | Sem converter |
|---|---|---|
| DEV-0204 | 6,32 %/h — OK | 24,34 %/h — CRÍTICO |
| DEV-0630 | 6,31 %/h — OK | 24,49 %/h — CRÍTICO |
| DEV-1214 | 3,59 %/h — OK | 13,91 %/h — CRÍTICO |
| DEV-1546 | 7,49 %/h — ATENÇÃO | 28,84 %/h — CRÍTICO |
| … (10 no total) | | |

A contagem iria de **16/8/4/2** para **10/6/12/2**: oito Críticos falsos, dois terços dos Críticos
exibidos. Testes: `DeviceMapperTest` (conversão e bordas) e `FleetPipelineTest` (DEV-0204 = OK);
desativar a conversão derruba 9 testes.

---

## 3. Defeito 2 — divisor zero

**Problema.** DEV-0311 tem `batteryCapacityMah = 0`; DEV-0745 tem `screenOnMinutes = 0`. Os dois
são divisores do dreno: a conta produziria `Infinity` ou `NaN`.

**Decisão.** Classificar como **INDETERMINADO** (exibido como "Sem dados"), mantendo o aparelho
visível, com o motivo (`CAPACIDADE_INVALIDA` / `TELA_INVALIDA`). Vale também para valor negativo ou
campo ausente. A checagem acontece antes de qualquer conta: nenhum caminho do código divide por zero.

**Alternativas descartadas.**
- **Excluir da lista:** esconderia do operador um problema real de telemetria — um aparelho que
  reporta capacidade 0 precisa de atenção, não de sumir.
- **Falhar na ingestão:** derrubaria os 28 registros válidos por causa de 2.

**Consequência.** O operador vê 2 aparelhos "Sem dados" e sabe exatamente qual campo faltou.
Testes: capacidade 0, negativa e ausente; tela 0, negativa e ausente — todos sem exceção e sem `NaN`.

---

## 4. Outros registros incompletos

O arquivo atual não tem campos ausentes, mas o código trata a ausência de qualquer campo:

| Situação | Decisão | Motivo |
|---|---|---|
| `crashCount7d` ou `jankFramesPct` ausente | INDETERMINADO (`FALHAS_AUSENTES` / `QUADROS_PERDIDOS_AUSENTES`) | Assumir 0 seria inventar que o aparelho está saudável |
| Campo descritivo ausente (modelo, bateria, API…) | `null` no domínio, "—" na tela | Nenhum valor é inventado |
| `id` ausente ou em branco | Registro descartado | Sem id não há como navegar nem favoritar |
| Chave desconhecida no JSON | Ignorada | Tolerância a versões futuras do export |
| JSON malformado | Falha explícita na leitura, com mensagem de erro na tela | Não há dado confiável para exibir |

---

## 5. Ameaça à validade — limiar único para todos os tiers

**Ameaça.** Os limiares são os mesmos para aparelhos `low`, `mid` e `high`. Mas o dreno relativo
também reflete a eficiência do SoC, e não só um defeito do aparelho. Os aparelhos `low` desta frota
usam SoCs de entrada (Helio P35, G37, G85, Exynos 850, Snapdragon 685), menos eficientes.

**Evidência nos dados.**

| Tier | Classificáveis | Mediana do dreno | ≥ 7 %/h | Capacidade média | OK / ATENÇÃO / CRÍTICO |
|---|---|---|---|---|---|
| high | 10 | 5,57 %/h | 3 (30 %) | 4385 mAh | 7 / 2 / 1 |
| mid | 10 | 6,15 %/h | 3 (30 %) | 5192 mAh | 6 / 3 / 1 |
| low | 8 | **7,50 %/h** | **5 (62 %)** | 4782 mAh | 3 / 3 / 2 |

O aparelho `low` *típico* já está acima do limiar de Atenção. Isso não vem de bateria pequena — os
`low` têm, em média, baterias maiores que os `high` —, e sim de consumo absoluto maior. Metade dos
Críticos (DEV-0522, DEV-1985) é `low`, com só 8 aparelhos classificáveis nesse tier.

**Consequência.** Para um `low`, "Atenção" pode significar "comportamento normal do hardware", e
não "aparelho com defeito". O operador que priorizar a lista pelo estado vai gastar tempo com
aparelhos baratos funcionando como esperado, e o sinal perde valor justamente onde há mais alertas.

**Limites da evidência.** A amostra é pequena (8–10 por tier) e vem de um único export; a diferença
é indicativa, não conclusiva.

**Mitigação possível (não implementada, para manter a regra de referência).** Limiares por tier —
`ClassifyDevice` já recebe `Thresholds` no construtor, então bastaria escolher o `Thresholds` pelo
`tier` do aparelho — ou comparar cada aparelho com a mediana do próprio tier.

---

## 6. Resultado obtido

Com a regra da seção 1, sobre o `frota.json` real (verificado por `FleetPipelineTest`):

| OK | ATENÇÃO | CRÍTICO | INDETERMINADO |
|---|---|---|---|
| 16 | 8 | 4 | 2 |

**Críticos** (todos pelo sinal de dreno):

| Aparelho | Tier | Unidade original | Dreno relativo |
|---|---|---|---|
| DEV-1839 | mid | mAh | 19,44 %/h |
| DEV-1985 | low | mAh | 13,50 %/h |
| DEV-0522 | low | mWh | 13,40 %/h |
| DEV-1924 | high | mWh | 13,38 %/h |

**Indeterminados:** DEV-0311 (capacidade = 0) e DEV-0745 (tela ligada = 0).

**Atenção:** sete por dreno (DEV-1514, 1546, 1559, 1582, 1625, 1711, 1767) e um por falhas
(DEV-0137: 2 falhas em 7 dias, com dreno de 6,23 %/h, abaixo do limiar).
