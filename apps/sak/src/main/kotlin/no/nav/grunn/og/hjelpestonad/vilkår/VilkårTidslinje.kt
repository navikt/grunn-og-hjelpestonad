package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.filtrerIkkeNull
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import java.util.UUID

fun <T : VilkårPeriode<T>> List<T>.tilTidslinje(): Tidslinje<T> = map { it.tilPeriode() }.tilTidslinje()

fun <T : VilkårPeriode<T>> List<T>.erOppfyltTidslinje(): Tidslinje<Boolean> = map { Periode(it.erVilkårOppfylt(), it.fraOgMedDato, it.tilOgMedDato) }.tilTidslinje()

fun <T> tomTidslinje(): Tidslinje<T> = emptyList<Periode<T>>().tilTidslinje()

fun <T : VilkårPeriode<T>> List<T>.medNyPeriode(periode: T): List<T> {
    val tidslinjer = filterNot { it.id == periode.id }.groupBy { it.tidslinjenøkkel() }
    val tidslinjeMedNyPeriode =
        tidslinjer[periode.tidslinjenøkkel()]?.forkortetAv(periode)?.map {
            val urørt = it.fom == it.verdi.fraOgMedDato && it.tom == it.verdi.tilOgMedDato
            it.verdi.kopierMedTidsrom(id = if (urørt) it.verdi.id else UUID.randomUUID(), fraOgMedDato = it.fom, tilOgMedDato = it.tom)
        } ?: emptyList()

    val uendredeTidslinjer = tidslinjer.filterKeys { it != periode.tidslinjenøkkel() }
    return uendredeTidslinjer.values.flatten() + tidslinjeMedNyPeriode + periode
}

fun <T : VilkårPeriode<T>> List<T>.forkortetAv(nyEllerEndretPeriode: Periodisert): List<Periode<T>> {
    val nyTidslinje = nyEllerEndretPeriode.tilPeriode().tilTidslinje()

    return nyTidslinje
        .kombinerMed(tilTidslinje()) { ny, lagret -> if (ny != null) null else lagret }
        .tilPerioder()
        .filtrerIkkeNull()
}
