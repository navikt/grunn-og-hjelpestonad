package no.nav.grunn.og.hjelpestonad.fagsak

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.ApplicationLocalSetup
import no.nav.grunn.og.hjelpestonad.fagsak.domain.StønadType
import no.nav.grunn.og.hjelpestonad.fagsak.dto.FagsakResponse
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.tilgangskontroll.TilgangService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.util.UUID

@WebMvcTest(FagsakController::class)
@ContextConfiguration(classes = [ApplicationLocalSetup::class])
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("integrasjonstest")
class FagsakControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var fagsakService: FagsakService

    @MockkBean(relaxed = true)
    private lateinit var tilgangService: TilgangService

    private val personident = "12345678910"
    private val fagsakPersonId = UUID.randomUUID()
    private val stønadstype = StønadType.BARNETILSYN

    private val fagsak =
        FagsakResponse(
            id = UUID.randomUUID(),
            fagsakPersonId = fagsakPersonId,
            personident = personident,
            stønadstype = stønadstype,
            eksternId = 1L,
        )

    @Test
    fun `opprett fagsak sjekker tilgang til personident og returnerer fagsak`() {
        every { fagsakService.hentEllerOpprettFagsak(personident, stønadstype) } returns fagsak

        mockMvc
            .post("/api/fagsak") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"personident":"$personident","stønadstype":"BARNETILSYN"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.fagsakPersonId") { value(fagsakPersonId.toString()) }
                jsonPath("$.personident") { value(personident) }
            }

        verify(exactly = 1) { tilgangService.validerTilgangTilPerson(personident) }
        verify(exactly = 1) { fagsakService.hentEllerOpprettFagsak(personident, stønadstype) }
    }

    @Test
    fun `opprett fagsak avviser ugyldig personident før tilgangssjekk og opprettelse`() {
        mockMvc
            .post("/api/fagsak") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"personident":"123","stønadstype":"BARNETILSYN"}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.melding") { value("Personident må være 11 siffer") }
            }

        verify(exactly = 0) { tilgangService.validerTilgangTilPerson(any()) }
        verify(exactly = 0) { fagsakService.hentEllerOpprettFagsak(any(), any()) }
    }

    @Test
    fun `opprett fagsak avviser request uten personident`() {
        mockMvc
            .post("/api/fagsak") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"fagsakPersonId":"$fagsakPersonId","stønadstype":"BARNETILSYN"}"""
            }.andExpect {
                status { isBadRequest() }
            }

        verify(exactly = 0) { fagsakService.hentEllerOpprettFagsak(any(), any()) }
    }

    @Test
    fun `hent fagsak sjekker tilgang til fagsakPersonId og oppretter ingenting`() {
        every { fagsakService.hentFagsak(fagsakPersonId, stønadstype) } returns fagsak

        mockMvc
            .get("/api/fagsak/fagsak-person/$fagsakPersonId/BARNETILSYN")
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(fagsak.id.toString()) }
            }

        verify(exactly = 1) { tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId) }
        verify(exactly = 0) { fagsakService.hentEllerOpprettFagsak(any(), any()) }
    }

    @Test
    fun `hent fagsak gir 404 når fagsaken ikke finnes`() {
        every { fagsakService.hentFagsak(fagsakPersonId, stønadstype) } throws
            Feil(melding = "Fant ikke fagsak", httpStatus = HttpStatus.NOT_FOUND)

        mockMvc
            .get("/api/fagsak/fagsak-person/$fagsakPersonId/BARNETILSYN")
            .andExpect { status { isNotFound() } }
    }
}
