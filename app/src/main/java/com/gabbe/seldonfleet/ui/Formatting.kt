package com.gabbe.seldonfleet.ui

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

// Formatação pt-BR (vírgula decimal). Valor ausente vira "—", nunca um número inventado.

private val ptBr: Locale = Locale.forLanguageTag("pt-BR")

const val MISSING = "—"

/** Número com casas decimais fixas: 13.4 → "13,4". */
fun Double?.formatPt(decimals: Int = 1): String =
    this?.let { String.format(ptBr, "%.${decimals}f", it) } ?: MISSING

/** Limiar sem ",0" desnecessário: 12.0 → "12", 7.5 → "7,5". */
fun Double?.formatLimit(): String =
    this?.let { DecimalFormat("0.#", DecimalFormatSymbols(ptBr)).format(it) } ?: MISSING

fun Int?.formatPt(): String = this?.toString() ?: MISSING

private val lastSeenFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm 'UTC'").withZone(ZoneOffset.UTC)

/** "2026-10-20T02:58:44Z" → "20/10/2026 02:58 UTC"; formato inesperado é exibido como veio. */
fun formatLastSeen(iso: String?): String {
    if (iso == null) return MISSING
    return try {
        lastSeenFormatter.format(Instant.parse(iso))
    } catch (e: DateTimeParseException) {
        iso
    }
}
