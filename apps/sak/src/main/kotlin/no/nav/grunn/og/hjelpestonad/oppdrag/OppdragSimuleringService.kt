package no.nav.grunn.og.hjelpestonad.oppdrag

import io.micrometer.core.instrument.MeterRegistry
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
 * Simulerer en behandling mot oppdrag gjennom `familie-oppdrag-backend`.
 *
 * Første versjon, for å se at integrasjonen virker: bare førstegangsbehandling, ingen kjeding mot
 * tidligere andeler og ingen lagring av resultatet (se ADR-0011).
 */
@Service
class OppdragSimuleringService(
    private val behandlingService: BehandlingService,
    private val fagsakRepository: FagsakRepository,
    private val fagsakService: FagsakService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val oppdragClient: OppdragClient,
    private val meterRegistry: MeterRegistry,
    @Value("\${oppdrag.simulering.bruk-testandel-hvis-mangler:false}")
    private val brukTestandelHvisMangler: Boolean,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val logger = LoggerFactory.getLogger(OppdragSimuleringService::class.java)

    fun simuler(behandlingId: UUID): OppdragSimuleringResponse {
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
                    throw Feil("Behandlingen har ingen tilkjent ytelse å simulere", HttpStatus.BAD_REQUEST)
                }
                logger.info("Behandling={} mangler tilkjent ytelse, simulerer med testandel", behandlingId)
                lagTestandel(behandlingId, YearMonth.now(clock))
            } else {
                null
            }

        val utbetalingsoppdrag =
            lagUtbetalingsoppdragForSimulering(
                SimuleringsgrunnlagOppdrag(
                    behandlingId = behandling.id,
                    eksternBehandlingId = behandling.eksternId,
                    eksternFagsakId = fagsak.eksternId,
                    personIdent = fagsakService.hentAktivIdent(fagsak.id),
                    saksbehandlerId = SikkerhetContext.hentSaksbehandler(),
                    vedtaksdato = LocalDate.now(clock),
                    andeler = testandel?.let { listOf(it) } ?: andeler.sortedBy { it.fom },
                ),
            )

        val resultat =
            runCatching { oppdragClient.simuler(utbetalingsoppdrag) }
                .onSuccess { tellSimulering("ok") }
                .onFailure { tellSimulering("feilet") }
                .getOrThrow()

        return OppdragSimuleringResponse(
            resultat = resultat,
            testandel = testandel?.let { Testandel(fom = it.fom, tom = it.tom, beløp = tilUtbetalingsbeløp(it.beløp)) },
        )
    }

    private fun tellSimulering(resultat: String) = meterRegistry.counter("oppdrag_simulering", "resultat", resultat).increment()

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

data class OppdragSimuleringResponse(
    val resultat: DetaljertSimuleringResultat,
    /** Satt når behandlingen manglet tilkjent ytelse og vi simulerte med en testandel. */
    val testandel: Testandel?,
)

data class Testandel(
    val fom: LocalDate,
    val tom: LocalDate,
    val beløp: Int,
)
