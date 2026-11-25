package no.nav.grunn.og.hjelpestonad.pdl

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakPersonService
import no.nav.grunn.og.hjelpestonad.felles.sikkerhet.SikkerhetContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

class PdlServiceTest {
    val pdlClient = mockk<PdlClient>()
    val fagsakPersonService = mockk<FagsakPersonService>()
    val pdlService = PdlService(pdlClient, fagsakPersonService)

    @BeforeEach
    fun setup() {
        mockkObject(SikkerhetContext)
        every { SikkerhetContext.erMaskinTilMaskinToken() } returns false
    }

    @AfterEach
    fun clearMocks() {
        unmockkObject(SikkerhetContext)
    }

    @Test
    fun `hent første navn ved response fra pdl`() {
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

    @Test
    fun `hentPersonMedPersonIdent returnerer person ved bruk av gyldig personIdent`() {
        every { pdlClient.hentPersonDataOBOToken(any()) } returns
            HentPersonData(
                hentPerson =
                    HentPerson(
                        navn = listOf(Navn("Fornavn", null, "Etternavn")),
                        foedselsdato = listOf(Foedselsdato(LocalDate.of(1990, 1, 15))),
                    ),
            )

        val personResponse = pdlService.hentPersonMedPersonIdent("S123456")

        assertThat(personResponse?.navn?.fornavn).isEqualTo("Fornavn")
        assertThat(personResponse?.navn?.etternavn).isEqualTo("Etternavn")
    }

    @Test
    fun `hentPersonMedPersonIdent kaster PdlException naar personIdent er null eller ugyldig`() {
        every { pdlClient.hentPersonDataOBOToken(any()) } returns null

        assertThrows<PdlException> { pdlService.hentPersonMedPersonIdent(null) }

        assertThrows<PdlException> { pdlService.hentPersonMedPersonIdent("ugyldig_ident") }
    }

    @Test
    fun `hentIdentitetOgDoedsfallForMigrering bruker OBO-token når kallet har brukerkontekst`() {
        every { SikkerhetContext.erMaskinTilMaskinToken() } returns false
        val forventetPerson =
            PersonIdentitetOgDoedsfall(
                falskIdentitet = FalskIdentitetEnkel(erFalsk = true),
                doedsfall = listOf(DoedsfallEnkel(doedsdato = LocalDate.of(2025, 4, 1))),
            )
        every { pdlClient.hentIdentitetOgDoedsfallOBOToken(any()) } returns forventetPerson

        val personIdentitetOgDoedsfall = pdlService.hentIdentitetOgDoedsfallForMigrering("01010199999")

        assertThat(personIdentitetOgDoedsfall).isEqualTo(forventetPerson)
        verify(exactly = 0) { pdlClient.hentIdentitetOgDoedsfallMaskinToken(any()) }
    }

    @Test
    fun `hentIdentitetOgDoedsfallForMigrering bruker maskintoken når kallet er maskin til maskin`() {
        every { SikkerhetContext.erMaskinTilMaskinToken() } returns true
        val forventetPerson =
            PersonIdentitetOgDoedsfall(
                falskIdentitet = null,
                doedsfall = emptyList(),
            )
        every { pdlClient.hentIdentitetOgDoedsfallMaskinToken(any()) } returns forventetPerson

        val personIdentitetOgDoedsfall = pdlService.hentIdentitetOgDoedsfallForMigrering("01010199999")

        assertThat(personIdentitetOgDoedsfall).isEqualTo(forventetPerson)
        verify(exactly = 0) { pdlClient.hentIdentitetOgDoedsfallOBOToken(any()) }
    }

    @Test
    fun `hentIdentitetOgDoedsfallForMigrering kaster PdlException når personen ikke finnes`() {
        every { SikkerhetContext.erMaskinTilMaskinToken() } returns false
        every { pdlClient.hentIdentitetOgDoedsfallOBOToken(any()) } returns null

        assertThrows<PdlException> { pdlService.hentIdentitetOgDoedsfallForMigrering("01010199999") }
    }
}
