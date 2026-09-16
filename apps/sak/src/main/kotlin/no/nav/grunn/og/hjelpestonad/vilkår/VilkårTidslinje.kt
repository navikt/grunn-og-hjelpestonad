package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje

fun List<VilkårVurdering>.tilTidslinje(): Tidslinje<VilkårVurdering> = map { it.tilPeriode() }.tilTidslinje()

fun List<VilkårVurdering>.validerIngenOverlappMed(nyEllerEndretPeriode: VilkårVurderingRequest) {
    val øvrigePerioder = map { Periode<Any>(it, it.fraOgMedDato, it.tilOgMedDato) }

    (øvrigePerioder + nyEllerEndretPeriode.tilPeriode()).tilTidslinje()
}

private fun VilkårVurdering.tilPeriode(): Periode<VilkårVurdering> = Periode(this, fraOgMedDato, tilOgMedDato)

private fun VilkårVurderingRequest.tilPeriode(): Periode<Any> = Periode(this, fraOgMedDato, tilOgMedDato)
