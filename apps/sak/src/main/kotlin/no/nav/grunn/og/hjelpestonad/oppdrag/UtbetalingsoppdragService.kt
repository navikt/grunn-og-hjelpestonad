package no.nav.grunn.og.hjelpestonad.oppdrag

import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag
import no.nav.grunn.og.hjelpestonad.behandling.Behandling
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakRepository
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakService
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.AndelTilkjentYtelse
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.TilkjentYtelseService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/**
 * Lager utbetalingsoppdraget for en behandling, kjedet mot andelene som allerede er sendt på fagsaken (ADR-0011).
 * Brukes av både simulering og iverksetting.
 */
@Service
class UtbetalingsoppdragService(
    private val behandlingService: BehandlingService,
    private val fagsakRepository: FagsakRepository,
    private val fagsakService: FagsakService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val oppdragResultatRepository: OppdragResultatRepository,
    @Value("\${oppdrag.bruk-testandel-hvis-mangler:false}")
    private val brukTestandelHvisMangler: Boolean,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val logger = LoggerFactory.getLogger(UtbetalingsoppdragService::class.java)

    /** Til simulering. Leser kjeden på fagsaken, men lagrer ingenting. */
    fun lagUtbetalingsoppdrag(
        behandlingId: UUID,
        saksbehandlerId: String,
    ): UtbetalingsoppdragForBehandling {
        val behandling = behandlingService.hentBehandlingEllerKast(behandlingId)
        val beregning =
            beregnUtbetalingsoppdrag(behandling, saksbehandlerId)
                ?: throw Feil("Behandlingen har ingen andeler å utbetale og ingen tidligere utbetaling å opphøre", HttpStatus.BAD_REQUEST)
        if (!beregning.oppdrag.harEndringFraForrigeBehandling) {
            throw Feil("Behandlingen endrer ikke utbetalingen", HttpStatus.BAD_REQUEST)
        }
        return UtbetalingsoppdragForBehandling(utbetalingsoppdrag = beregning.oppdrag.utbetalingsoppdrag, testandel = beregning.testandel)
    }

    /**
     * Til iverksetting. Lager oppdraget og lagrer det med andelene. Transaksjonen er alltid ny, slik at
     * oppdraget er committet før det sendes: et nytt forsøk sender da det samme oppdraget, og lager ikke et nytt.
     *
     * Finnes oppdraget for behandlingen fra før, returneres det lagrede uten ny generering.
     * Returnerer `null` når behandlingen ikke har andeler og fagsaken ikke har noen kjede å opphøre.
     *
     * Stopper hvis forrige oppdrag på fagsaken ikke er kvittert OK: da vet vi ikke om OS har andelene vi ville kjedet
     * mot. Et avvist oppdrag må rettes før fagsaken kan få nye oppdrag.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun lagOgLagreUtbetalingsoppdrag(
        behandlingId: UUID,
        saksbehandlerId: String,
    ): LagretUtbetalingsoppdrag? {
        val behandling = behandlingService.hentBehandlingEllerKast(behandlingId)
        oppdragResultatRepository.findByIdOrNull(behandlingId)?.let {
            logger.info("Oppdrag for behandling={} finnes fra før med status={}, bruker det lagrede", behandlingId, it.status)
            return LagretUtbetalingsoppdrag(oppdragResultat = it, testandel = null)
        }

        val statusPåForrige = oppdragResultatRepository.finnStatusPåSisteOppdrag(behandling.fagsakId, unntattBehandlingId = behandlingId)
        if (statusPåForrige != null && statusPåForrige !in OppdragStatus.FERDIG) {
            throw Feil(
                "Forrige oppdrag på fagsaken har status $statusPåForrige. Det må være kvittert OK før et nytt oppdrag kan sendes.",
                HttpStatus.CONFLICT,
            )
        }

        val beregning = beregnUtbetalingsoppdrag(behandling, saksbehandlerId) ?: return null
        val oppdragResultat =
            OppdragResultat(
                behandlingId = behandling.id,
                fagsakId = behandling.fagsakId,
                utbetalingsoppdrag = beregning.oppdrag.utbetalingsoppdrag,
                status = if (beregning.oppdrag.harEndringFraForrigeBehandling) OppdragStatus.OPPRETTET else OppdragStatus.INGEN_ENDRING,
                andeler = beregning.oppdrag.utbetalingsandeler.toSet(),
            )
        oppdragResultatRepository.insert(oppdragResultat)
        logger.info(
            "Lagret oppdrag for behandling={}: status={}, {} perioder, {} andeler",
            behandlingId,
            oppdragResultat.status,
            oppdragResultat.utbetalingsoppdrag.utbetalingsperiode.size,
            beregning.oppdrag.utbetalingsandeler.size,
        )
        return LagretUtbetalingsoppdrag(oppdragResultat = oppdragResultat, testandel = beregning.testandel)
    }

    /** ID-en `familie-oppdrag-backend` bruker for oppdraget til behandlingen, se [OppdragId]. */
    fun lagOppdragId(behandlingId: UUID): OppdragId {
        val behandling = behandlingService.hentBehandlingEllerKast(behandlingId)
        return OppdragId(
            fagsystem = GrunnOgHjelpFagsystem.IT28.kode,
            personIdent = fagsakService.hentAktivIdent(behandling.fagsakId),
            behandlingsId = behandling.eksternId.toString(),
        )
    }

    private fun beregnUtbetalingsoppdrag(
        behandling: Behandling,
        saksbehandlerId: String,
    ): Beregning? {
        val fagsak = fagsakRepository.findByIdOrNull(behandling.fagsakId) ?: error("Fant ikke fagsak for behandling=${behandling.id}")
        val tilkjentYtelse = tilkjentYtelseService.hentTilkjentYtelse(behandling.id)
        val andelerTilkjentYtelse = tilkjentYtelse?.andelerTilkjentYtelse.orEmpty().filter { tilUtbetalingsbeløp(it.beløp) != 0 }
        val utbetalingsandelerForrigeBehandling = oppdragResultatRepository.finnAndelerFraForrigeBehandling(fagsak.id, unntattBehandlingId = behandling.id)
        val sisteUtbetalingsandelPerKjedeIFagsak = oppdragResultatRepository.finnSisteAndelPerKjede(fagsak.id, unntattBehandlingId = behandling.id)

        // Tilkjent ytelse uten andeler er et opphør når fagsaken har en kjede, og ingenting ellers.
        // Mangler tilkjent ytelse helt, er den ikke beregnet ennå.
        val manglerAndeler = tilkjentYtelse == null || (andelerTilkjentYtelse.isEmpty() && sisteUtbetalingsandelPerKjedeIFagsak.isEmpty())
        val testandel =
            when {
                !manglerAndeler -> {
                    null
                }

                brukTestandelHvisMangler -> {
                    logger.info("Behandling={} mangler tilkjent ytelse, bruker testandel", behandling.id)
                    lagTestandel(behandling.id, YearMonth.now(clock))
                }

                tilkjentYtelse == null -> {
                    throw Feil("Behandlingen har ingen tilkjent ytelse", HttpStatus.BAD_REQUEST)
                }

                else -> {
                    return null
                }
            }

        val oppdrag =
            lagUtbetalingsoppdrag(
                Oppdragsgrunnlag(
                    behandlingId = behandling.id,
                    eksternBehandlingId = behandling.eksternId,
                    eksternFagsakId = fagsak.eksternId,
                    personIdent = fagsakService.hentAktivIdent(fagsak.id),
                    saksbehandlerId = saksbehandlerId,
                    vedtaksdato = LocalDate.now(clock),
                    andelerTilkjentYtelse = testandel?.let { listOf(it) } ?: andelerTilkjentYtelse.sortedBy { it.fom },
                    utbetalingsandelerForrigeBehandling = utbetalingsandelerForrigeBehandling,
                    sisteAndelIHverKjedeForFagsak = sisteUtbetalingsandelPerKjedeIFagsak,
                ),
            )
        return Beregning(
            oppdrag = oppdrag,
            testandel = testandel?.let { Testandel(fom = it.fom, tom = it.tom, beløp = tilUtbetalingsbeløp(it.beløp)) },
        )
    }

    private data class Beregning(
        val oppdrag: BeregnetOppdrag,
        val testandel: Testandel?,
    )

    companion object {
        private const val TESTANDEL_BELØP = 1000

        /**
         * TODO: Midlertidig, bare for å teste integrasjonen i dev før behandlingen lager tilkjent ytelse
         * (issue 09 og 10 i planen for sats og trygdetid). Fjern når den flyten er på plass.
         */
        private fun lagTestandel(
            behandlingId: UUID,
            måned: YearMonth,
        ): AndelTilkjentYtelse =
            AndelTilkjentYtelse(
                beløp = TESTANDEL_BELØP.toBigDecimal(),
                fom = måned.atDay(1),
                tom = måned.atEndOfMonth(),
                kildeBehandlingId = behandlingId,
            )
    }
}

data class UtbetalingsoppdragForBehandling(
    val utbetalingsoppdrag: Utbetalingsoppdrag,
    /** Satt når behandlingen manglet tilkjent ytelse og oppdraget ble laget med en testandel. */
    val testandel: Testandel?,
)

data class LagretUtbetalingsoppdrag(
    val oppdragResultat: OppdragResultat,
    /** Satt når oppdraget ble laget nå med en testandel. Null når oppdraget fantes fra før. */
    val testandel: Testandel?,
)

data class Testandel(
    val fom: LocalDate,
    val tom: LocalDate,
    val beløp: Int,
)
