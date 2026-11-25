package no.nav.grunn.og.hjelpestonad.oppdrag

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.ApplicationLocalSetup
import no.nav.grunn.og.hjelpestonad.oppdrag.simulering.SimuleringOppsummerer
import no.nav.grunn.og.hjelpestonad.oppdrag.simulering.SimuleringResponse
import no.nav.grunn.og.hjelpestonad.oppdrag.simulering.SimuleringsstatusResponse
import no.nav.grunn.og.hjelpestonad.tilgangskontroll.ManglerTilgangTilPersonFeil
import no.nav.grunn.og.hjelpestonad.tilgangskontroll.TilgangService
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Roller og tilgang for simuleringen. Alle med lesetilgang kan hente den lagrede simuleringen, men bare saksbehandler
 * kan simulere på nytt. Hver Azure-gruppe gir én rolle, så en beslutter har ikke `SAKSBEHANDLER`.
 */
@WebMvcTest(OppdragSimuleringController::class)
@ContextConfiguration(classes = [ApplicationLocalSetup::class])
@Import(OppdragSimuleringControllerTest.MetodesikkerhetConfig::class)
@AutoConfigureMockMvc
@ActiveProfiles("integrasjonstest")
class OppdragSimuleringControllerTest {
    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    open class MetodesikkerhetConfig

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var oppdragSimuleringService: OppdragSimuleringService

    @MockkBean(relaxed = true)
    private lateinit var tilgangService: TilgangService

    private val behandlingId = UUID.randomUUID()
    private val simuleringUrl = "/api/oppdrag/simulering/$behandlingId"

    private val simuleringResponse =
        SimuleringResponse(
            status = SimuleringsstatusResponse.SIMULERT,
            simulertTidspunkt = LocalDateTime.of(2026, 11, 19, 10, 0),
            oppsummering =
                SimuleringOppsummerer.oppsummer(
                    DetaljertSimuleringResultat(
                        simuleringMottaker =
                            listOf(
                                SimuleringMottaker(
                                    simulertPostering =
                                        listOf(
                                            SimulertPostering(
                                                fagOmrådeKode = FagOmrådeKode.GRUNNSTØNAD_INFOTRYGD.name,
                                                fom = LocalDate.of(2026, 10, 1),
                                                tom = LocalDate.of(2026, 10, 31),
                                                betalingType = "DEBIT",
                                                beløp = BigDecimal.valueOf(752),
                                                posteringType = "YTELSE",
                                                forfallsdato = LocalDate.of(2026, 10, 20),
                                            ),
                                        ),
                                    mottakerNummer = "12345678910",
                                    mottakerType = "BRUKER",
                                ),
                            ),
                    ),
                    simuleringHentetDato = LocalDate.of(2026, 11, 19),
                ),
        )

    @Test
    @WithMockUser(roles = ["SAKSBEHANDLER"])
    fun `saksbehandler kan simulere på nytt, og svaret har ikke fødselsnummeret`() {
        every { oppdragSimuleringService.simulerPåNytt(behandlingId) } returns simuleringResponse

        mockMvc.post(simuleringUrl) { with(csrf()) }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("SIMULERT") }
            jsonPath("$.oppsummering.etterbetaling") { value(752) }
            jsonPath("$.oppsummering.perioder[0].beløpPerMåned.nyttBeløp") { value(752) }
            jsonPath("$.oppsummering.perioder[0].måneder") { doesNotExist() }
            content { string(not(containsString("12345678910"))) }
            content { string(not(containsString("mottakerNummer"))) }
        }

        verify(exactly = 1) { tilgangService.validerTilgangTilBehandling(behandlingId) }
        verify(exactly = 1) { oppdragSimuleringService.simulerPåNytt(behandlingId) }
    }

    @Test
    @WithMockUser(roles = ["BESLUTTER"])
    fun `beslutter kan ikke simulere på nytt`() {
        mockMvc.post(simuleringUrl) { with(csrf()) }.andExpect { status { isForbidden() } }

        verify(exactly = 0) { oppdragSimuleringService.simulerPåNytt(any()) }
    }

    @Test
    @WithMockUser(roles = ["LESETILGANG"])
    fun `bruker med lesetilgang kan ikke simulere på nytt`() {
        mockMvc.post(simuleringUrl) { with(csrf()) }.andExpect { status { isForbidden() } }

        verify(exactly = 0) { oppdragSimuleringService.simulerPåNytt(any()) }
    }

    @Test
    @WithMockUser(roles = ["SAKSBEHANDLER"])
    fun `avslag fra tilgangssjekk gir 403 når saksbehandler simulerer, og servicen kalles ikke`() {
        every { tilgangService.validerTilgangTilBehandling(behandlingId) } throws ManglerTilgangTilPersonFeil(avvisningskode = null)

        mockMvc.post(simuleringUrl) { with(csrf()) }.andExpect { status { isForbidden() } }

        verify(exactly = 0) { oppdragSimuleringService.simulerPåNytt(any()) }
    }

    @Test
    @WithMockUser(roles = ["SAKSBEHANDLER"])
    fun `saksbehandler kan hente den lagrede simuleringen`() {
        hentOgForventOk()
    }

    @Test
    @WithMockUser(roles = ["BESLUTTER"])
    fun `beslutter kan hente den lagrede simuleringen`() {
        hentOgForventOk()
    }

    @Test
    @WithMockUser(roles = ["LESETILGANG"])
    fun `bruker med lesetilgang kan hente den lagrede simuleringen`() {
        hentOgForventOk()
    }

    @Test
    @WithMockUser
    fun `bruker uten rolle får 403 på henting, og servicen kalles ikke`() {
        mockMvc.get(simuleringUrl).andExpect { status { isForbidden() } }

        verify(exactly = 0) { tilgangService.validerTilgangTilBehandling(any()) }
        verify(exactly = 0) { oppdragSimuleringService.hentSimulering(any()) }
    }

    @Test
    @WithMockUser(roles = ["BESLUTTER"])
    fun `avslag fra tilgangssjekk gir 403 på henting, og servicen kalles ikke`() {
        every { tilgangService.validerTilgangTilBehandling(behandlingId) } throws ManglerTilgangTilPersonFeil(avvisningskode = null)

        mockMvc.get(simuleringUrl).andExpect { status { isForbidden() } }

        verify(exactly = 0) { oppdragSimuleringService.hentSimulering(any()) }
    }

    private fun hentOgForventOk(): ResultActionsDsl {
        every { oppdragSimuleringService.hentSimulering(behandlingId) } returns simuleringResponse

        val resultActions =
            mockMvc.get(simuleringUrl).andExpect {
                status { isOk() }
                jsonPath("$.status") { value("SIMULERT") }
            }

        verify(exactly = 1) { tilgangService.validerTilgangTilBehandling(behandlingId) }
        verify(exactly = 1) { oppdragSimuleringService.hentSimulering(behandlingId) }
        verify(exactly = 0) { oppdragSimuleringService.simulerPåNytt(any()) }
        return resultActions
    }
}
