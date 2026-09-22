package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.filtrerIkkeNull
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioder

fun <T : VilkårPeriode<T>> List<T>.tilTidslinje(): Tidslinje<T> = map { it.tilPeriode() }.tilTidslinje()

fun <T : VilkårPeriode<T>> List<T>.erOppfyltTidslinje(): Tidslinje<Boolean> = map { Periode(it.erVilkårOppfylt(), it.fraOgMedDato, it.tilOgMedDato) }.tilTidslinje()

fun <T> tomTidslinje(): Tidslinje<T> = emptyList<Periode<T>>().tilTidslinje()

fun <T : VilkårPeriode<T>> List<T>.forkortetAv(nyEllerEndretPeriode: Periodisert): List<Periode<T>> {
    val nyTidslinje = nyEllerEndretPeriode.tilPeriode().tilTidslinje()

    return nyTidslinje
        .kombinerMed(tilTidslinje()) { ny, lagret -> if (ny != null) null else lagret }
        .tilPerioder()
        .filtrerIkkeNull()
}
