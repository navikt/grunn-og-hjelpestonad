package no.nav.grunn.og.hjelpestonad.oppdrag

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Sender oppdraget for en behandling til OS gjennom `familie-oppdrag-backend`.
 *
 * Første skive for å teste integrasjonen i dev: synkront kall med saksbehandlerens OBO-token, ingen lagring av
 * oppdraget og ingen kjeding. Statusen hentes manuelt med [hentStatus]. Den ekte iverksettingen bør gå asynkront
 * med client credentials og polling av status, slik familie-ef-iverksett gjør.
 */
@Service
class OppdragIverksettingService(
    private val utbetalingsoppdragService: UtbetalingsoppdragService,
    private val oppdragClient: OppdragClient,
    private val meterRegistry: MeterRegistry,
) {
    fun iverksett(behandlingId: UUID): OppdragIverksettingResponse {
        val (utbetalingsoppdrag, testandel) = utbetalingsoppdragService.lagUtbetalingsoppdrag(behandlingId)

        runCatching { oppdragClient.iverksett(utbetalingsoppdrag) }
            .onSuccess { tellIverksetting("ok") }
            .onFailure { tellIverksetting("feilet") }
            .getOrThrow()

        return OppdragIverksettingResponse(
            saksnummer = utbetalingsoppdrag.saksnummer,
            behandlingsId =
                utbetalingsoppdrag.utbetalingsperiode
                    .first()
                    .behandlingId
                    .toString(),
            antallPerioder = utbetalingsoppdrag.utbetalingsperiode.size,
            testandel = testandel,
        )
    }

    fun hentStatus(behandlingId: UUID): OppdragStatusSvar = oppdragClient.hentStatus(utbetalingsoppdragService.lagOppdragId(behandlingId))

    private fun tellIverksetting(resultat: String) = meterRegistry.counter("oppdrag_iverksetting", "resultat", resultat).increment()
}

data class OppdragIverksettingResponse(
    val saksnummer: String,
    /** Behandlings-ID-en oppdraget er lagret med i `familie-oppdrag-backend` (vår eksterne behandlings-ID). */
    val behandlingsId: String,
    val antallPerioder: Int,
    /** Satt når behandlingen manglet tilkjent ytelse og vi iverksatte med en testandel. */
    val testandel: Testandel?,
)
