package no.nav.grunn.og.hjelpestonad.tilkjentytelse

import no.nav.grunn.og.hjelpestonad.vedtak.BarnetilsynBeregning
import no.nav.grunn.og.hjelpestonad.vedtak.BeløpsperioderResponse
import no.nav.grunn.og.hjelpestonad.vedtak.BeregningUtils.beregnBarnetilsynperiode
import no.nav.grunn.og.hjelpestonad.vedtak.PeriodetypeBarnetilsyn
import no.nav.grunn.og.hjelpestonad.vedtak.Vedtak
import java.util.UUID

// TODO: Beregningen er arvet fra barnetilsyn i forken og er feil for grunnstønad.
// Andelene skal beregnes fra sats og trygdetid fra vilkårsvurderingen, som ikke er på plass ennå.
fun beregnAndeler(
    vedtak: Vedtak,
    kildeBehandlingId: UUID,
): Set<AndelTilkjentYtelse> {
    val beregninger =
        vedtak.grunnstønadPerioder.map { periode ->
            BarnetilsynBeregning(
                datoFra = periode.datoFra,
                datoTil = periode.datoTil,
                utgifter = periode.utgifter,
                barn = periode.barn,
                periodetype = periode.periodetype,
            )
        }
    return beregnBarnetilsynperiode(beregninger)
        .filter { it.periodetype == PeriodetypeBarnetilsyn.ORDINÆR && it.beløp > 0 }
        .map { it.tilAndelTilkjentYtelse(kildeBehandlingId) }
        .toSet()
}

private fun BeløpsperioderResponse.tilAndelTilkjentYtelse(kildeBehandlingId: UUID) =
    AndelTilkjentYtelse(
        beløp = this.beløp,
        fom = this.datoFra.atDay(1),
        tom = this.datoTil.atEndOfMonth(),
        kildeBehandlingId = kildeBehandlingId,
    )
