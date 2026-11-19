package no.nav.grunn.og.hjelpestonad.oppdrag

import io.micrometer.core.instrument.MeterRegistry
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingSteg
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingStegService
import no.nav.grunn.og.hjelpestonad.felles.sikkerhet.SikkerhetContext
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Simulerer en behandling mot oppdrag gjennom `familie-oppdrag-backend`, kjedet mot det som er sendt før. Resultatet lagres ikke (se ADR-0011).
 * Simulering krever at behandlingen har nådd steget VEDTAK_OG_BREV mens den er redigerbar.
 */
@Service
class OppdragSimuleringService(
    private val behandlingStegService: BehandlingStegService,
    private val utbetalingsoppdragService: UtbetalingsoppdragService,
    private val oppdragClient: OppdragClient,
    private val meterRegistry: MeterRegistry,
) {
    fun simuler(behandlingId: UUID): OppdragSimuleringResponse {
        behandlingStegService.validerHarNåddStegHvisRedigerbar(behandlingId, BehandlingSteg.VEDTAK_OG_BREV)
        val utbetalingsoppdragForBehandling = utbetalingsoppdragService.lagUtbetalingsoppdrag(behandlingId, SikkerhetContext.hentSaksbehandler())
        val utbetalingsoppdrag = utbetalingsoppdragForBehandling.utbetalingsoppdrag

        val resultat =
            runCatching { oppdragClient.simuler(utbetalingsoppdrag) }
                .onSuccess { tellSimulering("ok") }
                .onFailure { tellSimulering("feilet") }
                .getOrThrow()

        return OppdragSimuleringResponse(resultat = resultat)
    }

    private fun tellSimulering(resultat: String) = meterRegistry.counter("oppdrag_simulering", "resultat", resultat).increment()
}

data class OppdragSimuleringResponse(
    val resultat: DetaljertSimuleringResultat,
)
