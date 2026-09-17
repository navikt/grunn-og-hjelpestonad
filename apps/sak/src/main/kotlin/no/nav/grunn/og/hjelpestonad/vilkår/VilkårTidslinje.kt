package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje

fun <T : VilkårPeriode> List<T>.tilTidslinje(): Tidslinje<T> = map { Periode(it, it.fraOgMedDato, it.tilOgMedDato) }.tilTidslinje()

fun List<Periodisert>.validerIngenOverlappMed(nyEllerEndretPeriode: Periodisert) {
    (this + nyEllerEndretPeriode).map { Periode<Any>(it, it.fraOgMedDato, it.tilOgMedDato) }.tilTidslinje()
}
