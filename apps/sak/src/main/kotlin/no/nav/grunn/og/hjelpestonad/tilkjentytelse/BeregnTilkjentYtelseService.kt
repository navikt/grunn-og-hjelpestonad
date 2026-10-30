package no.nav.grunn.og.hjelpestonad.tilkjentytelse

import no.nav.grunn.og.hjelpestonad.vedtak.ResultatType
import no.nav.grunn.og.hjelpestonad.vedtak.Vedtak
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BeregnTilkjentYtelseService(
    private val tilkjentYtelseService: TilkjentYtelseService,
) {
    fun opprettEllerOppdaterTilkjentYtelse(
        behandlingId: UUID,
        vedtak: Vedtak,
    ) {
        if (vedtak.resultatType == ResultatType.AVSLÅTT || vedtak.resultatType == ResultatType.HENLAGT) return

        tilkjentYtelseService.lagre(
            TilkjentYtelse(
                behandlingId = behandlingId,
                andelerTilkjentYtelse = beregnAndeler(vedtak, kildeBehandlingId = behandlingId),
            ),
        )
    }
}
