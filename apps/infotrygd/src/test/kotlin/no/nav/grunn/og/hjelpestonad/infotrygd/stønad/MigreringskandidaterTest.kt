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

    /** Deler kandidatene i partier som repositoryet: synkende stønadId, under førStønadId. */
    private fun partierAv(
        kandidater: List<Migreringskandidat>,
        partistørrelse: Int,
        forespurtePartier: MutableList<Long?> = mutableListOf(),
    ): (Long?) -> List<Migreringskandidat> =
        { førStønadId ->
            forespurtePartier.add(førStønadId)
            kandidater
                .filter { førStønadId == null || it.stønadId < førStønadId }
                .sortedByDescending { it.stønadId }
                .take(partistørrelse)
        }

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
                hjemmelbokstav = "F",
                antall = 10,
                hentParti = partierAv(kandidater, partistørrelse = 10),
                hentEvalueringer = { vedtakIder -> evalueringer.filterKeys { it in vedtakIder } },
            )

        assertEquals(setOf("person4", "person3"), personer)
    }

    @Test
    fun `motstridende hjemmelbokstav gir ikke treff`() {
        val personer =
            finnPersonerMedHjemmelbokstav(
                hjemmelbokstav = "F",
                antall = 10,
                hentParti = partierAv(listOf(kandidat(1)), partistørrelse = 10),
                hentEvalueringer = { mapOf(10L to evaluering(null, motstridendeFelt = setOf(Ev10Posisjon.HJEMMELBOKSTAV))) },
            )

        assertEquals(emptySet(), personer)
    }

    @Test
    fun `henter nye partier til antall er nådd, og ikke flere`() {
        val kandidater = (1L..6L).map { kandidat(it) }
        val forespurtePartier = mutableListOf<Long?>()

        val personer =
            finnPersonerMedHjemmelbokstav(
                hjemmelbokstav = "F",
                antall = 3,
                hentParti = partierAv(kandidater, partistørrelse = 2, forespurtePartier),
                // Bare kandidatene med partall stønadId har F.
                hentEvalueringer = { vedtakIder -> vedtakIder.associateWith { evaluering(if (it % 20 == 0L) "F" else "B") } },
            )

        assertEquals(setOf("person6", "person4", "person2"), personer)
        assertEquals(listOf(null, 5L, 3L), forespurtePartier)
    }

    @Test
    fun `stopper når et parti er tomt, med færre personer enn antall`() {
        val forespurtePartier = mutableListOf<Long?>()

        val personer =
            finnPersonerMedHjemmelbokstav(
                hjemmelbokstav = "F",
                antall = 10,
                hentParti = partierAv(listOf(kandidat(2), kandidat(1)), partistørrelse = 1, forespurtePartier),
                hentEvalueringer = { vedtakIder -> vedtakIder.associateWith { evaluering("F") } },
            )

        assertEquals(setOf("person2", "person1"), personer)
        assertEquals(listOf(null, 2L, 1L), forespurtePartier)
    }

    @Test
    fun `returnerer ikke flere enn antall selv om partiet har flere treff`() {
        val personer =
            finnPersonerMedHjemmelbokstav(
                hjemmelbokstav = "F",
                antall = 2,
                hentParti = partierAv((1L..5L).map { kandidat(it) }, partistørrelse = 10),
                hentEvalueringer = { vedtakIder -> vedtakIder.associateWith { evaluering("F") } },
            )

        assertEquals(setOf("person5", "person4"), personer)
    }

    @Test
    fun `samme person med to stønader telles én gang`() {
        val personer =
            finnPersonerMedHjemmelbokstav(
                hjemmelbokstav = "F",
                antall = 2,
                hentParti = partierAv(listOf(kandidat(3, "a"), kandidat(2, "a"), kandidat(1, "b")), partistørrelse = 10),
                hentEvalueringer = { vedtakIder -> vedtakIder.associateWith { evaluering("F") } },
            )

        assertEquals(setOf("a", "b"), personer)
    }

    @Test
    fun `feiler i stedet for å gå evig når partiet ikke flytter seg`() {
        assertThrows<IllegalStateException> {
            finnPersonerMedHjemmelbokstav(
                hjemmelbokstav = "F",
                antall = 10,
                hentParti = { listOf(kandidat(1)) },
                hentEvalueringer = { emptyMap() },
            )
        }
    }
}
