package no.nav.gjenlevende.bs.sak.pdl

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import no.nav.gjenlevende.bs.sak.fagsak.FagsakPersonService
import no.nav.gjenlevende.bs.sak.felles.sikkerhet.SikkerhetContext
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

class PdlServiceTest {
    val pdlClient = mockk<PdlClient>()
    val fagsakPersonService = mockk<FagsakPersonService>()
    val pdlService = PdlService(pdlClient, fagsakPersonService)

    @Test
    fun `hent første navn ved response fra pdl`() {
        mockkObject(SikkerhetContext)
        every { SikkerhetContext.erMaskinTilMaskinToken() } returns false

        every { fagsakPersonService.hentAktivIdent(any()) } returns "01010199999"

        every { pdlClient.hentPersonDataOBOToken(any()) } returns
            HentPersonData(
                hentPerson =
                    HentPerson(
                        navn =
                            listOf(
                                Navn("Fornavn", null, "Etternavn"),
                                Navn("Fornavn2", null, "Etternavn2"),
                            ),
                        foedselsdato = listOf(Foedselsdato(LocalDate.of(1990, 1, 15))),
                    ),
            )
        val fagsakPersonId = UUID.randomUUID()
        val navn = pdlService.hentPersonMedFagsakPersonId(fagsakPersonId)?.navn

        assertThat(navn?.fornavn).isEqualTo("Fornavn")
        assertThat(navn?.etternavn).isEqualTo("Etternavn")
    }
}
