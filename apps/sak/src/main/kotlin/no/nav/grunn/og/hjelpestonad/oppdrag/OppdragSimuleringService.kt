package no.nav.grunn.og.hjelpestonad.oppdrag

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service
import java.util.UUID

/** Simulerer en behandling mot oppdrag gjennom `familie-oppdrag-backend`. Resultatet lagres ikke (se ADR-0011). */
@Service
class OppdragSimuleringService(
    private val utbetalingsoppdragService: UtbetalingsoppdragService,
    private val oppdragClient: OppdragClient,
    private val meterRegistry: MeterRegistry,
) {
    fun simuler(behandlingId: UUID): OppdragSimuleringResponse {
        val utbetalingsoppdragForBehandling = utbetalingsoppdragService.lagUtbetalingsoppdrag(behandlingId)
        val utbetalingsoppdrag = utbetalingsoppdragForBehandling.utbetalingsoppdrag
        val testandel = utbetalingsoppdragForBehandling.testandel

        val resultat =
            runCatching { oppdragClient.simuler(utbetalingsoppdrag) }
                .onSuccess { tellSimulering("ok") }
                .onFailure { tellSimulering("feilet") }
                .getOrThrow()

        return OppdragSimuleringResponse(resultat = resultat, testandel = testandel)
    }

    private fun tellSimulering(resultat: String) = meterRegistry.counter("oppdrag_simulering", "resultat", resultat).increment()
}

data class OppdragSimuleringResponse(
    val resultat: DetaljertSimuleringResultat,
    /** Satt når behandlingen manglet tilkjent ytelse og vi simulerte med en testandel. */
    val testandel: Testandel?,
)
