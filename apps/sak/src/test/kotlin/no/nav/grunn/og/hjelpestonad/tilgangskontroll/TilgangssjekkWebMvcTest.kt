package no.nav.grunn.og.hjelpestonad.tilgangskontroll

import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.ApplicationLocalSetup
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingController
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.behandling.henleggBehandling.HenleggBehandlingService
import no.nav.grunn.og.hjelpestonad.behandling.oppretteBehandling.OpprettBehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakController
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakService
import no.nav.grunn.og.hjelpestonad.faktagrunnlag.pdl.PdlFaktagrunnlag
import no.nav.grunn.og.hjelpestonad.faktagrunnlag.pdl.PdlFaktagrunnlagController
import no.nav.grunn.og.hjelpestonad.faktagrunnlag.pdl.PdlFaktagrunnlagService
import no.nav.grunn.og.hjelpestonad.faktagrunnlag.pdl.PdlFalskIdentitet
import no.nav.grunn.og.hjelpestonad.faktagrunnlag.pdl.PdlMetadata
import no.nav.grunn.og.hjelpestonad.pdl.PdlController
import no.nav.grunn.og.hjelpestonad.pdl.PdlService
import no.nav.grunn.og.hjelpestonad.vedtak.GjeldendeVedtakService
import no.nav.grunn.og.hjelpestonad.vedtak.VedtakController
import no.nav.grunn.og.hjelpestonad.vedtak.VedtakService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.test.context.support.WithAnonymousUser
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.LocalDateTime
import java.util.UUID

/**
 * Én test per type ID som peker på en person: avslag fra TilgangService gir 403 med generell
 * melding, og tjenesten bak blir aldri kalt. Se ADR-0009.
 *
 * Filtrene og method security er på, så innlogging (401) og rolle (403) testes også.
 */
@WebMvcTest(
    controllers = [
        VedtakController::class,
        PdlController::class,
        FagsakController::class,
        BehandlingController::class,
        PdlFaktagrunnlagController::class,
    ],
)
@ContextConfiguration(classes = [ApplicationLocalSetup::class])
@Import(TilgangssjekkWebMvcTest.MetodesikkerhetConfig::class)
@AutoConfigureMockMvc
@ActiveProfiles("integrasjonstest")
@WithMockUser(roles = ["SAKSBEHANDLER"])
class TilgangssjekkWebMvcTest {
    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    open class MetodesikkerhetConfig

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

    @MockkBean
    private lateinit var pdlFaktagrunnlagService: PdlFaktagrunnlagService

    private val avslag = ManglerTilgangTilPersonFeil(avvisningskode = null)
    private val behandlingId = UUID.randomUUID()
    private val falskIdentitetUrl = "/api/behandling/$behandlingId/faktagrunnlag/pdl/falsk-identitet"

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
            .postJson("/api/fagsak", """{"personident":"12345678901","stønadstype":"GRUNNSTØNAD"}""")
            .forventAvslag()

        verify(exactly = 0) { fagsakService.hentEllerOpprettFagsak(any(), any()) }
    }

    @Test
    @WithAnonymousUser
    fun `manglende innlogging gir 401 på falsk-identitet, og grunnlaget leses ikke`() {
        mockMvc.get(falskIdentitetUrl).andExpect { status { isUnauthorized() } }

        verify(exactly = 0) { tilgangService.validerTilgangTilBehandling(any()) }
        verify(exactly = 0) { pdlFaktagrunnlagService.hent(any()) }
    }

    @Test
    @WithMockUser
    fun `bruker uten saksbehandlerrolle gir 403 på falsk-identitet, og tilgangen sjekkes ikke`() {
        mockMvc.get(falskIdentitetUrl).andExpect { status { isForbidden() } }

        verify(exactly = 0) { tilgangService.validerTilgangTilBehandling(any()) }
        verify(exactly = 0) { pdlFaktagrunnlagService.hent(any()) }
    }

    @Test
    fun `behandlingId i stien gir 403 ved avslag på falsk-identitet, og grunnlaget leses ikke`() {
        every { tilgangService.validerTilgangTilBehandling(behandlingId) } throws avslag

        mockMvc.get(falskIdentitetUrl).forventAvslag()

        verify(exactly = 0) { pdlFaktagrunnlagService.hent(any()) }
    }

    @Test
    fun `falsk identitet gir 200, harFalskIdentitet true og no-store`() {
        every { tilgangService.validerTilgangTilBehandling(behandlingId) } just Runs
        every { pdlFaktagrunnlagService.hent(behandlingId) } returns grunnlagMedFalskIdentitet(erFalsk = true)

        mockMvc.get(falskIdentitetUrl).andExpect {
            status { isOk() }
            header { string(HttpHeaders.CACHE_CONTROL, "no-store") }
            jsonPath("$.harFalskIdentitet") { value(true) }
        }
    }

    @Test
    fun `ikke falsk identitet gir 200, harFalskIdentitet false og no-store`() {
        every { tilgangService.validerTilgangTilBehandling(behandlingId) } just Runs
        every { pdlFaktagrunnlagService.hent(behandlingId) } returns grunnlagMedFalskIdentitet(erFalsk = false)

        mockMvc.get(falskIdentitetUrl).andExpect {
            status { isOk() }
            header { string(HttpHeaders.CACHE_CONTROL, "no-store") }
            jsonPath("$.harFalskIdentitet") { value(false) }
        }
    }

    @Test
    fun `uten grunnlag gir 200, harFalskIdentitet false og no-store`() {
        every { tilgangService.validerTilgangTilBehandling(behandlingId) } just Runs
        every { pdlFaktagrunnlagService.hent(behandlingId) } returns null

        mockMvc.get(falskIdentitetUrl).andExpect {
            status { isOk() }
            header { string(HttpHeaders.CACHE_CONTROL, "no-store") }
            jsonPath("$.harFalskIdentitet") { value(false) }
        }
    }

    private fun MockMvc.postJson(
        url: String,
        body: String,
    ) = post(url) {
        with(csrf())
        contentType = MediaType.APPLICATION_JSON
        content = body
    }

    private fun ResultActionsDsl.forventAvslag() {
        andExpect {
            status { isForbidden() }
            jsonPath("$.melding") { value(TilgangService.MANGLER_TILGANG_MELDING) }
        }
    }

    // Syntetiske data: ingen fødselsnummer eller navn, bare felt som endepunktet leser.
    private fun grunnlagMedFalskIdentitet(erFalsk: Boolean) =
        PdlFaktagrunnlag(
            hentetTidspunkt = LocalDateTime.of(2026, 11, 25, 9, 12),
            folkeregisterpersonstatus = emptyList(),
            bostedsadresse = emptyList(),
            statsborgerskap = emptyList(),
            opphold = emptyList(),
            innflyttingTilNorge = emptyList(),
            utflyttingFraNorge = emptyList(),
            oppholdsadresse = emptyList(),
            doedsfall = emptyList(),
            falskIdentitet =
                PdlFalskIdentitet(
                    behandlingId = behandlingId,
                    erFalsk = erFalsk,
                    metadata = PdlMetadata(historisk = false, master = "FREG"),
                ),
        )
}
