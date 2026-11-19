package no.nav.grunn.og.hjelpestonad.oppdrag

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingSteg
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingStegService
import no.nav.grunn.og.hjelpestonad.felles.sikkerhet.SikkerhetContext
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.util.UUID

class OppdragSimuleringServiceTest {
    private val behandlingStegService = mockk<BehandlingStegService>()
    private val utbetalingsoppdragService = mockk<UtbetalingsoppdragService>()
    private val oppdragClient = mockk<OppdragClient>()
    private val service = OppdragSimuleringService(behandlingStegService, utbetalingsoppdragService, oppdragClient, SimpleMeterRegistry())

    private val behandlingId = UUID.randomUUID()
    private val innloggetSaksbehandlerIdent = "Z999999"
    private val utbetalingsoppdrag = mockk<Utbetalingsoppdrag>()
    private val detaljertSimuleringResultat = mockk<DetaljertSimuleringResultat>(relaxed = true)

    @BeforeEach
    fun setup() {
        mockkObject(SikkerhetContext)
        every { SikkerhetContext.hentSaksbehandler() } returns innloggetSaksbehandlerIdent
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SikkerhetContext)
    }

    @Test
    fun `kaster feilen og simulerer ikke når steget VEDTAK_OG_BREV ikke er nådd`() {
        val feil = Feil(melding = "Fullfør «Sats og trygdetid» før du går videre.", httpStatus = HttpStatus.BAD_REQUEST)
        every { behandlingStegService.validerHarNåddStegHvisRedigerbar(behandlingId, BehandlingSteg.VEDTAK_OG_BREV) } throws feil

        assertThatThrownBy { service.simuler(behandlingId) }.isSameAs(feil)
        verify(exactly = 0) { utbetalingsoppdragService.lagUtbetalingsoppdrag(any(), any()) }
        verify(exactly = 0) { oppdragClient.simuler(any()) }
    }

    @Test
    fun `validerer steget før oppdraget lages og simuleres, og returnerer resultatet fra klienten`() {
        every { behandlingStegService.validerHarNåddStegHvisRedigerbar(behandlingId, BehandlingSteg.VEDTAK_OG_BREV) } just runs
        every { utbetalingsoppdragService.lagUtbetalingsoppdrag(behandlingId, innloggetSaksbehandlerIdent) } returns
            UtbetalingsoppdragForBehandling(utbetalingsoppdrag = utbetalingsoppdrag, testandel = null)
        every { oppdragClient.simuler(utbetalingsoppdrag) } returns detaljertSimuleringResultat

        val oppdragSimuleringResponse = service.simuler(behandlingId)

        assertThat(oppdragSimuleringResponse).isEqualTo(OppdragSimuleringResponse(resultat = detaljertSimuleringResultat, testandel = null))
        verifyOrder {
            behandlingStegService.validerHarNåddStegHvisRedigerbar(behandlingId, BehandlingSteg.VEDTAK_OG_BREV)
            utbetalingsoppdragService.lagUtbetalingsoppdrag(behandlingId, innloggetSaksbehandlerIdent)
            oppdragClient.simuler(utbetalingsoppdrag)
        }
    }
}
