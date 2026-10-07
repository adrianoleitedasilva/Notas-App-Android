package com.notas.app.reminders

import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/** Uma data encontrada no texto de uma nota, com a hora (se houver) e a linha onde estava. */
data class DetectedDate(val date: LocalDate, val time: LocalTime?, val line: String)

/**
 * Encontra datas escritas em português no texto de uma nota:
 * `15/10`, `15/10/2026`, `15/10/26`, `15 de outubro`, `15 de outubro de 2026`.
 * Uma hora na mesma linha (`14:30`, `14h`, `14h30`) é associada à data.
 */
object DateDetector {

    private val numeric = Regex("""(?<![\d/])(\d{1,2})/(\d{1,2})(?:/(\d{4}|\d{2}))?(?![\d/])""")
    private val written = Regex("""(?<!\d)(\d{1,2})\s+de\s+(\p{L}+)(?:\s+de\s+(\d{4}))?""", RegexOption.IGNORE_CASE)
    private val time = Regex("""(?<![\d/:])([01]?\d|2[0-3])(?:h([0-5]\d)?|:([0-5]\d))(?![\d\p{L}])""", RegexOption.IGNORE_CASE)

    private val months = mapOf(
        "janeiro" to 1, "jan" to 1,
        "fevereiro" to 2, "fev" to 2,
        "março" to 3, "marco" to 3, "mar" to 3,
        "abril" to 4, "abr" to 4,
        "maio" to 5, "mai" to 5,
        "junho" to 6, "jun" to 6,
        "julho" to 7, "jul" to 7,
        "agosto" to 8, "ago" to 8,
        "setembro" to 9, "set" to 9,
        "outubro" to 10, "out" to 10,
        "novembro" to 11, "nov" to 11,
        "dezembro" to 12, "dez" to 12,
    )

    /** Datas sem ano que já passaram contam para o ano seguinte só se estiverem a até 6 meses. */
    private const val MAX_DAYS_AHEAD_WITHOUT_YEAR = 183L

    fun detect(text: String, today: LocalDate): List<DetectedDate> =
        text.lineSequence()
            .flatMap { line -> detectInLine(line, today) }
            .distinctBy { it.date to it.time }
            .toList()

    private fun detectInLine(line: String, today: LocalDate): List<DetectedDate> {
        val dates = mutableListOf<Pair<IntRange, LocalDate>>()

        numeric.findAll(line).forEach { m ->
            val (d, mo, y) = m.destructured
            // "1/2", "3/4" sem ano parecem frações, não datas
            if (y.isEmpty() && d.length == 1 && mo.length == 1) return@forEach
            resolve(d.toInt(), mo.toInt(), y, today)?.let { dates += m.range to it }
        }
        written.findAll(line).forEach { m ->
            val (d, monthName, y) = m.destructured
            val month = months[monthName.lowercase()] ?: return@forEach
            resolve(d.toInt(), month, y, today)?.let { dates += m.range to it }
        }
        if (dates.isEmpty()) return emptyList()

        // A hora não pode ser um pedaço de uma data já encontrada
        val hour = time.findAll(line)
            .firstOrNull { t -> dates.none { (range, _) -> t.range.first in range } }
            ?.let { t ->
                val (h, minH, minColon) = t.destructured
                LocalTime.of(h.toInt(), (minH.ifEmpty { minColon }.ifEmpty { "0" }).toInt())
            }

        val text = line.trim()
        return dates.sortedBy { it.first.first }.map { (_, date) -> DetectedDate(date, hour, text) }
    }

    private fun resolve(day: Int, month: Int, year: String, today: LocalDate): LocalDate? {
        val explicitYear = when (year.length) {
            4 -> year.toInt()
            2 -> 2000 + year.toInt()
            else -> null
        }
        return try {
            if (explicitYear != null) {
                LocalDate.of(explicitYear, month, day)
            } else {
                val thisYear = LocalDate.of(today.year, month, day)
                if (!thisYear.isBefore(today)) {
                    thisYear
                } else {
                    val nextYear = thisYear.plusYears(1)
                    nextYear.takeIf { ChronoUnit.DAYS.between(today, it) <= MAX_DAYS_AHEAD_WITHOUT_YEAR }
                }
            }
        } catch (_: DateTimeException) {
            null // 31/02, 15/13 etc.
        }
    }
}
