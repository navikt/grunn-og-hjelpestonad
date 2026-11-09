package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje

fun <T : VilkårPeriode<T>> List<T>.erOppfyltTidslinje(): Tidslinje<Boolean> = map { Periode(it.erVilkårOppfylt(), it.fraOgMedDato, it.tilOgMedDato) }.tilTidslinje()
