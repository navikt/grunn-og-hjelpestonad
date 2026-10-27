package no.nav.grunn.og.hjelpestonad.infotrygd

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.ApplicationLocalSetup
import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.StonaderRequest
import no.nav.grunn.og.hjelpestonad.tilgangskontroll.ManglerTilgangTilPersonFeil
import no.nav.grunn.og.hjelpestonad.tilgangskontroll.TilgangService
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.post

/**
 * Rolle og tilgang for Infotrygd-endepunktene. Rollesjekken krever at filtrene og method security er på,
 * derfor samme oppsett som FagsakControllerRolleTest. Tilgangsavslag gir 403 og tjenesten blir ikke kalt.
 */
@WebMvcTest(InfotrygdController::class)
@ContextConfiguration(classes = [ApplicationLocalSetup::class])
@Import(InfotrygdControllerTest.MetodesikkerhetConfig::class)
@AutoConfigureMockMvc
@ActiveProfiles("integrasjonstest")
class InfotrygdControllerTest {
    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    open class MetodesikkerhetConfig

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var infotrygdService: InfotrygdService

    @MockkBean(relaxed = true)
    private lateinit var tilgangService: TilgangService

    private val personident = "12345678901"
    private val stonaderUrl = "/api/infotrygd/stonader"
    private val gjeldendePerioderUrl = "/api/infotrygd/gjeldende-perioder"

    private val stønaderResponse =
        InfotrygdArv
            .fyllVedtakMedVerdierFraTidligereVedtak(
                listOf(
                    InfotrygdTestdata.stønad(
                        InfotrygdTestdata.vedtak(1L, evaluering = InfotrygdTestdata.evaluering()),
                        InfotrygdTestdata.vedtak(2L),
                    ),
                ),
            ).tilInfotrygdStonaderResponse()

    private val gjeldendePerioderResponse =
        GjeldendeInfotrygdPerioder
            .utled(
                InfotrygdArv.fyllVedtakMedVerdierFraTidligereVedtak(
                    listOf(InfotrygdTestdata.stønad(InfotrygdTestdata.vedtak(1L, evaluering = InfotrygdTestdata.evaluering()))),
                ),
            ).tilInfotrygdGjeldendePerioderResponse()

    @Test
    @WithMockUser(roles = ["SAKSBEHANDLER"])
    fun `saksbehandler med tilgang får 200 og no-store på stønader`() {
        every {
            infotrygdService.hentStønader(personident, StonaderRequest.Stønadstype.GRUNNSTØNAD)
        } returns stønaderResponse

        postJson(stonaderUrl).andExpect {
            status { isOk() }
            header { string(HttpHeaders.CACHE_CONTROL, containsString("no-store")) }
            jsonPath("$.stonader[0].vedtak[0].vedtakId") { value(1) }
            jsonPath("$.stonader[0].vedtak[1].evaluering.arvetFraVedtak.vedtakId") { value(1) }
        }

        verify(exactly = 1) { tilgangService.validerTilgangTilPerson(personident) }
        verify(exactly = 1) { infotrygdService.hentStønader(personident, StonaderRequest.Stønadstype.GRUNNSTØNAD) }
    }

    @Test
    @WithMockUser
    fun `bruker uten rolle får 403 på stønader, og tjenesten kalles ikke`() {
        postJson(stonaderUrl).andExpect { status { isForbidden() } }

        verify(exactly = 0) { tilgangService.validerTilgangTilPerson(any()) }
        verify(exactly = 0) { infotrygdService.hentStønader(any(), any()) }
    }

    @Test
    @WithMockUser(roles = ["SAKSBEHANDLER"])
    fun `avslag fra tilgangssjekk gir 403 på stønader, og tjenesten kalles ikke`() {
        every { tilgangService.validerTilgangTilPerson(personident) } throws ManglerTilgangTilPersonFeil(avvisningskode = null)

        postJson(stonaderUrl).andExpect {
            status { isForbidden() }
            jsonPath("$.melding") { value(TilgangService.MANGLER_TILGANG_MELDING) }
        }

        verify(exactly = 0) { infotrygdService.hentStønader(any(), any()) }
    }

    @Test
    @WithMockUser(roles = ["SAKSBEHANDLER"])
    fun `saksbehandler med tilgang får 200 og no-store på gjeldende perioder`() {
        every {
            infotrygdService.hentGjeldendePerioder(personident, StonaderRequest.Stønadstype.GRUNNSTØNAD)
        } returns gjeldendePerioderResponse

        postJson(gjeldendePerioderUrl).andExpect {
            status { isOk() }
            header { string(HttpHeaders.CACHE_CONTROL, containsString("no-store")) }
            jsonPath("$.perioder[0].fom") { value("2020-01-01") }
            jsonPath("$.perioder[0].stonadId") { value(1) }
            jsonPath("$.perioder[0].vedtak.vedtakId") { value(1) }
        }

        verify(exactly = 1) { tilgangService.validerTilgangTilPerson(personident) }
        verify(exactly = 1) { infotrygdService.hentGjeldendePerioder(personident, StonaderRequest.Stønadstype.GRUNNSTØNAD) }
    }

    @Test
    @WithMockUser
    fun `bruker uten rolle får 403 på gjeldende perioder, og tjenesten kalles ikke`() {
        postJson(gjeldendePerioderUrl).andExpect { status { isForbidden() } }

        verify(exactly = 0) { tilgangService.validerTilgangTilPerson(any()) }
        verify(exactly = 0) { infotrygdService.hentGjeldendePerioder(any(), any()) }
    }

    @Test
    @WithMockUser(roles = ["SAKSBEHANDLER"])
    fun `avslag fra tilgangssjekk gir 403 på gjeldende perioder, og tjenesten kalles ikke`() {
        every { tilgangService.validerTilgangTilPerson(personident) } throws ManglerTilgangTilPersonFeil(avvisningskode = null)

        postJson(gjeldendePerioderUrl).andExpect {
            status { isForbidden() }
            jsonPath("$.melding") { value(TilgangService.MANGLER_TILGANG_MELDING) }
        }

        verify(exactly = 0) { infotrygdService.hentGjeldendePerioder(any(), any()) }
    }

    private fun postJson(url: String): ResultActionsDsl =
        mockMvc.post(url) {
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = """{"personident":"$personident","stønadstype":"GRUNNSTØNAD"}"""
        }
}
