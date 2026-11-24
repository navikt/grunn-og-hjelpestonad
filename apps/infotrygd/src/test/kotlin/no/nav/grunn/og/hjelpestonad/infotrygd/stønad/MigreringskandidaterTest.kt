package no.nav.grunn.og.hjelpestonad.infotrygd.stønad

import no.nav.grunn.og.hjelpestonad.infotrygd.evaluering.Ev10Posisjon
import no.nav.grunn.og.hjelpestonad.infotrygd.evaluering.InfotrygdEvaluering
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth
import kotlin.test.assertEquals

class MigreringskandidaterTest {
    private fun kandidat(
        stønadId: Long,
        personident: String = "person$stønadId",
    ) = Migreringskandidat(personident = personident, stønadId = stønadId, vedtakId = stønadId * 10)

    private fun evaluering(
        hjemmelbokstav: String?,
        motstridendeFelt: Set<Ev10Posisjon> = emptySet(),
    ) = InfotrygdEvaluering(
        månedNotertIInfotrygd = YearMonth.of(2025, 10),
        diagnose1 = null,
        diagnose2 = null,
        sats = null,
        hjemmelbokstav = hjemmelbokstav,
        virkningstidspunkt = null,
        motstridendeFelt = motstridendeFelt,
    )

    @Test
    fun `tar med personer der evalueringen har hjemmelbokstaven, sammenlignet på trimmet tekst`() {
        val kandidater = listOf(kandidat(4), kandidat(3), kandidat(2), kandidat(1))
        val evalueringer =
            mapOf(
                40L to evaluering("F"),
                30L to evaluering(" F  "),
                20L to evaluering("B"),
                // Kandidat 1 har ingen evaluering.
            )

        val personer =
            finnPersonerMedHjemmelbokstav(
                kandidater = kandidater,
                hjemmelbokstav = "F",
                antall = 10,
                bitstørrelse = 10,
                hentEvalueringer = { vedtakIder -> evalueringer.filterKeys { it in vedtakIder } },
            )

        assertEquals(setOf("person4", "person3"), personer)
    }

    @Test
    fun `motstridende hjemmelbokstav gir ikke treff`() {
        val personer =
            finnPersonerMedHjemmelbokstav(
                kandidater = listOf(kandidat(1)),
                hjemmelbokstav = "F",
                antall = 10,
                bitstørrelse = 10,
                hentEvalueringer = { mapOf(10L to evaluering(null, motstridendeFelt = setOf(Ev10Posisjon.HJEMMELBOKSTAV))) },
            )

        assertEquals(emptySet(), personer)
    }

    @Test
    fun `henter evalueringer bit for bit til antall er nådd, og ikke flere`() {
        val kandidater = (6L downTo 1L).map { kandidat(it) }
        val forespurteBiter = mutableListOf<Set<Long>>()

        val personer =
            finnPersonerMedHjemmelbokstav(
                kandidater = kandidater,
                hjemmelbokstav = "F",
                antall = 2,
                bitstørrelse = 2,
                hentEvalueringer = { vedtakIder ->
                    forespurteBiter.add(vedtakIder)
                    // Bare kandidatene med partall stønadId har F.
                    vedtakIder.associateWith { evaluering(if (it % 20 == 0L) "F" else "B") }
                },
            )

        assertEquals(setOf("person6", "person4"), personer)
        assertEquals(listOf(setOf(60L, 50L), setOf(40L, 30L)), forespurteBiter)
    }

    @Test
    fun `går gjennom alle kandidatene når det er færre treff enn antall`() {
        val forespurteBiter = mutableListOf<Set<Long>>()

        val personer =
            finnPersonerMedHjemmelbokstav(
                kandidater = listOf(kandidat(3), kandidat(2), kandidat(1)),
                hjemmelbokstav = "F",
                antall = 10,
                bitstørrelse = 2,
                hentEvalueringer = { vedtakIder ->
                    forespurteBiter.add(vedtakIder)
                    vedtakIder.associateWith { evaluering("F") }
                },
            )

        assertEquals(setOf("person3", "person2", "person1"), personer)
        assertEquals(listOf(setOf(30L, 20L), setOf(10L)), forespurteBiter)
    }

    @Test
    fun `henter ingen evalueringer uten kandidater`() {
        val personer =
            finnPersonerMedHjemmelbokstav(
                kandidater = emptyList(),
                hjemmelbokstav = "F",
                antall = 10,
                bitstørrelse = 2,
                hentEvalueringer = { error("Skal ikke hente evalueringer uten kandidater") },
            )

        assertEquals(emptySet(), personer)
    }

    @Test
    fun `returnerer ikke flere enn antall selv om biten har flere treff`() {
        val personer =
            finnPersonerMedHjemmelbokstav(
                kandidater = (5L downTo 1L).map { kandidat(it) },
                hjemmelbokstav = "F",
                antall = 2,
                bitstørrelse = 10,
                hentEvalueringer = { vedtakIder -> vedtakIder.associateWith { evaluering("F") } },
            )

        assertEquals(setOf("person5", "person4"), personer)
    }

    @Test
    fun `samme person med to stønader telles én gang`() {
        val personer =
            finnPersonerMedHjemmelbokstav(
                kandidater = listOf(kandidat(3, "a"), kandidat(2, "a"), kandidat(1, "b")),
                hjemmelbokstav = "F",
                antall = 2,
                bitstørrelse = 10,
                hentEvalueringer = { vedtakIder -> vedtakIder.associateWith { evaluering("F") } },
            )

        assertEquals(setOf("a", "b"), personer)
    }

    @Test
    fun `bitstørrelse må være større enn 0`() {
        assertThrows<IllegalArgumentException> {
            finnPersonerMedHjemmelbokstav(
                kandidater = listOf(kandidat(1)),
                hjemmelbokstav = "F",
                antall = 10,
                bitstørrelse = 0,
                hentEvalueringer = { emptyMap() },
            )
        }
    }
}
