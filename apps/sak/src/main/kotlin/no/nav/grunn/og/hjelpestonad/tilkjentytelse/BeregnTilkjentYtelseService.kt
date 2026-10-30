package no.nav.grunn.og.hjelpestonad.tilkjentytelse

import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.vedtak.ResultatType
import no.nav.grunn.og.hjelpestonad.vedtak.Vedtak
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BeregnTilkjentYtelseService(
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseService: TilkjentYtelseService,
) {
    fun opprettEllerOppdaterTilkjentYtelse(
        behandlingId: UUID,
        vedtak: Vedtak,
    ) {
        if (vedtak.resultatType == ResultatType.AVSLÅTT || vedtak.resultatType == ResultatType.HENLAGT) return

        val andeler = beregnAndeler(vedtak, kildeBehandlingId = behandlingId)
        val forrigeStartmåned =
            behandlingService.hentBehandlingEllerKast(behandlingId).forrigeBehandlingId?.let {
                tilkjentYtelseService.hentTilkjentYtelse(it)?.startmåned
            }

        tilkjentYtelseService.lagre(
            TilkjentYtelse(
                behandlingId = behandlingId,
                startmåned =
                    utledStartmåned(
                        andeler = andeler,
                        forrigeStartmåned = forrigeStartmåned,
                        opphørsmåned = vedtak.opphørFom.takeIf { vedtak.resultatType == ResultatType.OPPHØR },
                    ),
                andelerTilkjentYtelse = andeler,
            ),
        )
    }
}
