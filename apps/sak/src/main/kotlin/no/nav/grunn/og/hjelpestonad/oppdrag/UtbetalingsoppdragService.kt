package no.nav.grunn.og.hjelpestonad.oppdrag

import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakRepository
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakService
import no.nav.grunn.og.hjelpestonad.felles.sikkerhet.SikkerhetContext
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.AndelTilkjentYtelse
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.TilkjentYtelseService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/**
 * Lager utbetalingsoppdraget for en behandling. Brukes av både simulering og iverksetting.
 *
 * Bare førstegangsbehandling: ingen kjeding mot tidligere andeler (se ADR-0011).
 */
@Service
class UtbetalingsoppdragService(
    private val behandlingService: BehandlingService,
    private val fagsakRepository: FagsakRepository,
    private val fagsakService: FagsakService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    @Value("\${oppdrag.bruk-testandel-hvis-mangler:false}")
    private val brukTestandelHvisMangler: Boolean,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val logger = LoggerFactory.getLogger(UtbetalingsoppdragService::class.java)

    fun lagUtbetalingsoppdrag(behandlingId: UUID): UtbetalingsoppdragForBehandling {
        val behandling = behandlingService.hentBehandlingEllerKast(behandlingId)
        val fagsak = fagsakRepository.findByIdOrNull(behandling.fagsakId) ?: error("Fant ikke fagsak for behandling=$behandlingId")
        val andeler =
            tilkjentYtelseService
                .hentTilkjentYtelse(behandlingId)
                ?.andelerTilkjentYtelse
                .orEmpty()
                .filter { tilUtbetalingsbeløp(it.beløp) != 0 }

        val testandel =
            if (andeler.isEmpty()) {
                if (!brukTestandelHvisMangler) {
                    throw Feil("Behandlingen har ingen tilkjent ytelse", HttpStatus.BAD_REQUEST)
                }
                logger.info("Behandling={} mangler tilkjent ytelse, bruker testandel", behandlingId)
                lagTestandel(behandlingId, YearMonth.now(clock))
            } else {
                null
            }

        val utbetalingsoppdrag =
            lagUtbetalingsoppdrag(
                Oppdragsgrunnlag(
                    behandlingId = behandling.id,
                    eksternBehandlingId = behandling.eksternId,
                    eksternFagsakId = fagsak.eksternId,
                    personIdent = fagsakService.hentAktivIdent(fagsak.id),
                    saksbehandlerId = SikkerhetContext.hentSaksbehandler(),
                    vedtaksdato = LocalDate.now(clock),
                    andeler = testandel?.let { listOf(it) } ?: andeler.sortedBy { it.fom },
                ),
            )

        return UtbetalingsoppdragForBehandling(
            utbetalingsoppdrag = utbetalingsoppdrag,
            testandel = testandel?.let { Testandel(fom = it.fom, tom = it.tom, beløp = tilUtbetalingsbeløp(it.beløp)) },
        )
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

data class Testandel(
    val fom: LocalDate,
    val tom: LocalDate,
    val beløp: Int,
)
