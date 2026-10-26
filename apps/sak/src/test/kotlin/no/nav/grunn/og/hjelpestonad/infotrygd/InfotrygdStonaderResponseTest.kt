package no.nav.grunn.og.hjelpestonad.infotrygd

import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.InfotrygdEvaluering
import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.InfotrygdStonadResponse
import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.InfotrygdVedtakResponse
import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.StonaderResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class InfotrygdStonaderResponseTest {
    @Test
    fun `diagnosekode trimmes, og tom kode slås ikke opp`() {
        val response =
            stonaderResponse(
                stønad(vedtak(evaluering = evaluering(diagnose1 = " E10.9 ", diagnose2 = "   "))),
            ).tilInfotrygdStonaderResponse()

        assertThat(response.diagnosetekster).containsOnlyKeys("E10.9")
    }

    @Test
    fun `ICD-10-tekst slås opp for diagnose1, og ukjent kode får ingen tekst`() {
        val response =
            stonaderResponse(
                stønad(vedtak(evaluering = evaluering(diagnose1 = "E10.9", diagnose2 = "ZZZ99"))),
            ).tilInfotrygdStonaderResponse()

        assertThat(response.diagnosetekster).containsExactlyEntriesOf(
            mapOf("E10.9" to "Diabetes mellitus type 1 uten komplikasjoner"),
        )
    }

    @Test
    fun `motstridendeFelt DIAGNOSE_1 med diagnose1 null gir tekst bare for diagnose2`() {
        val response =
            stonaderResponse(
                stønad(
                    vedtak(
                        evaluering =
                            evaluering(
                                diagnose1 = null,
                                diagnose2 = "E10.9",
                                motstridendeFelt = setOf(InfotrygdEvaluering.MotstridendeFelt.DIAGNOSE_1),
                            ),
                    ),
                ),
            ).tilInfotrygdStonaderResponse()

        assertThat(response.diagnosetekster).containsExactlyEntriesOf(
            mapOf("E10.9" to "Diabetes mellitus type 1 uten komplikasjoner"),
        )
    }

    @Test
    fun `samme diagnosekode i flere vedtak og stønader gir én tekst`() {
        val response =
            stonaderResponse(
                stønad(
                    vedtak(vedtakId = 1L, evaluering = evaluering(diagnose1 = "E10.9")),
                    vedtak(vedtakId = 2L, evaluering = evaluering(diagnose1 = "E10.9")),
                ),
                stønad(vedtak(vedtakId = 3L, evaluering = evaluering(diagnose2 = "E10.9")), stønadId = 2L),
            ).tilInfotrygdStonaderResponse()

        assertThat(response.diagnosetekster).hasSize(1)
    }

    @Test
    fun `vedtak uten evaluering gir ingen diagnosetekst, og stønadene beholdes`() {
        val stønader = listOf(stønad(vedtak(evaluering = null)))

        val response = StonaderResponse(stonader = stønader).tilInfotrygdStonaderResponse()

        assertThat(response.diagnosetekster).isEmpty()
        assertThat(response.stonader).isEqualTo(stønader)
    }

    private fun stonaderResponse(vararg stønader: InfotrygdStonadResponse) = StonaderResponse(stonader = stønader.toList())

    private fun stønad(
        vararg vedtak: InfotrygdVedtakResponse,
        stønadId: Long = 1L,
    ) = InfotrygdStonadResponse(
        stonadId = stønadId,
        personnr = "12345678901",
        kodeRutine = "GS",
        datoStart = LocalDate.parse("2020-01-01"),
        vedtak = vedtak.toList(),
    )

    private fun vedtak(
        vedtakId: Long = 2L,
        evaluering: InfotrygdEvaluering? = null,
    ) = InfotrygdVedtakResponse(
        vedtakId = vedtakId,
        tknr = "0301",
        saksblokk = "A",
        saksnr = 1L,
        typeSak = "S",
        kodeResultat = "I",
        datoInnvFom = LocalDate.parse("2020-01-01"),
        datoMottattSak = LocalDate.parse("2019-12-15"),
        kodeVedtaksnivaa = "L",
        typeBeregning = "M",
        tidspunktReg = LocalDateTime.parse("2020-01-01T12:00:00"),
        brukerid = "Z123456",
        endringer = listOf("F"),
        delytelser = emptyList(),
        gh = emptyList(),
        evaluering = evaluering,
    )

    private fun evaluering(
        diagnose1: String? = null,
        diagnose2: String? = null,
        motstridendeFelt: Set<InfotrygdEvaluering.MotstridendeFelt> = emptySet(),
    ) = InfotrygdEvaluering(
        månedNotertIInfotrygd = "2020-01",
        diagnose1 = diagnose1,
        diagnose2 = diagnose2,
        motstridendeFelt = motstridendeFelt,
    )
}
