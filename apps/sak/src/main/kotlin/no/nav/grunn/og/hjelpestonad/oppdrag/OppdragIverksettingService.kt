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
    private val oppdragRepository: OppdragRepository,
    private val meterRegistry: MeterRegistry,
) {
    private val logger = LoggerFactory.getLogger(OppdragIverksettingService::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    /**
     * @param saksbehandlerId identen som står i oppdraget: saksbehandleren som sendte til beslutter, eller innlogget
     * bruker fra testendepunktet.
     * @return `null` når behandlingen ikke har noe å utbetale og fagsaken ingen kjede å opphøre.
     */
    fun iverksett(
        behandlingId: UUID,
        saksbehandlerId: String,
    ): Iverksettingsresultat? {
        val lagret = utbetalingsoppdragService.lagOgLagreUtbetalingsoppdrag(behandlingId, saksbehandlerId)
        if (lagret == null) {
            logger.info("Ingenting å sende til oppdrag for behandling={}", behandlingId)
            tellIverksetting("ingenting_aa_sende")
            return null
        }

        val oppdrag = lagret.oppdragResultat
        if (oppdrag.status == OppdragStatus.INGEN_ENDRING) {
            logger.info("Behandling={} endrer ikke utbetalingen, sender ikke oppdrag", behandlingId)
            tellIverksetting("ingen_endring")
            return Iverksettingsresultat(oppdrag = oppdrag, testandel = lagret.testandel, sendt = false)
        }

        val utfall =
            runCatching { oppdragClient.iverksett(oppdrag.utbetalingsoppdrag) }
                .onFailure { tellIverksetting("feilet") }
                .getOrThrow()
        tellIverksetting(if (utfall == IverksettUtfall.ALLEREDE_SENDT) "allerede_sendt" else "ok")

        if (oppdrag.status == OppdragStatus.OPPRETTET) {
            oppdragRepository.oppdaterStatus(behandlingId, OppdragStatus.LAGT_PÅ_KØ, kvitteringsmelding = null)
        }
        return Iverksettingsresultat(oppdrag = oppdrag, testandel = lagret.testandel, sendt = true)
    }

    /**
     * Henter status fra `familie-oppdrag-backend` og lagrer den på oppdraget, hvis vi har lagret oppdraget.
     *
     * `familie-oppdrag-backend` finner oppdraget med identen det ble sendt med. Vi bruker derfor `aktoer` fra det
     * lagrede oppdraget, slik at et identbytte etter sendingen ikke gir 404.
     */
    fun hentStatus(behandlingId: UUID): OppdragStatusSvar {
        val lagret = oppdragRepository.findByIdOrNull(behandlingId)
        val oppdragId = lagret?.utbetalingsoppdrag?.tilOppdragId() ?: utbetalingsoppdragService.lagOppdragId(behandlingId)
        val svar = oppdragClient.hentStatus(oppdragId)
        if (lagret != null) {
            oppdragRepository.oppdaterStatus(behandlingId, svar.status, svar.melding)
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

/** @param sendt usann når generatoren ikke fant noen endring, og ingenting ble sendt. */
data class Iverksettingsresultat(
    val oppdrag: OppdragResultat,
    val testandel: Testandel?,
    val sendt: Boolean,
)

data class OppdragIverksettingResponse(
    val saksnummer: String,
    /** Behandlings-ID-en oppdraget er lagret med i `familie-oppdrag-backend` (vår eksterne behandlings-ID). */
    val behandlingsId: String,
    val antallPerioder: Int,
    /** Satt når behandlingen manglet tilkjent ytelse og vi iverksatte med en testandel. */
    val testandel: Testandel?,
)
