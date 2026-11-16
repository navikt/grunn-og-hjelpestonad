package no.nav.grunn.og.hjelpestonad.oppdrag

import io.micrometer.core.instrument.MeterRegistry
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Sender oppdraget for en behandling til OS gjennom `familie-oppdrag-backend`.
 *
 * Oppdraget lages, kjedes og lagres først (se [UtbetalingsoppdragService.lagOgLagreUtbetalingsoppdrag]), og sendes
 * etterpå. Kvitteringen fra OS kommer asynkront og hentes med [hentStatus], fra [HentOppdragStatusTask] eller
 * testendepunktet.
 */
@Service
class OppdragIverksettingService(
    private val utbetalingsoppdragService: UtbetalingsoppdragService,
    private val oppdragClient: OppdragClient,
    private val oppdragResultatRepository: OppdragResultatRepository,
    private val meterRegistry: MeterRegistry,
) {
    private val logger = LoggerFactory.getLogger(OppdragIverksettingService::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    /**
     * @param saksbehandlerId identen som står i oppdraget: saksbehandleren som sendte til beslutter, eller innlogget
     * bruker fra testendepunktet.
     */
    fun iverksett(
        behandlingId: UUID,
        saksbehandlerId: String,
    ): Iverksettingsresultat {
        val lagretUtbetalingsoppdrag = utbetalingsoppdragService.lagOgLagreUtbetalingsoppdrag(behandlingId, saksbehandlerId)
        if (lagretUtbetalingsoppdrag == null) {
            logger.info("Ingenting å sende til oppdrag for behandling={}", behandlingId)
            tellIverksetting("ingenting_aa_sende")
            return Iverksettingsresultat.IngentingÅSende
        }

        val oppdragResultat = lagretUtbetalingsoppdrag.oppdragResultat
        if (oppdragResultat.status == OppdragStatus.INGEN_ENDRING) {
            logger.info("Behandling={} endrer ikke utbetalingen, sender ikke oppdrag", behandlingId)
            tellIverksetting("ingen_endring")
            return Iverksettingsresultat.IngenEndring(oppdragResultat = oppdragResultat, testandel = lagretUtbetalingsoppdrag.testandel)
        }

        val iverksettUtfall =
            runCatching { oppdragClient.iverksett(oppdragResultat.utbetalingsoppdrag) }
                .onFailure { tellIverksetting("feilet") }
                .getOrThrow()
        tellIverksetting(if (iverksettUtfall == IverksettUtfall.ALLEREDE_SENDT) "allerede_sendt" else "ok")

        if (oppdragResultat.status == OppdragStatus.OPPRETTET) {
            oppdragResultatRepository.oppdaterStatus(behandlingId, OppdragStatus.LAGT_PÅ_KØ, kvitteringsmelding = null)
        }
        return Iverksettingsresultat.Sendt(oppdragResultat = oppdragResultat, testandel = lagretUtbetalingsoppdrag.testandel)
    }

    /**
     * Henter status fra `familie-oppdrag-backend` og lagrer den på oppdraget, hvis vi har lagret oppdraget.
     *
     * `familie-oppdrag-backend` finner oppdraget med identen det ble sendt med. Vi bruker derfor `aktoer` fra det
     * lagrede oppdraget, slik at et identbytte etter sendingen ikke gir 404.
     */
    fun hentStatus(behandlingId: UUID): OppdragStatusSvar {
        val lagretOppdrag = oppdragResultatRepository.findByIdOrNull(behandlingId)
        val lagretOppdragId = lagretOppdrag?.utbetalingsoppdrag?.tilOppdragId()
        val oppdragId = lagretOppdragId ?: utbetalingsoppdragService.lagOppdragId(behandlingId)
        val svar = oppdragClient.hentStatus(oppdragId)
        if (lagretOppdrag != null) {
            oppdragResultatRepository.oppdaterStatus(behandlingId, svar.status, svar.melding)
        }
        if (svar.melding != null) {
            secureLogger.info("Kvitteringsmelding for behandling={}: {}", behandlingId, svar.melding)
        }
        return svar
    }

    private fun tellIverksetting(resultat: String) = meterRegistry.counter("oppdrag_iverksetting", "resultat", resultat).increment()
}

/** `null` når oppdraget ikke har perioder, og derfor ikke er sendt. */
private fun Utbetalingsoppdrag.tilOppdragId(): OppdragId? =
    utbetalingsperiode.firstOrNull()?.let {
        OppdragId(fagsystem = fagSystem, personIdent = aktoer, behandlingsId = it.behandlingId.toString())
    }

sealed interface Iverksettingsresultat {
    /** Behandlingen har ingen andeler å utbetale, og fagsaken har ingen kjede å opphøre. Ingenting er lagret eller sendt. */
    data object IngentingÅSende : Iverksettingsresultat

    /** Generatoren fant ingen endring mot forrige behandling. Oppdraget er lagret med status `INGEN_ENDRING`, men ikke sendt. */
    data class IngenEndring(
        val oppdragResultat: OppdragResultat,
        val testandel: Testandel?,
    ) : Iverksettingsresultat

    /** Oppdraget er sendt til OS, eller var sendt fra før (409 fra `familie-oppdrag-backend`). Kvitteringen kommer asynkront. */
    data class Sendt(
        val oppdragResultat: OppdragResultat,
        val testandel: Testandel?,
    ) : Iverksettingsresultat
}

data class OppdragIverksettingResponse(
    val saksnummer: String,
    /** Behandlings-ID-en oppdraget er lagret med i `familie-oppdrag-backend` (vår eksterne behandlings-ID). */
    val behandlingsId: String,
    val antallPerioder: Int,
    /** Satt når behandlingen manglet tilkjent ytelse og vi iverksatte med en testandel. */
    val testandel: Testandel?,
)
