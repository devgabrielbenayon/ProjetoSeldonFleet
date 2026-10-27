package com.gabbe.seldonfleet.ui

import com.gabbe.seldonfleet.domain.Classification
import com.gabbe.seldonfleet.domain.Device
import com.gabbe.seldonfleet.domain.Signal

/**
 * Frase em português que explica a classificação, citando o sinal e o valor.
 * O domínio entrega dados estruturados (Reason); o texto é responsabilidade da UI.
 */
fun reasonText(classification: Classification, device: Device): String {
    val reason = classification.reason
        ?: return "Nenhum sinal atingiu os limiares de atenção. " +
            "Dreno relativo de ${classification.drainPctPerHour.formatPt()} %/h." +
            conversionNote(classification, device)

    val observed = reason.observed
    val threshold = reason.threshold
    val comparison = if (observed != null && threshold != null && observed > threshold) {
        "acima do"
    } else {
        "igual ao"
    }

    val sentence = when (reason.signal) {
        Signal.DRENO ->
            "Dreno relativo de ${observed.formatPt()} %/h, $comparison limiar de ${threshold.formatLimit()} %/h."
        Signal.FALHAS ->
            "${crashes(observed)} nos últimos 7 dias, $comparison limiar de ${crashes(threshold)}."
        Signal.QUADROS_PERDIDOS ->
            "Quadros perdidos em ${observed.formatPt()} %, $comparison limiar de ${threshold.formatLimit()} %."
        Signal.CAPACIDADE_INVALIDA ->
            if (observed == null) "Capacidade da bateria ausente no arquivo; não é possível calcular o dreno."
            else "Capacidade da bateria registrada como ${observed.toInt()} mAh; não é possível calcular o dreno."
        Signal.TELA_INVALIDA ->
            if (observed == null) "Tempo de tela ligada ausente no arquivo; não é possível calcular o dreno."
            else "Tempo de tela ligada registrado como ${observed.toInt()} min; não é possível calcular o dreno."
        Signal.ENERGIA_AUSENTE -> missingEnergyText(device.energyReportedUnit)
        Signal.FALHAS_AUSENTES -> "Contagem de falhas ausente no arquivo."
        Signal.QUADROS_PERDIDOS_AUSENTES -> "Percentual de quadros perdidos ausente no arquivo."
    }
    return sentence + conversionNote(classification, device)
}

private fun crashes(value: Double?): String = when (val n = value?.toInt()) {
    null -> MISSING
    1 -> "1 falha"
    else -> "$n falhas"
}

private fun missingEnergyText(unit: String?): String = when (unit) {
    null -> "Consumo de energia ausente no arquivo; não é possível calcular o dreno."
    "mWh" -> "Consumo reportado em mWh sem tensão nominal válida; não é possível converter para mAh."
    else -> "Consumo reportado em unidade desconhecida (\"$unit\"); não é possível converter para mAh."
}

// Só faz sentido quando o dreno foi de fato calculado a partir de um valor convertido.
private fun conversionNote(classification: Classification, device: Device): String =
    if (device.energyReportedUnit == "mWh" && classification.drainPctPerHour != null) {
        " Consumo normalizado a partir de mWh antes da comparação."
    } else {
        ""
    }
