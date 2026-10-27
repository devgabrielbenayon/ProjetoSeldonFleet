# Trilha C — Eficiência energética da atualização periódica

**Resumo.** O app agenda uma releitura periódica da frota com WorkManager: a cada **12 h** em
aparelhos comuns e a cada **24 h, só carregando**, em aparelhos fracos, sempre com bateria acima do
nível baixo. A estimativa é de 0,1 mAh por execução. Rodar a cada 15 min custaria ~0,26 % da bateria
por dia (até ~1,2 % no pior caso em um aparelho `low`); a política adotada custa menos de 0,01 %.
O ponto mais importante, porém, é outro: **neste app a releitura é desperdício puro**, porque o
arquivo lido nunca muda (seção 2).

---

## 1. O que foi implementado

| Peça | Arquivo | Papel |
|---|---|---|
| `FleetRefreshWorker` | `work/FleetRefreshWorker.kt` | `CoroutineWorker` que relê a frota pelo `DeviceRepository` (a mesma interface usada pela UI) e grava o resultado em `FleetCache` (um `StateFlow`) |
| `FleetRefreshScheduler` | `work/FleetRefreshScheduler.kt` | Detecta a classe do aparelho e agenda com `PeriodicWorkRequest` + `enqueueUniquePeriodicWork(KEEP)` |
| Política | `work/RefreshPolicy.kt` | Funções puras, testadas na JVM (`RefreshPolicyTest`, 9 casos) |

**Política adotada**

| Classe do aparelho | Como é detectada | Intervalo | Restrições |
|---|---|---|---|
| Fraco (`LOW_END`) | `isLowRamDevice()` ou RAM total < 5 GB | 24 h | bateria não baixa **e carregando** |
| Padrão (`STANDARD`) | demais | 12 h | bateria não baixa |

- **Por que RAM:** na frota, todo aparelho `low` tem até 4 GB de RAM e todo `mid`/`high` tem 6 GB ou
  mais. O Android reporta um pouco menos que o valor nominal (4 GB aparecem como ~3,7 GB), por isso
  o corte fica em 5 GB.
- **`KEEP`:** abrir o app de novo não recria nem reinicia o agendamento.
- **Falha não gera nova tentativa:** como o arquivo não muda, tentar de novo só gastaria bateria; o
  worker devolve `failure()` em vez de `retry()`.
- **A UI não depende do worker:** a tela continua lendo o arquivo ao abrir; o núcleo não mudou.

---

## 2. Observação honesta: neste app, reler é desperdício

O `frota.json` está **dentro do APK** e é imutável: cada releitura devolve exatamente os mesmos 30
aparelhos. Qualquer energia gasta pelo worker é desperdício, por menor que seja — a política ótima
para *este* app é **não ter atualização periódica** e ler o arquivo ao abrir, como a UI já faz.

O worker faz sentido num cenário real, em que os dados viriam de um servidor. Mesmo assim, **a
frequência certa é a da mudança dos dados, não a do relógio**: se a telemetria é exportada uma vez
por dia, atualizar a cada hora gasta 23 execuções por dia para nada. As alternativas melhores são
atualizar ao abrir o app (com cache e `ETag` para não baixar o que não mudou) ou receber uma
notificação *push* do servidor quando houver dados novos.

---

## 3. Doze e App Standby: o horário não é garantido

- **Intervalo mínimo:** o WorkManager não aceita intervalo periódico menor que 15 min.
- **Doze:** com a tela desligada e o aparelho parado, o Android suspende alarmes e jobs e os libera
  em *janelas de manutenção*, que ficam mais espaçadas quanto mais tempo o aparelho fica parado. Um
  trabalho de 12 h pode rodar com horas de atraso.
- **App Standby Buckets (Android 9+):** apps pouco usados recebem cotas menores de execução em
  segundo plano; um app de consulta como este tende a cair nos buckets mais restritivos.
- **Sem flex definido,** a execução pode ocorrer em qualquer ponto de cada período.

**Consequência:** nada no app pode depender do horário exato da atualização. Para dados de frota,
isso é aceitável. E há um efeito positivo: ao agrupar trabalhos na mesma janela, o sistema divide o
custo de acordar o aparelho entre vários apps, e o custo marginal real fica abaixo da estimativa
da seção 4.

---

## 4. Custo estimado de uma execução

**Isto é uma estimativa, não uma medição.** O emulador não tem bateria física. A medição correta
seria em aparelho real, com o Power Profiler do Android Studio, Perfetto ou `batterystats`
(seção 7).

Uma execução tem três fases:

| Fase | O que acontece | Otimista | Central | Pessimista |
|---|---|---|---|---|
| A. Acordar e iniciar | sair da suspensão, despacho do job, processo criado do zero (após horas, o processo já foi encerrado), `Application.onCreate`, banco interno do WorkManager | 0,4 s a 200 mA | 1,0 s a 250 mA | 2,0 s a 350 mA |
| B. Trabalho útil | ler ~13 KB do APK e decodificar 30 registros | 0,02 s a 200 mA | 0,05 s a 250 mA | 0,05 s a 350 mA |
| C. Cauda | sistema acordado e ocioso antes de voltar a suspender | 1 s a 40 mA | 2 s a 60 mA | 4 s a 100 mA |
| **Total** | | **0,034 mAh** | **0,106 mAh** | **0,310 mAh** |

(energia em mAh = soma de tempo × corrente ÷ 3600)

**De onde vêm as correntes.** São premissas de ordem de grandeza, coerentes com dois pontos de apoio:
- O modelo de energia do Android (AOSP, *Power profiles*) atribui a cada componente uma corrente
  própria — CPU suspensa, ociosa, ativa por cluster e frequência — tipicamente de dezenas a
  centenas de mA, conforme o fabricante.
- A própria frota mede **235 a 375 mA** de consumo médio com a tela ligada (mediana do dreno
  absoluto por tier). Um surto de CPU sem tela fica na mesma ordem de grandeza, o que torna os
  250 mA da fase A plausíveis.

**O trabalho útil é menos de 5 % do custo.** Quase tudo é acordar o aparelho e iniciar o processo.
Por isso o que importa é **quantas vezes** o worker roda, e não o quanto o parse é eficiente.

---

## 5. Custo por dia em % da bateria

Usando as capacidades extremas da frota: **3900 mAh** (a menor) e **6000 mAh** (a maior).

| Frequência | Execuções/dia | Central — 3900 mAh | Central — 6000 mAh | Pessimista — 3900 mAh | Pessimista — 6000 mAh |
|---|---|---|---|---|---|
| 15 min | 96 | 0,26 % | 0,17 % | 0,76 % | 0,50 % |
| 1 h | 24 | 0,065 % | 0,043 % | 0,19 % | 0,12 % |
| 6 h | 4 | 0,011 % | 0,007 % | 0,032 % | 0,021 % |
| 12 h | 2 | 0,005 % | 0,004 % | 0,016 % | 0,010 % |
| 24 h | 1 | 0,003 % | 0,002 % | 0,008 % | 0,005 % |

**Leitura.**
- Passar de 15 min para 12 h corta o custo **48 vezes** (96 → 2 execuções por dia).
- No cenário central, a cada 15 min o worker gastaria 10,2 mAh/dia — o equivalente a ~2,6 min de
  tela ligada num aparelho `high` da frota. Pouco para um aparelho, mas multiplicado pela frota
  inteira e somado aos outros apps que fazem o mesmo, é bateria jogada fora todos os dias.
- A partir de 6 h, o custo some no ruído (< 0,03 % mesmo no cenário pessimista).

---

## 6. A pergunta do tier: o mesmo trabalho não custa o mesmo

O custo em tempo e corrente da seção 4 vale para um SoC eficiente. Os aparelhos `low` da frota usam
SoCs de entrada (Helio P35, G37, G85, Exynos 850, Snapdragon 685), que gastam mais energia para o
mesmo trabalho. Os dados da própria frota permitem estimar o fator:

| Tier | Aparelhos | Dreno absoluto mediano | Fator de custo (vs. `high`) | Bateria média | Menor bateria |
|---|---|---|---|---|---|
| high | 10 | 235 mAh/h | 1,00 | 4385 mAh | 3900 mAh |
| mid | 10 | 310 mAh/h | 1,32 | 5192 mAh | 4410 mAh |
| low | 10 | 375 mAh/h | **1,60** | 4782 mAh | 4020 mAh |

(O fator usa o dreno absoluto com a tela ligada como aproximação da eficiência do SoC. É uma
aproximação: o uso de cada aparelho também influencia esse número.)

**Custo por dia no cenário central, aplicando o fator:**

| Tier | A cada 15 min (bateria média → menor) | A cada 12 h | A cada 24 h |
|---|---|---|---|
| high | 0,23 % → 0,26 % | 0,005 % | 0,002 % |
| mid | 0,26 % → 0,31 % | 0,005 % | 0,003 % |
| low | **0,34 % → 0,41 %** | 0,007 % | 0,004 % |

No pior caso (cenário pessimista, a cada 15 min, aparelho `low` com a menor bateria), o custo chega a
**47,6 mAh/dia ≈ 1,2 % da bateria** — cerca de 7,6 min de tela ligada perdidos por dia, no aparelho
que tem menos folga.

**Por que a recomendação muda por tier.**
- O aparelho `low` paga ~60 % a mais por execução, e é justamente o usuário que mais sente a
  bateria acabar.
- Exigir **carregamento** nos aparelhos fracos zera o custo para a bateria: a energia vem da
  tomada. O preço é atualizar menos (só quando o aparelho é carregado), o que é aceitável para dados
  que mudam devagar.
- Nos aparelhos padrão, 12 h com bateria não baixa já mantém o custo abaixo de 0,01 % ao dia, sem
  depender de o usuário carregar o aparelho.

| Tier | Recomendação | Implementado como |
|---|---|---|
| high / mid | 12 h, bateria não baixa | `STANDARD` |
| low | 24 h, bateria não baixa **e carregando** | `LOW_END` |

---

## 7. Limitações e como validar

- **Correntes estimadas.** Os números da seção 4 são premissas. Para validar: instalar em um aparelho
  físico de cada tier, forçar o job com
  `adb shell cmd jobscheduler run -f com.gabbe.seldonfleet <id>` e medir com o Power Profiler do
  Android Studio ou com Perfetto (rastros de energia); `adb shell dumpsys batterystats` dá o consumo
  atribuído ao app ao longo de um dia.
- **Fator de tier aproximado.** Ele vem do uso com a tela ligada de 10 aparelhos por tier, não de
  um benchmark de CPU.
- **Detecção por RAM.** Separa bem esta frota, mas é uma heurística: um aparelho barato com muita RAM
  seria tratado como padrão.
- **Fora do escopo:** consumo de rede (o app não usa rede); num cenário com servidor, acordar o rádio
  custaria mais que todo o resto desta estimativa.
