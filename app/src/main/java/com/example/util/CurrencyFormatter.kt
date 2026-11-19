package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class InputCurrency { RSD, EUR }

object CurrencyFormatter {

    private val symbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ' '
        decimalSeparator = ','
    }

    private val intFormat = DecimalFormat("#,##0", symbols)
    private val decFormat = DecimalFormat("#,##0.##", symbols)
    private val eurFormat = DecimalFormat("#,##0.##", symbols)

    fun round2(value: Double): Double = (value * 100.0).roundToInt() / 100.0

    fun roundRsd(value: Double): Double = round2(value)

    fun formatRsd(amount: Double, showSign: Boolean = false, withUnit: Boolean = true): String {
        val clean = round2(amount)
        val absVal = abs(clean)
        val formatted = if (abs(absVal - absVal.roundToInt()) < 0.005) {
            intFormat.format(absVal.roundToInt())
        } else {
            decFormat.format(absVal)
        }
        val sign = when {
            clean < -0.004 -> "−"
            showSign && clean > 0.004 -> "+"
            else -> ""
        }
        return if (withUnit) "$sign$formatted RSD" else "$sign$formatted"
    }

    fun formatCompactRsd(amount: Double): String {
        val absVal = abs(amount)
        return when {
            absVal >= 1_000_000 -> "${String.format(Locale.US, "%.1f", absVal / 1_000_000.0).replace('.', ',')}M"
            absVal >= 1_000 -> "${String.format(Locale.US, "%.1f", absVal / 1_000.0).replace(".0", "").replace('.', ',')}k"
            else -> absVal.roundToInt().toString()
        }
    }

    fun toEur(rsd: Double, eurRate: Double): Double {
        val safeRate = if (eurRate > 0.0) eurRate else 117.0
        return round2(rsd / safeRate)
    }

    fun toRsd(eur: Double, eurRate: Double): Double {
        val safeRate = if (eurRate > 0.0) eurRate else 117.0
        return round2(eur * safeRate)
    }

    fun formatEurApprox(rsd: Double, eurRate: Double): String {
        val eur = toEur(rsd, eurRate)
        val absEur = abs(eur)
        val sign = if (eur < -0.004) "−" else ""
        return "≈ $sign${eurFormat.format(absEur)} €"
    }

    fun formatEurValue(eur: Double): String = "${eurFormat.format(eur)} €"

    /**
     * Parses numeric user input supporting both '.' and ',' decimal separators.
     */
    fun parseAmount(input: String): Double? {
        val normalized = input.trim()
            .replace(" ", "")
            .replace(',', '.')
        if (normalized.isEmpty()) return null
        val parsed = normalized.toDoubleOrNull() ?: return null
        return if (parsed >= 0.0 && parsed.isFinite()) round2(parsed) else null
    }

    fun sanitizeDecimalInput(raw: String): String {
        val filtered = buildString {
            var hasSeparator = false
            var decimals = 0
            for (ch in raw) {
                if (ch.isDigit()) {
                    if (!hasSeparator || decimals < 2) {
                        append(ch)
                        if (hasSeparator) decimals++
                    }
                } else if ((ch == '.' || ch == ',') && !hasSeparator) {
                    append(ch)
                    hasSeparator = true
                }
            }
        }
        return filtered
    }

    fun appendEurNote(note: String, eurAmount: Double): String {
        val tag = "(${eurFormat.format(eurAmount)} €)"
        val trimmed = note.trim()
        return when {
            trimmed.isEmpty() -> tag
            trimmed.contains("€)") -> trimmed
            else -> "$trimmed $tag"
        }
    }
}
