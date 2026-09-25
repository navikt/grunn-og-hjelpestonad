package no.nav.grunn.og.hjelpestonad.pdl

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakPersonService
import no.nav.grunn.og.hjelpestonad.felles.sikkerhet.SikkerhetContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.*
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
        mockkObject(SikkerhetContext)
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
    fun `hentBarnPersonidenter returnerer liste med barn identiteter`() {
        every { pdlClient.hentFamilieRelasjoner(any()) } returns
            FamilieRelasjonerResponse(
                hentPerson =
                    FamilieRelasjoner(
                        forelderBarnRelasjon =
                            listOf(
                                ForelderBarnRelasjon("111", Familierolle.BARN, Familierolle.MOR),
                                ForelderBarnRelasjon("222", Familierolle.FAR, Familierolle.MOR),
                            ),
                    ),
            )

        val barnIdentiteter = pdlService.hentBarnPersonidenter("S123456")

        assertThat(barnIdentiteter).containsOnly("111")
    }
}
