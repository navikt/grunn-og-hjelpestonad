package no.nav.grunn.og.hjelpestonad.vedtak

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.ApplicationLocalSetup
import no.nav.grunn.og.hjelpestonad.tilgangskontroll.TilgangService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.util.UUID

@WebMvcTest(VedtakController::class)
@ContextConfiguration(classes = [ApplicationLocalSetup::class])
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("integrasjonstest")
class VedtakControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var vedtakService: VedtakService

    @MockkBean(relaxed = true)
    private lateinit var tilgangService: TilgangService

    @Test
    fun `henter lagret vedtak`() {
        val behandlingId = UUID.randomUUID()
        every { vedtakService.hentVedtak(behandlingId) } returns
            Vedtak(behandlingId = behandlingId, resultatType = ResultatType.INNVILGET, saksbehandlerIdent = "Z123456", opprettetAv = "Z123456")

        mockMvc.get("/api/vedtak/$behandlingId/hent-vedtak").andExpect {
            status { isOk() }
            jsonPath("$.resultatType") { value("INNVILGET") }
            jsonPath("$.grunnstønadPerioder") { doesNotExist() }
        }

        verify { tilgangService.validerTilgangTilBehandling(behandlingId) }
    }

    @Test
    fun `manglende vedtak gir tomt svar`() {
        val behandlingId = UUID.randomUUID()
        every { vedtakService.hentVedtak(behandlingId) } returns null

        mockMvc.get("/api/vedtak/$behandlingId/hent-vedtak").andExpect {
            status { isNoContent() }
        }
    }
}
