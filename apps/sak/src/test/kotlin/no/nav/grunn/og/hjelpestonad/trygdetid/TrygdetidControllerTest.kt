package no.nav.grunn.og.hjelpestonad.trygdetid

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.ApplicationLocalSetup
import no.nav.grunn.og.hjelpestonad.tilgangskontroll.ManglerTilgangTilPersonFeil
import no.nav.grunn.og.hjelpestonad.tilgangskontroll.TilgangService
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
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.util.UUID

/**
 * Kjører med ekte method-security (@PreAuthorize på kontrolleren) og ekte sikkerhetsfilter.
 * Ingen test slår av security. Rollen settes med @WithMockUser. Hver 403-test har en kontroll
 * med samme rolle og csrf, slik at 403 bare kan komme fra rollen eller tilgangsavslaget.
 */
@WebMvcTest(TrygdetidController::class)
@ContextConfiguration(classes = [ApplicationLocalSetup::class])
@Import(TrygdetidControllerTest.MetodesikkerhetConfig::class)
@AutoConfigureMockMvc
@ActiveProfiles("integrasjonstest")
class TrygdetidControllerTest {
    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    open class MetodesikkerhetConfig

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var trygdetidService: TrygdetidService

    @MockkBean(relaxed = true)
    private lateinit var tilgangService: TilgangService

    private val behandlingId = UUID.randomUUID()
    private val trygdetidId = UUID.randomUUID()
    private val url = "/api/behandling/$behandlingId/trygdetid"
    private val slettUrl = "$url/$trygdetidId"
    private val requestJson = """{"teller":40,"kilde":"ALDERSPENSJON","begrunnelse":"Test"}"""
    private val avslag = ManglerTilgangTilPersonFeil(avvisningskode = null)

    @Test
    @WithMockUser(roles = ["SAKSBEHANDLER"])
    fun `personavslag gir 403 på GET og henter ikke trygdetid`() {
        every { tilgangService.validerTilgangTilBehandling(behandlingId) } throws avslag

        mockMvc.get(url).andExpect { status { isForbidden() } }

        verify(exactly = 0) { trygdetidService.hentPerioder(any()) }
    }

    @Test
    @WithMockUser(roles = ["SAKSBEHANDLER"])
    fun `personavslag gir 403 på POST og lagrer ikke trygdetid`() {
        every { tilgangService.validerTilgangTilBehandling(behandlingId) } throws avslag

        postTrygdetid().andExpect { status { isForbidden() } }

        verify(exactly = 0) { trygdetidService.lagrePeriode(any(), any()) }
    }

    @Test
    @WithMockUser(roles = ["SAKSBEHANDLER"])
    fun `personavslag gir 403 på DELETE og sletter ikke trygdetid`() {
        every { tilgangService.validerTilgangTilBehandling(behandlingId) } throws avslag

        mockMvc.delete(slettUrl) { with(csrf()) }.andExpect { status { isForbidden() } }

        verify(exactly = 0) { trygdetidService.slettPeriode(any(), any()) }
    }

    @Test
    @WithMockUser(roles = ["BESLUTTER", "LESETILGANG"])
    fun `uten saksbehandlerrollen får ikke hente trygdetid`() {
        mockMvc.get(url).andExpect { status { isForbidden() } }

        verify(exactly = 0) { tilgangService.validerTilgangTilBehandling(any()) }
        verify(exactly = 0) { trygdetidService.hentPerioder(any()) }
    }

    @Test
    @WithMockUser(roles = ["BESLUTTER", "LESETILGANG"])
    fun `uten saksbehandlerrollen får ikke lagre trygdetid`() {
        postTrygdetid().andExpect { status { isForbidden() } }

        verify(exactly = 0) { tilgangService.validerTilgangTilBehandling(any()) }
        verify(exactly = 0) { trygdetidService.lagrePeriode(any(), any()) }
    }

    @Test
    @WithMockUser(roles = ["BESLUTTER", "LESETILGANG"])
    fun `uten saksbehandlerrollen får ikke slette trygdetid`() {
        mockMvc.delete(slettUrl) { with(csrf()) }.andExpect { status { isForbidden() } }

        verify(exactly = 0) { tilgangService.validerTilgangTilBehandling(any()) }
        verify(exactly = 0) { trygdetidService.slettPeriode(any(), any()) }
    }

    @Test
    @WithMockUser(roles = ["SAKSBEHANDLER"])
    fun `saksbehandler med tilgang får hente trygdetid`() {
        every { trygdetidService.hentPerioder(behandlingId) } returns emptyList()

        mockMvc.get(url).andExpect { status { isOk() } }
    }

    @Test
    @WithMockUser(roles = ["SAKSBEHANDLER"])
    fun `saksbehandler med tilgang får lagre trygdetid`() {
        every { trygdetidService.lagrePeriode(behandlingId, any()) } returns
            Trygdetid(behandlingId = behandlingId, teller = 40, kilde = TrygdetidKilde.ALDERSPENSJON)

        postTrygdetid().andExpect { status { isOk() } }
    }

    @Test
    @WithMockUser(roles = ["SAKSBEHANDLER"])
    fun `saksbehandler med tilgang får slette trygdetid`() {
        justRun { trygdetidService.slettPeriode(behandlingId, trygdetidId) }

        mockMvc.delete(slettUrl) { with(csrf()) }.andExpect { status { isNoContent() } }
    }

    private fun postTrygdetid(): ResultActionsDsl =
        mockMvc.post(url) {
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = requestJson
        }
}
