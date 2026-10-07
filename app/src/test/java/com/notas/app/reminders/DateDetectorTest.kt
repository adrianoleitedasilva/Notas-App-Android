package com.notas.app.reminders

import com.notas.app.data.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class DateDetectorTest {

    private val today = LocalDate.of(2026, 10, 7)

    private fun dates(text: String) = DateDetector.detect(text, today)

    @Test
    fun `dia e mes sem ano usa este ano`() {
        assertEquals(LocalDate.of(2026, 10, 15), dates("dentista 15/10").single().date)
    }

    @Test
    fun `ano com 4 e 2 digitos`() {
        assertEquals(LocalDate.of(2027, 3, 2), dates("prova 02/03/2027").single().date)
        assertEquals(LocalDate.of(2027, 3, 2), dates("prova 02/03/27").single().date)
    }

    @Test
    fun `data escrita por extenso`() {
        assertEquals(LocalDate.of(2026, 11, 20), dates("viagem 20 de novembro").single().date)
        assertEquals(LocalDate.of(2027, 3, 1), dates("1 de março de 2027").single().date)
        assertEquals(LocalDate.of(2026, 12, 5), dates("5 de dez").single().date)
    }

    @Test
    fun `data sem ano que ja passou vai para o ano seguinte se estiver perto`() {
        assertEquals(LocalDate.of(2027, 1, 5), dates("volta às aulas 05/01").single().date)
    }

    @Test
    fun `data sem ano que ja passou ha pouco e ignorada`() {
        assertTrue(dates("reunião foi 01/10").isEmpty())
    }

    @Test
    fun `fracoes nao sao datas`() {
        assertTrue(dates("1/2 xícara de açúcar e 3/4 de leite").isEmpty())
    }

    @Test
    fun `datas invalidas sao ignoradas`() {
        assertTrue(dates("31/02 e 15/13 e 10 de nada").isEmpty())
    }

    @Test
    fun `hora na mesma linha`() {
        assertEquals(LocalTime.of(14, 30), dates("médico 20/10 às 14:30").single().time)
        assertEquals(LocalTime.of(9, 0), dates("médico 20/10 9h").single().time)
        assertEquals(LocalTime.of(18, 15), dates("18h15 jantar 20/10").single().time)
    }

    @Test
    fun `hora em outra linha nao conta`() {
        assertNull(dates("médico 20/10\n14:30").single().time)
    }

    @Test
    fun `varias datas no texto`() {
        val found = dates("ida 20/10\nvolta 25/10")
        assertEquals(listOf(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 25)), found.map { it.date })
        assertEquals("volta 25/10", found[1].line)
    }

    @Test
    fun `avisos na vespera e no dia`() {
        val note = Note(1, "Dentista", "consulta 15/10 às 14:00", 0)
        val reminders = ReminderPlanner.plan(note, LocalDateTime.of(2026, 10, 7, 12, 0))
        assertEquals(
            listOf(LocalDateTime.of(2026, 10, 14, 9, 0), LocalDateTime.of(2026, 10, 15, 13, 0)),
            reminders.map { it.at },
        )
    }

    @Test
    fun `sem hora avisa as 8h do dia`() {
        val note = Note(1, "", "aniversário da Ana 15/10", 0)
        val sameDay = ReminderPlanner.plan(note, LocalDateTime.of(2026, 10, 7, 12, 0)).last()
        assertEquals(LocalDateTime.of(2026, 10, 15, 8, 0), sameDay.at)
        assertEquals("aniversário da Ana 15/10", sameDay.noteTitle)
    }

    @Test
    fun `avisos que ja passaram nao sao agendados`() {
        val note = Note(1, "", "hoje 07/10 às 20:00", 0)
        val reminders = ReminderPlanner.plan(note, LocalDateTime.of(2026, 10, 7, 12, 0))
        assertEquals(listOf(ReminderKind.SAME_DAY), reminders.map { it.kind })
    }
}
