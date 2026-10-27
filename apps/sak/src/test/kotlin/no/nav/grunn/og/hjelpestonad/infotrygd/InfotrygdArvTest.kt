package no.nav.grunn.og.hjelpestonad.infotrygd

import no.nav.grunn.og.hjelpestonad.infotrygd.InfotrygdTestdata.evaluering
import no.nav.grunn.og.hjelpestonad.infotrygd.InfotrygdTestdata.gh
import no.nav.grunn.og.hjelpestonad.infotrygd.InfotrygdTestdata.stønad
import no.nav.grunn.og.hjelpestonad.infotrygd.InfotrygdTestdata.vedtak
import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.InfotrygdStonadResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class InfotrygdArvTest {
    @Nested
    inner class ArvAvEvaluering {
        @Test
        fun `arver fra nærmeste vedtak med lavere vedtakId som har egen evaluering`() {
            val a = vedtak(vedtakId = 1, evaluering = evaluering("A00"))
            val b = vedtak(vedtakId = 2, evaluering = evaluering("B00"))
            val c = vedtak(vedtakId = 3)

            val arvet = medArv(stønad(a, b, c)).vedtak(3)

            assertThat(arvet.evaluering?.verdi).isEqualTo(b.evaluering)
            assertThat(arvet.evaluering?.arvetFraVedtak).isEqualTo(InfotrygdKildevedtak(2, b.tidspunktReg))
        }

        @Test
        fun `arver ikke fra vedtak med høyere vedtakId`() {
            val stønader = medArv(stønad(vedtak(vedtakId = 1), vedtak(vedtakId = 2, evaluering = evaluering())))

            assertThat(stønader.vedtak(1).evaluering).isNull()
        }

        @Test
        fun `vedtakId bestemmer rekkefølgen, ikke tidspunktReg`() {
            val tidlig = LocalDateTime.parse("2020-01-01T00:00:00")
            val sent = LocalDateTime.parse("2024-01-01T00:00:00")
            val stønader =
                medArv(
                    stønad(
                        vedtak(vedtakId = 1, tidspunktReg = sent, evaluering = evaluering("A00")),
                        vedtak(vedtakId = 2, tidspunktReg = tidlig),
                    ),
                )

            assertThat(
                stønader
                    .vedtak(2)
                    .evaluering
                    ?.arvetFraVedtak
                    ?.vedtakId,
            ).isEqualTo(1)
        }

        @Test
        fun `arver fra vedtak på en annen stønad`() {
            val stønader =
                medArv(
                    stønad(vedtak(vedtakId = 1, evaluering = evaluering("A00")), stønadId = 1),
                    stønad(vedtak(vedtakId = 2), stønadId = 2),
                )

            assertThat(
                stønader
                    .vedtak(2)
                    .evaluering
                    ?.verdi
                    ?.diagnose1,
            ).isEqualTo("A00")
            assertThat(
                stønader
                    .vedtak(2)
                    .evaluering
                    ?.arvetFraVedtak
                    ?.vedtakId,
            ).isEqualTo(1)
        }

        @Test
        fun `arver ikke i kjede, både B og C peker på A`() {
            val stønader =
                medArv(
                    stønad(
                        vedtak(vedtakId = 1, evaluering = evaluering("A00")),
                        vedtak(vedtakId = 2),
                        vedtak(vedtakId = 3),
                    ),
                )

            assertThat(
                stønader
                    .vedtak(2)
                    .evaluering
                    ?.arvetFraVedtak
                    ?.vedtakId,
            ).isEqualTo(1)
            assertThat(
                stønader
                    .vedtak(3)
                    .evaluering
                    ?.arvetFraVedtak
                    ?.vedtakId,
            ).isEqualTo(1)
        }

        @Test
        fun `overskriver ikke egen evaluering, og arvetFraVedtak er null`() {
            val egen = evaluering("B00")
            val stønader = medArv(stønad(vedtak(vedtakId = 1, evaluering = evaluering("A00")), vedtak(vedtakId = 2, evaluering = egen)))

            assertThat(stønader.vedtak(2).evaluering).isEqualTo(VerdiMedKilde(verdi = egen, arvetFraVedtak = null))
        }

        @Test
        fun `arver fra vedtak med tom før eller lik fom`() {
            val stønader =
                medArv(
                    stønad(
                        vedtak(vedtakId = 1, fom = "2020-01-01", tom = "2020-01-01", evaluering = evaluering("A00")),
                        vedtak(vedtakId = 2),
                    ),
                )

            assertThat(
                stønader
                    .vedtak(2)
                    .evaluering
                    ?.arvetFraVedtak
                    ?.vedtakId,
            ).isEqualTo(1)
        }

        @Test
        fun `uten tidligere evaluering forblir evalueringen null`() {
            val stønader = medArv(stønad(vedtak(vedtakId = 1), vedtak(vedtakId = 2)))

            assertThat(stønader.vedtak(2).evaluering).isNull()
        }
    }

    @Nested
    inner class ArvAvGh {
        @Test
        fun `arver hele gh-listen fra nærmeste vedtak med lavere vedtakId som har egen gh`() {
            val ghA = listOf(gh(10))
            val ghB = listOf(gh(20), gh(30))
            val stønader =
                medArv(stønad(vedtak(vedtakId = 1, gh = ghA), vedtak(vedtakId = 2, gh = ghB), vedtak(vedtakId = 3)))

            assertThat(stønader.vedtak(3).gh?.verdi).isEqualTo(ghB)
            assertThat(
                stønader
                    .vedtak(3)
                    .gh
                    ?.arvetFraVedtak
                    ?.vedtakId,
            ).isEqualTo(2)
        }

        @Test
        fun `arver ikke fra vedtak med høyere vedtakId`() {
            val stønader = medArv(stønad(vedtak(vedtakId = 1), vedtak(vedtakId = 2, gh = listOf(gh()))))

            assertThat(stønader.vedtak(1).gh).isNull()
        }

        @Test
        fun `arver fra vedtak på en annen stønad`() {
            val stønader =
                medArv(
                    stønad(vedtak(vedtakId = 1, gh = listOf(gh())), stønadId = 1),
                    stønad(vedtak(vedtakId = 2), stønadId = 2),
                )

            assertThat(
                stønader
                    .vedtak(2)
                    .gh
                    ?.arvetFraVedtak
                    ?.vedtakId,
            ).isEqualTo(1)
        }

        @Test
        fun `arver ikke i kjede, både B og C peker på A`() {
            val stønader = medArv(stønad(vedtak(vedtakId = 1, gh = listOf(gh())), vedtak(vedtakId = 2), vedtak(vedtakId = 3)))

            assertThat(
                stønader
                    .vedtak(2)
                    .gh
                    ?.arvetFraVedtak
                    ?.vedtakId,
            ).isEqualTo(1)
            assertThat(
                stønader
                    .vedtak(3)
                    .gh
                    ?.arvetFraVedtak
                    ?.vedtakId,
            ).isEqualTo(1)
        }

        @Test
        fun `overskriver ikke egen gh, og arvetFraVedtak er null`() {
            val egen = listOf(gh(20))
            val stønader = medArv(stønad(vedtak(vedtakId = 1, gh = listOf(gh(10))), vedtak(vedtakId = 2, gh = egen)))

            assertThat(stønader.vedtak(2).gh).isEqualTo(VerdiMedKilde(verdi = egen, arvetFraVedtak = null))
        }
    }

    @Test
    fun `gh er null når verken vedtaket eller et tidligere vedtak har gh`() {
        val stønader = medArv(stønad(vedtak(vedtakId = 1), vedtak(vedtakId = 2)))

        assertThat(stønader.vedtak(2).gh).isNull()
    }

    @Test
    fun `evaluering og gh arves hver for seg og kan komme fra ulike vedtak`() {
        val stønader =
            medArv(
                stønad(
                    vedtak(vedtakId = 1, evaluering = evaluering()),
                    vedtak(vedtakId = 2, gh = listOf(gh())),
                    vedtak(vedtakId = 3),
                ),
            )

        assertThat(
            stønader
                .vedtak(3)
                .evaluering
                ?.arvetFraVedtak
                ?.vedtakId,
        ).isEqualTo(1)
        assertThat(
            stønader
                .vedtak(3)
                .gh
                ?.arvetFraVedtak
                ?.vedtakId,
        ).isEqualTo(2)
    }

    @Test
    fun `stønadsfeltene og vedtakenes rekkefølge beholdes`() {
        val stønad = stønad(vedtak(vedtakId = 2), vedtak(vedtakId = 1), stønadId = 7)

        val medArv = InfotrygdArv.fyllVedtakMedVerdierFraTidligereVedtak(listOf(stønad)).single()

        assertThat(medArv.stonadId).isEqualTo(7)
        assertThat(medArv.personnr).isEqualTo(stønad.personnr)
        assertThat(medArv.vedtak.map { it.vedtakId }).containsExactly(2L, 1L)
    }

    private fun medArv(vararg stønader: InfotrygdStonadResponse) = InfotrygdArv.fyllVedtakMedVerdierFraTidligereVedtak(stønader.toList())

    private fun List<InfotrygdStonadMedArv>.vedtak(vedtakId: Long) = flatMap { it.vedtak }.single { it.vedtakId == vedtakId }
}
