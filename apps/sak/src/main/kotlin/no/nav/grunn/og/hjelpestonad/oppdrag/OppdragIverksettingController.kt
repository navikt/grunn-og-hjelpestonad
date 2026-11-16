package no.nav.grunn.og.hjelpestonad.oppdrag

import no.nav.grunn.og.hjelpestonad.felles.sikkerhet.SikkerhetContext
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.tilgangskontroll.TilgangService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Midlertidig endepunkt for å teste iverksetting mot oppdrag i dev. Finnes bare når
 * `oppdrag.iverksetting.testendepunkt-aktivert=true`, som ikke er satt i prod.
 *
 * Går gjennom samme kjeding og lagring som iverksettingen ved godkjenning, men krever ikke at vedtaket er besluttet.
 * Må fjernes eller kreve godkjent vedtak før prod. Oppretter ingen statustask: status hentes med GET-endepunktet, og
 * neste oppdrag på fagsaken stopper til statusen er `KVITTERT_OK`.
 */
@RestController
@RequestMapping(path = ["/api/oppdrag/iverksetting"])
@PreAuthorize("hasRole('SAKSBEHANDLER')")
@ConditionalOnProperty(name = ["oppdrag.iverksetting.testendepunkt-aktivert"], havingValue = "true")
class OppdragIverksettingController(
    private val tilgangService: TilgangService,
    private val oppdragIverksettingService: OppdragIverksettingService,
    private val utbetalingsoppdragService: UtbetalingsoppdragService,
) {
    @PostMapping("/{behandlingId}")
    fun iverksettMotOppdrag(
        @PathVariable behandlingId: UUID,
    ): OppdragIverksettingResponse {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val resultat =
            oppdragIverksettingService.iverksett(behandlingId, SikkerhetContext.hentSaksbehandler())
                ?: throw Feil("Behandlingen har ingen andeler å utbetale og ingen tidligere utbetaling å opphøre", HttpStatus.BAD_REQUEST)
        return OppdragIverksettingResponse(
            saksnummer = resultat.oppdrag.utbetalingsoppdrag.saksnummer,
            behandlingsId = utbetalingsoppdragService.lagOppdragId(behandlingId).behandlingsId,
            antallPerioder = resultat.oppdrag.utbetalingsoppdrag.utbetalingsperiode.size,
            testandel = resultat.testandel,
        )
    }

    @GetMapping("/{behandlingId}/status")
    fun hentOppdragStatus(
        @PathVariable behandlingId: UUID,
    ): OppdragStatusSvar {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return oppdragIverksettingService.hentStatus(behandlingId)
    }
}
