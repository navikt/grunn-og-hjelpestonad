package no.nav.grunn.og.hjelpestonad.infotrygd

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import no.nav.grunn.og.hjelpestonad.infotrygd.InfotrygdTestdata.evaluering
import no.nav.grunn.og.hjelpestonad.infotrygd.InfotrygdTestdata.stønad
import no.nav.grunn.og.hjelpestonad.infotrygd.InfotrygdTestdata.vedtak
import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.InfotrygdStonadResponse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class GjeldendeInfotrygdPerioderTest {
    @Test
    fun `ett løpende vedtak gir én åpen periode`() {
        val perioder = utled(stønad(vedtak(vedtakId = 1, fom = "2020-01-01")))

        assertThat(perioder.forenklet()).containsExactly(Forenklet("2020-01-01", null, 1))
    }

    @Test
    fun `ingen vedtak gir ingen perioder`() {
        assertThat(GjeldendeInfotrygdPerioder.utled(emptyList())).isEmpty()
        assertThat(utled(stønad())).isEmpty()
    }

    @Test
    fun `nyere vedtak overskriver eldre der de overlapper, og eldre gjelder igjen etterpå`() {
        val perioder =
            utled(
                stønad(
                    vedtak(vedtakId = 1, fom = "2020-01-01"),
                    vedtak(vedtakId = 2, fom = "2022-01-01", tom = "2022-12-31"),
                ),
            )

        assertThat(perioder.forenklet()).containsExactly(
            Forenklet("2020-01-01", "2021-12-31", 1),
            Forenklet("2022-01-01", "2022-12-31", 2),
            Forenklet("2023-01-01", null, 1),
        )
    }

    @Test
    fun `nyere vedtak uten tom overskriver alt framover`() {
        val perioder =
            utled(
                stønad(
                    vedtak(vedtakId = 1, fom = "2020-01-01", tom = "2025-12-31"),
                    vedtak(vedtakId = 2, fom = "2022-01-01"),
                ),
            )

        assertThat(perioder.forenklet()).containsExactly(
            Forenklet("2020-01-01", "2021-12-31", 1),
            Forenklet("2022-01-01", null, 2),
        )
    }

    @Test
    fun `eldre vedtak overskriver ikke nyere, uansett rekkefølgen i svaret`() {
        val perioder =
            utled(
                stønad(
                    vedtak(vedtakId = 2, fom = "2022-01-01", tom = "2022-12-31"),
                    vedtak(vedtakId = 1, fom = "2020-01-01"),
                ),
            )

        assertThat(perioder.forenklet()).containsExactly(
            Forenklet("2020-01-01", "2021-12-31", 1),
            Forenklet("2022-01-01", "2022-12-31", 2),
            Forenklet("2023-01-01", null, 1),
        )
    }

    @Test
    fun `vedtakId bestemmer rekkefølgen, ikke tidspunktReg`() {
        val perioder =
            utled(
                stønad(
                    vedtak(vedtakId = 1, fom = "2020-01-01", tidspunktReg = LocalDateTime.parse("2024-01-01T00:00:00")),
                    vedtak(vedtakId = 2, fom = "2020-01-01", tidspunktReg = LocalDateTime.parse("2020-01-01T00:00:00")),
                ),
            )

        assertThat(perioder.forenklet()).containsExactly(Forenklet("2020-01-01", null, 2))
    }

    @Test
    fun `nyere vedtak som dekker hele det eldre gir én periode`() {
        val perioder =
            utled(
                stønad(
                    vedtak(vedtakId = 1, fom = "2021-01-01", tom = "2021-12-31"),
                    vedtak(vedtakId = 2, fom = "2020-01-01", tom = "2022-12-31"),
                ),
            )

        assertThat(perioder.forenklet()).containsExactly(Forenklet("2020-01-01", "2022-12-31", 2))
    }

    @Test
    fun `går på tvers av stønader, og perioden får stønaden vedtaket hører til`() {
        val perioder =
            utled(
                stønad(vedtak(vedtakId = 1, fom = "2020-01-01"), stønadId = 10),
                stønad(vedtak(vedtakId = 2, fom = "2022-01-01"), stønadId = 20),
            )

        assertThat(perioder.map { it.stonadId to it.vedtak.vedtakId }).containsExactly(10L to 1L, 20L to 2L)
        assertThat(perioder.forenklet()).containsExactly(
            Forenklet("2020-01-01", "2021-12-31", 1),
            Forenklet("2022-01-01", null, 2),
        )
    }

    @Test
    fun `datoOpphor på stønaden kutter ikke perioden`() {
        val perioder =
            utled(stønad(vedtak(vedtakId = 1, fom = "2020-01-01"), datoOpphor = LocalDate.parse("2021-01-01")))

        assertThat(perioder.forenklet()).containsExactly(Forenklet("2020-01-01", null, 1))
    }

    @Test
    fun `tidsrom uten vedtak gir ingen periode`() {
        val perioder =
            utled(
                stønad(
                    vedtak(vedtakId = 1, fom = "2020-01-01", tom = "2020-12-31"),
                    vedtak(vedtakId = 2, fom = "2022-01-01", tom = "2022-12-31"),
                ),
            )

        assertThat(perioder.forenklet()).containsExactly(
            Forenklet("2020-01-01", "2020-12-31", 1),
            Forenklet("2022-01-01", "2022-12-31", 2),
        )
    }

    @Nested
    inner class UgyldigPeriode {
        @Test
        fun `vedtak med tom før fom gir feil`() {
            assertThatThrownBy {
                utled(
                    stønad(
                        vedtak(vedtakId = 1, fom = "2020-01-01"),
                        vedtak(vedtakId = 4711, fom = "2022-01-01", tom = "2021-12-31"),
                    ),
                )
            }.isInstanceOf(UgyldigInput::class.java)
                .hasMessageContainingAll("4711", "01.01.2022", "31.12.2021")
        }

        @Test
        fun `vedtak med tom lik fom gir feil`() {
            assertThatThrownBy {
                utled(
                    stønad(
                        vedtak(vedtakId = 1, fom = "2020-01-01"),
                        vedtak(vedtakId = 4711, fom = "2022-01-01", tom = "2022-01-01"),
                    ),
                )
            }.isInstanceOf(UgyldigInput::class.java)
                .hasMessageContainingAll("4711", "01.01.2022")
        }

        @Test
        fun `vedtak med tom dagen etter fom er med`() {
            val perioder = utled(stønad(vedtak(vedtakId = 1, fom = "2022-01-01", tom = "2022-01-02")))

            assertThat(perioder.forenklet()).containsExactly(Forenklet("2022-01-01", "2022-01-02", 1))
        }
    }

    @Test
    fun `perioden har vedtaket med arv`() {
        val perioder =
            utled(
                stønad(
                    vedtak(vedtakId = 1, fom = "2020-01-01", tom = "2020-12-31", evaluering = evaluering("A00")),
                    vedtak(vedtakId = 2, fom = "2021-01-01"),
                ),
            )

        val periode2 = perioder.single { it.vedtak.vedtakId == 2L }
        assertThat(
            periode2.vedtak.evaluering
                ?.verdi
                ?.diagnose1,
        ).isEqualTo("A00")
        assertThat(
            periode2.vedtak.evaluering
                ?.arvetFraVedtak
                ?.vedtakId,
        ).isEqualTo(1)
    }

    @Nested
    inner class AntallVedtakUtenforRekkefølge {
        @Test
        fun `er null når tidspunktReg stiger med vedtakId`() {
            val stønader = InfotrygdArv.fyllVedtakMedVerdierFraTidligereVedtak(listOf(stønad(vedtak(vedtakId = 1), vedtak(vedtakId = 2), vedtak(vedtakId = 3))))

            assertThat(GjeldendeInfotrygdPerioder.antallVedtakUtenforRekkefølge(stønader)).isZero()
        }

        @Test
        fun `teller vedtak registrert før vedtaket med nærmeste lavere vedtakId, på tvers av stønader`() {
            val stønader =
                InfotrygdArv.fyllVedtakMedVerdierFraTidligereVedtak(
                    listOf(
                        stønad(vedtak(vedtakId = 1, tidspunktReg = LocalDateTime.parse("2022-01-01T00:00:00")), stønadId = 1),
                        stønad(vedtak(vedtakId = 2, tidspunktReg = LocalDateTime.parse("2021-01-01T00:00:00")), stønadId = 2),
                    ),
                )

            assertThat(GjeldendeInfotrygdPerioder.antallVedtakUtenforRekkefølge(stønader)).isEqualTo(1)
        }
    }

    private data class Forenklet(
        val fom: String,
        val tom: String?,
        val vedtakId: Long,
    )

    private fun List<GjeldendeInfotrygdPeriode>.forenklet() = map { Forenklet(it.fom.toString(), it.tom?.toString(), it.vedtak.vedtakId) }

    private fun utled(vararg stønader: InfotrygdStonadResponse) = GjeldendeInfotrygdPerioder.utled(InfotrygdArv.fyllVedtakMedVerdierFraTidligereVedtak(stønader.toList()))
}
