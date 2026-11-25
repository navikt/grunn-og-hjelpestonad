package no.nav.grunn.og.hjelpestonad.migrering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.pdl.DoedsfallEnkel
import no.nav.grunn.og.hjelpestonad.pdl.FalskIdentitetEnkel
import no.nav.grunn.og.hjelpestonad.pdl.PdlException
import no.nav.grunn.og.hjelpestonad.pdl.PdlService
import no.nav.grunn.og.hjelpestonad.pdl.PersonIdentitetOgDoedsfall
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.time.LocalDate
import kotlin.test.Test

class MigreringPersonValideringServiceTest {
    private val pdlService = mockk<PdlService>()
    private val migreringPersonValideringService = MigreringPersonValideringService(pdlService)

    @Test
    fun `henter ferske opplysninger fra PDL og godkjenner levende person uten falsk identitet`() {
        every { pdlService.hentIdentitetOgDoedsfallForMigrering("12345678901") } returns
            PersonIdentitetOgDoedsfall(falskIdentitet = null, doedsfall = emptyList())

        val valideringsresultat = migreringPersonValideringService.validerPersonForMigrering("12345678901")

        assertThat(valideringsresultat.kanMigreres).isTrue()
        assertThat(valideringsresultat.blokkeringer).isEmpty()
        verify(exactly = 1) { pdlService.hentIdentitetOgDoedsfallForMigrering("12345678901") }
    }

    @Test
    fun `returnerer blokkeringer når PDL melder dødsfall eller falsk identitet`() {
        every { pdlService.hentIdentitetOgDoedsfallForMigrering("12345678901") } returns
            PersonIdentitetOgDoedsfall(
                falskIdentitet = FalskIdentitetEnkel(erFalsk = true),
                doedsfall = listOf(DoedsfallEnkel(doedsdato = LocalDate.of(2025, 1, 1))),
            )

        val valideringsresultat = migreringPersonValideringService.validerPersonForMigrering("12345678901")

        assertThat(valideringsresultat.kanMigreres).isFalse()
        assertThat(valideringsresultat.blokkeringer).containsExactlyInAnyOrder(
            MigreringPersonBlokkering.DØD,
            MigreringPersonBlokkering.FALSK_IDENTITET,
        )
    }

    @Test
    fun `propagerer PdlException ved feil mot PDL (fail-closed)`() {
        every { pdlService.hentIdentitetOgDoedsfallForMigrering("12345678901") } throws PdlException("Teknisk feil")

        assertThatThrownBy { migreringPersonValideringService.validerPersonForMigrering("12345678901") }
            .isInstanceOf(PdlException::class.java)
    }
}
