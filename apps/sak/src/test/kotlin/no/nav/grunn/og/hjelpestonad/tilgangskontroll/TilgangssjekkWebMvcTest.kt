package no.nav.grunn.og.hjelpestonad.tilgangskontroll

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.ApplicationLocalSetup
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingController
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.behandling.henleggBehandling.HenleggBehandlingService
import no.nav.grunn.og.hjelpestonad.behandling.oppretteBehandling.OpprettBehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakController
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakService
import no.nav.grunn.og.hjelpestonad.pdl.PdlController
import no.nav.grunn.og.hjelpestonad.pdl.PdlService
import no.nav.grunn.og.hjelpestonad.vedtak.GjeldendeVedtakService
import no.nav.grunn.og.hjelpestonad.vedtak.VedtakController
import no.nav.grunn.og.hjelpestonad.vedtak.VedtakService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.util.UUID

/**
 * Én test per type ID som peker på en person: avslag fra TilgangService gir 403 med generell
 * melding, og tjenesten bak blir aldri kalt. Se ADR-0009.
 */
@WebMvcTest(
    controllers = [
        VedtakController::class,
        PdlController::class,
        FagsakController::class,
        BehandlingController::class,
    ],
)
@ContextConfiguration(classes = [ApplicationLocalSetup::class])
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("integrasjonstest")
class TilgangssjekkWebMvcTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var tilgangService: TilgangService

    @MockkBean
    private lateinit var vedtakService: VedtakService

    @MockkBean
    private lateinit var gjeldendeVedtakService: GjeldendeVedtakService

    @MockkBean
    private lateinit var pdlService: PdlService

    @MockkBean
    private lateinit var fagsakService: FagsakService

    @MockkBean
    private lateinit var behandlingService: BehandlingService

    @MockkBean
    private lateinit var opprettBehandlingService: OpprettBehandlingService

    @MockkBean
    private lateinit var henleggBehandlingService: HenleggBehandlingService

    @MockkBean
    private lateinit var endringshistorikkService: EndringshistorikkService

    private val avslag = ManglerTilgangTilPersonFeil(avvisningskode = null)

    @Test
    fun `behandlingId i stien gir 403 ved avslag`() {
        every { tilgangService.validerTilgangTilBehandling(any()) } throws avslag

        mockMvc.get("/api/vedtak/${UUID.randomUUID()}/hent-vedtak").forventAvslag()

        verify(exactly = 0) { vedtakService.hentVedtak(any()) }
    }

    @Test
    fun `fagsakId gir 403 ved avslag`() {
        every { tilgangService.validerTilgangTilFagsak(any()) } throws avslag

        mockMvc.postJson("/api/behandling/hentBehandlinger", """{"fagsakId":"${UUID.randomUUID()}"}""").forventAvslag()

        verify(exactly = 0) { behandlingService.hentBehandlingerFraFagsak(any()) }
    }

    @Test
    fun `fagsakPersonId gir 403 ved avslag`() {
        every { tilgangService.validerTilgangTilFagsakPerson(any()) } throws avslag

        mockMvc.postJson("/api/pdl/person", """{"fagsakPersonId":"${UUID.randomUUID()}"}""").forventAvslag()

        verify(exactly = 0) { pdlService.hentPersonMedFagsakPersonId(any()) }
    }

    @Test
    fun `personident gir 403 ved avslag`() {
        every { tilgangService.validerTilgangTilPerson(any()) } throws avslag

        mockMvc
            .postJson("/api/fagsak", """{"personident":"12345678901","stønadstype":"BARNETILSYN"}""")
            .forventAvslag()

        verify(exactly = 0) { fagsakService.hentEllerOpprettFagsak(any(), any()) }
    }

    private fun MockMvc.postJson(
        url: String,
        body: String,
    ) = post(url) {
        contentType = MediaType.APPLICATION_JSON
        content = body
    }

    private fun ResultActionsDsl.forventAvslag() {
        andExpect {
            status { isForbidden() }
            jsonPath("$.melding") { value(TilgangService.MANGLER_TILGANG_MELDING) }
        }
    }
}
