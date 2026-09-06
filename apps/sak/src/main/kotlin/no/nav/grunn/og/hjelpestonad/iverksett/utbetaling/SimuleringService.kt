package no.nav.grunn.og.hjelpestonad.iverksett.utbetaling

import no.nav.grunn.og.hjelpestonad.behandling.Behandling
import no.nav.grunn.og.hjelpestonad.felles.sikkerhet.Tilgangskontroll
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.TilkjentYtelseService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Tilgangskontroll
@Service
class SimuleringService(
    val utbetalingProducer: UtbetalingProducer,
    val simuleringRepository: SimuleringRepository,
    val tilkjentYtelseService: TilkjentYtelseService,
) {
    @Transactional
    fun simuler(behandlingId: UUID) {
        val melding = lagUtbetalingMelding(behandlingId)

        if (simuleringRepository.existsById(behandlingId)) {
            simuleringRepository.update(
                Simulering(
                    behandlingId = behandlingId,
                    status = SimuleringStatus.VENTER,
                ),
            )
        } else {
            simuleringRepository.insert(
                Simulering(
                    behandlingId = behandlingId,
                    status = SimuleringStatus.VENTER,
                ),
            )
        }

        utbetalingProducer.sendSimulering(behandlingId, melding)
    }

    fun hentSimulering(behandlingId: UUID): Simulering? = simuleringRepository.findById(behandlingId).orElse(null)

    fun lagUtbetalingMelding(
        behandlingId: UUID,
    ): UtbetalingMelding {
        val andeler = tilkjentYtelseService.hentTilkjentYtelse(behandlingId)?.andelerTilkjentYtelse ?: emptySet()
        // TODO
        return UtbetalingMelding(
            behandlingId = behandlingId,
            sakId = "sakId",
            personident = "12345699999",
            stønad = "GJENLEVENDE_BARNETILSYN",
            vedtakstidspunkt = LocalDateTime.now(),
            periodetype = Periodetype.MND,
            perioder = andeler.map { Periode(fom = it.fom, tom = it.tom, beløp = it.beløp) },
            saksbehandler = "Saksbehandler",
            beslutter = "beslutter",
            dryrun = true,
        )
    }
}
