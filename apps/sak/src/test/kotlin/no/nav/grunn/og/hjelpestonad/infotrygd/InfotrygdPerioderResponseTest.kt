package no.nav.grunn.og.hjelpestonad.infotrygd

import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.InfotrygdEvaluering
import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.PeriodeResponse
import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.PerioderResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class InfotrygdPerioderResponseTest {
    @Test
    fun `diagnosekode trimmes, og tom kode slås ikke opp`() {
        val response =
            perioderResponse(
                grunnstønad = listOf(periode(evaluering = evaluering(diagnose1 = " E10.9 ", diagnose2 = "   "))),
            ).tilInfotrygdPerioderResponse()

        assertThat(response.diagnosetekster).containsOnlyKeys("E10.9")
    }

    @Test
    fun `ICD-10-tekst slås opp for diagnose1, og ukjent kode får ingen tekst`() {
        val response =
            perioderResponse(
                grunnstønad = listOf(periode(evaluering = evaluering(diagnose1 = "E10.9", diagnose2 = "ZZZ99"))),
            ).tilInfotrygdPerioderResponse()

        assertThat(response.diagnosetekster).containsExactlyEntriesOf(
            mapOf("E10.9" to "Diabetes mellitus type 1 uten komplikasjoner"),
        )
    }

    @Test
    fun `motstridendeFelt DIAGNOSE_1 med diagnose1 null gir tekst bare for diagnose2`() {
        val response =
            perioderResponse(
                hjelpestønad =
                    listOf(
                        periode(
                            stønadstype = PeriodeResponse.Stønadstype.HJELPESTØNAD,
                            evaluering =
                                evaluering(
                                    diagnose1 = null,
                                    diagnose2 = "E10.9",
                                    motstridendeFelt = setOf(InfotrygdEvaluering.MotstridendeFelt.DIAGNOSE_1),
                                ),
                        ),
                    ),
            ).tilInfotrygdPerioderResponse()

        assertThat(response.diagnosetekster).containsExactlyEntriesOf(
            mapOf("E10.9" to "Diabetes mellitus type 1 uten komplikasjoner"),
        )
    }

    @Test
    fun `samme diagnosekode i flere perioder gir én tekst`() {
        val response =
            perioderResponse(
                grunnstønad = listOf(periode(evaluering = evaluering(diagnose1 = "E10.9"))),
                hjelpestønad =
                    listOf(
                        periode(
                            stønadstype = PeriodeResponse.Stønadstype.HJELPESTØNAD,
                            evaluering = evaluering(diagnose1 = "E10.9"),
                        ),
                    ),
            ).tilInfotrygdPerioderResponse()

        assertThat(response.diagnosetekster).hasSize(1)
    }

    @Test
    fun `periode uten evaluering gir ingen diagnosetekst, og periodene beholdes`() {
        val grunnstønad = listOf(periode(evaluering = null))
        val hjelpestønad = emptyList<PeriodeResponse>()

        val response =
            perioderResponse(grunnstønad = grunnstønad, hjelpestønad = hjelpestønad)
                .tilInfotrygdPerioderResponse()

        assertThat(response.diagnosetekster).isEmpty()
        assertThat(response.grunnstønad).isEqualTo(grunnstønad)
        assertThat(response.hjelpestønad).isEqualTo(hjelpestønad)
    }

    private fun perioderResponse(
        grunnstønad: List<PeriodeResponse> = emptyList(),
        hjelpestønad: List<PeriodeResponse> = emptyList(),
    ) = PerioderResponse(grunnstønad = grunnstønad, hjelpestønad = hjelpestønad)

    private fun periode(
        stønadstype: PeriodeResponse.Stønadstype = PeriodeResponse.Stønadstype.GRUNNSTØNAD,
        evaluering: InfotrygdEvaluering? = null,
    ) = PeriodeResponse(
        personident = "12345678901",
        stønadstype = stønadstype,
        sakstype = "S",
        kode = "F",
        brukerId = "Z123456",
        stønadId = 1L,
        vedtakId = 2L,
        vedtakstidspunkt = LocalDateTime.parse("2020-01-01T12:00:00"),
        datoMottattSak = LocalDate.parse("2019-12-15"),
        vedtakKodeResultat = "I",
        startDato = LocalDate.parse("2020-01-01"),
        innvilgetFom = LocalDate.parse("2020-01-01"),
        typeDelytelse = "E",
        typeSats = "M",
        typeUtbetaling = "M",
        stønadFom = LocalDate.parse("2020-01-01"),
        beløp = BigDecimal("1234.50"),
        trygdetidOgSats = emptyList(),
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
