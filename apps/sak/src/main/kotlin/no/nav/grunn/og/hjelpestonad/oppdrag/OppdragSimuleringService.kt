package no.nav.grunn.og.hjelpestonad.oppdrag

import io.micrometer.core.instrument.MeterRegistry
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingSteg
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingStegService
import no.nav.grunn.og.hjelpestonad.felles.sikkerhet.SikkerhetContext
import no.nav.grunn.og.hjelpestonad.oppdrag.simulering.LagretSimulering
import no.nav.grunn.og.hjelpestonad.oppdrag.simulering.SimuleringOppsummerer
import no.nav.grunn.og.hjelpestonad.oppdrag.simulering.SimuleringRepository
import no.nav.grunn.og.hjelpestonad.oppdrag.simulering.SimuleringResponse
import no.nav.grunn.og.hjelpestonad.oppdrag.simulering.Simuleringsoppsummering
import no.nav.grunn.og.hjelpestonad.oppdrag.simulering.Simuleringsstatus
import no.nav.grunn.og.hjelpestonad.oppdrag.simulering.SimuleringsstatusResponse
import no.nav.grunn.og.hjelpestonad.oppdrag.simulering.tilSimuleringsstatusResponse
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.TilkjentYtelseService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Simulerer en behandling mot oppdrag gjennom `familie-oppdrag-backend`, kjedet mot det som er sendt før.
 * Svaret fra Oppdrag lagres uendret i `simulering` (ADR-0018). [hentSimulering] oppsummerer det lagrede svaret.
 * Simulering ved knapp krever ansvarlig saksbehandler og redigerbar behandling på steget VEDTAK_OG_BREV.
 */
@Service
class OppdragSimuleringService(
    private val behandlingStegService: BehandlingStegService,
    private val ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
    private val utbetalingsoppdragService: UtbetalingsoppdragService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val oppdragClient: OppdragClient,
    private val simuleringRepository: SimuleringRepository,
    private val objectMapper: ObjectMapper,
    private val meterRegistry: MeterRegistry,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val logger = LoggerFactory.getLogger(OppdragSimuleringService::class.java)

    fun simulerPåNytt(behandlingId: UUID): SimuleringResponse {
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
        behandlingStegService.validerKanEndreSteg(behandlingId, BehandlingSteg.VEDTAK_OG_BREV)
        simulerOgLagre(behandlingId, Simuleringsutløser.KNAPP)
        return hentSimulering(behandlingId)
    }

    fun simulerEtterNyTilkjentYtelse(behandlingId: UUID) {
        if (tilkjentYtelseService.hentTilkjentYtelse(behandlingId) == null) return
        simulerOgLagre(behandlingId, Simuleringsutløser.TILKJENT_YTELSE)
    }

    fun hentSimulering(behandlingId: UUID): SimuleringResponse {
        val lagretSimulering =
            simuleringRepository.finn(behandlingId)
                ?: return SimuleringResponse(status = SimuleringsstatusResponse.IKKE_SIMULERT, simulertTidspunkt = null, oppsummering = null)
        val lagretRespons = lagretSimulering.respons
        if (lagretSimulering.status == Simuleringsstatus.SIMULERT && lagretRespons != null) {
            return oppsummerLagretSimulering(lagretSimulering, lagretRespons)
        }
        return SimuleringResponse(
            status = lagretSimulering.status.tilSimuleringsstatusResponse(),
            simulertTidspunkt = lagretSimulering.opprettetTid,
            oppsummering = null,
        )
    }

    /** Gir FEILET uten oppsummering hvis det lagrede svaret ikke lenger kan oppsummeres, for eksempel etter en endring i koden. */
    private fun oppsummerLagretSimulering(
        lagretSimulering: LagretSimulering,
        lagretRespons: JsonNode,
    ): SimuleringResponse {
        val simuleringsoppsummering =
            try {
                lagretRespons.oppsummer(simuleringHentetDato = lagretSimulering.opprettetTid.toLocalDate())
            } catch (exception: Exception) {
                // Feilmeldingen og stacktrace kan inneholde fødselsnummer fra svaret fra Oppdrag, så bare feiltypen logges.
                logger.warn(
                    "Lagret simulering for behandling={} kunne ikke oppsummeres, feiltype={}",
                    lagretSimulering.behandlingId,
                    exception::class.simpleName,
                )
                return SimuleringResponse(
                    status = SimuleringsstatusResponse.FEILET,
                    simulertTidspunkt = lagretSimulering.opprettetTid,
                    oppsummering = null,
                )
            }
        return SimuleringResponse(
            status = SimuleringsstatusResponse.SIMULERT,
            simulertTidspunkt = lagretSimulering.opprettetTid,
            oppsummering = simuleringsoppsummering,
        )
    }

    private fun simulerOgLagre(
        behandlingId: UUID,
        utløser: Simuleringsutløser,
    ) {
        val simuleringstidspunkt = LocalDateTime.now(clock)
        val utbetalingsoppdragForBehandling =
            utbetalingsoppdragService.lagUtbetalingsoppdrag(behandlingId, SikkerhetContext.hentSaksbehandler())
        if (utbetalingsoppdragForBehandling == null) {
            simuleringRepository.lagre(behandlingId, Simuleringsstatus.INGEN_ENDRING, respons = null, opprettetTid = simuleringstidspunkt)
            tellSimulering("ingen_endring", utløser)
            return
        }

        val simuleringsrespons =
            try {
                oppdragClient.simuler(utbetalingsoppdragForBehandling.utbetalingsoppdrag)
            } catch (exception: Exception) {
                simuleringRepository.lagre(behandlingId, Simuleringsstatus.FEILET, respons = null, opprettetTid = simuleringstidspunkt)
                tellSimulering("feilet", utløser)
                // Ikke logg message eller stacktrace: de kan inneholde svaret fra Oppdrag, med fødselsnummer.
                logger.warn(
                    "Simulering mot Oppdrag feilet for behandling={}, utløser={}, feiltype={}",
                    behandlingId,
                    utløser,
                    exception::class.simpleName,
                )
                return
            }

        // Svaret lagres uendret. Oppsummeringen regnes ut ved GET, så et svar vi ikke kan tolke, gir ikke feil her.
        simuleringRepository.lagre(behandlingId, Simuleringsstatus.SIMULERT, respons = simuleringsrespons, opprettetTid = simuleringstidspunkt)
        tellSimulering("ok", utløser)
    }

    private fun tellSimulering(
        resultat: String,
        utløser: Simuleringsutløser,
    ) = meterRegistry.counter("oppdrag_simulering", "resultat", resultat, "utloser", utløser.tag).increment()

    private fun JsonNode.oppsummer(
        simuleringHentetDato: LocalDate,
    ): Simuleringsoppsummering =
        SimuleringOppsummerer.oppsummer(
            detaljertSimuleringResultat = objectMapper.treeToValue(this, DetaljertSimuleringResultat::class.java),
            simuleringHentetDato = simuleringHentetDato,
        )
}

/** Hva som startet simuleringen. Brukes som tag `utloser` på telleren `oppdrag_simulering`. */
enum class Simuleringsutløser(
    val tag: String,
) {
    KNAPP("knapp"),
    TILKJENT_YTELSE("tilkjent_ytelse"),
}
