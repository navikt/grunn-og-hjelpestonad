package no.nav.grunn.og.hjelpestonad.fagsak

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.ApplicationLocalSetup
import no.nav.grunn.og.hjelpestonad.fagsak.domain.StønadType
import no.nav.grunn.og.hjelpestonad.fagsak.dto.FagsakResponse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.util.UUID

@WebMvcTest(FagsakController::class)
@ContextConfiguration(classes = [ApplicationLocalSetup::class])
@Import(FagsakControllerRolleTest.MetodesikkerhetConfig::class)
@AutoConfigureMockMvc
@ActiveProfiles("integrasjonstest")
class FagsakControllerRolleTest {
    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    open class MetodesikkerhetConfig

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var fagsakService: FagsakService
    private val personident = "12345678910"
    private val fagsakPersonId = UUID.randomUUID()
    private val hentUrl = "/api/fagsak/fagsak-person/$fagsakPersonId/BARNETILSYN"
    private val fagsak =
        FagsakResponse(
            id = UUID.randomUUID(),
            fagsakPersonId = fagsakPersonId,
            personident = personident,
            stønadstype = StønadType.BARNETILSYN,
            eksternId = 1L,
        )

    @Test
    @WithMockUser(roles = ["SAKSBEHANDLER"])
    fun `saksbehandler får hente fagsak`() {
        every { fagsakService.hentFagsak(fagsakPersonId, StønadType.BARNETILSYN) } returns fagsak

        mockMvc.get(hentUrl).andExpect { status { isOk() } }
    }

    @Test
    @WithMockUser(roles = ["BESLUTTER"])
    fun `beslutter får hente fagsak`() {
        every { fagsakService.hentFagsak(fagsakPersonId, StønadType.BARNETILSYN) } returns fagsak

        mockMvc.get(hentUrl).andExpect { status { isOk() } }
    }

    @Test
    @WithMockUser(roles = ["LESETILGANG"])
    fun `bruker med lesetilgang får hente fagsak`() {
        every { fagsakService.hentFagsak(fagsakPersonId, StønadType.BARNETILSYN) } returns fagsak

        mockMvc.get(hentUrl).andExpect { status { isOk() } }
    }

    @Test
    @WithMockUser
    fun `bruker uten rolle får ikke hente fagsak`() {
        mockMvc.get(hentUrl).andExpect { status { isForbidden() } }
        verify(exactly = 0) { fagsakService.hentFagsak(any(), any()) }
    }

    @Test
    @WithMockUser(roles = ["SAKSBEHANDLER"])
    fun `saksbehandler får opprette fagsak`() {
        every { fagsakService.hentEllerOpprettFagsak(personident, StønadType.BARNETILSYN) } returns fagsak

        opprettFagsak().andExpect { status { isOk() } }
    }

    @Test
    @WithMockUser(roles = ["BESLUTTER", "LESETILGANG"])
    fun `bruker uten saksbehandlerrollen får ikke opprette fagsak`() {
        opprettFagsak().andExpect { status { isForbidden() } }
        verify(exactly = 0) { fagsakService.hentEllerOpprettFagsak(any(), any()) }
    }

    private fun opprettFagsak() =
        mockMvc.post("/api/fagsak") {
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = """{"personident":"$personident","stønadstype":"BARNETILSYN"}"""
        }
}
