package no.nav.grunn.og.hjelpestonad.migrering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.pdl.DødsfallEnkel
import no.nav.grunn.og.hjelpestonad.pdl.FalskIdentitetEnkel
import no.nav.grunn.og.hjelpestonad.pdl.PdlException
import no.nav.grunn.og.hjelpestonad.pdl.PdlService
import no.nav.grunn.og.hjelpestonad.pdl.PersonopplysningerForMigrering
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.time.LocalDate
import kotlin.test.Test

class MigreringPersonValideringServiceTest {
    private val pdlService = mockk<PdlService>()
    private val migreringPersonValideringService = MigreringPersonValideringService(pdlService)

    @Test
    fun `henter ferske opplysninger fra PDL og godkjenner levende person uten falsk identitet`() {
        every { pdlService.hentFalskIdentitetOgDødsfallForMigrering("12345678901") } returns
            PersonopplysningerForMigrering(falskIdentitet = null, dødsfall = emptyList())

        val personopplysningerForMigreringValideringResultat = migreringPersonValideringService.validerPersonForMigrering("12345678901")

        assertThat(personopplysningerForMigreringValideringResultat.kanMigreres).isTrue()
        assertThat(personopplysningerForMigreringValideringResultat.blokkeringer).isEmpty()
        verify(exactly = 1) { pdlService.hentFalskIdentitetOgDødsfallForMigrering("12345678901") }
    }

    @Test
    fun `returnerer blokkeringer når PDL melder dødsfall eller falsk identitet`() {
        every { pdlService.hentFalskIdentitetOgDødsfallForMigrering("12345678901") } returns
            PersonopplysningerForMigrering(
                falskIdentitet = FalskIdentitetEnkel(erFalsk = true),
                dødsfall = listOf(DødsfallEnkel(dødsdato = LocalDate.of(2025, 1, 1))),
            )

        val personopplysningerForMigreringValideringResultat = migreringPersonValideringService.validerPersonForMigrering("12345678901")

        assertThat(personopplysningerForMigreringValideringResultat.kanMigreres).isFalse()
        assertThat(personopplysningerForMigreringValideringResultat.blokkeringer).containsExactlyInAnyOrder(
            PersonblokkeringForMigrering.DØD,
            PersonblokkeringForMigrering.FALSK_IDENTITET,
        )
    }

    @Test
    fun `propagerer PdlException ved feil mot PDL (fail-closed)`() {
        every { pdlService.hentFalskIdentitetOgDødsfallForMigrering("12345678901") } throws PdlException("Teknisk feil")

        assertThatThrownBy { migreringPersonValideringService.validerPersonForMigrering("12345678901") }
            .isInstanceOf(PdlException::class.java)
    }
}
