package no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering

import no.nav.familie.tidslinje.filtrerIkkeNull
import no.nav.familie.tidslinje.utvidelser.slåSammenLikePerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.grunn.og.hjelpestonad.vilkår.Periodisert
import java.time.LocalDate

/**
 * En periode der både medlemskap- og diagnosevilkåret er oppfylt.
 */
data class PeriodeMedOppfylteInngangsvilkår(
    override val fraOgMedDato: LocalDate?,
    override val tilOgMedDato: LocalDate?,
) : Periodisert

object PerioderMedOppfylteInngangsvilkårUtleder {
    fun utled(vurderteInngangsvilkår: VurderteInngangsvilkår): List<PeriodeMedOppfylteInngangsvilkår> =
        vurderteInngangsvilkår.medlemskap
            .erOppfyltTidslinje(vurderteInngangsvilkår.diagnoser)
            .slåSammenLikePerioder()
            .tilPerioder()
            .filtrerIkkeNull()
            .map { PeriodeMedOppfylteInngangsvilkår(fraOgMedDato = it.fom, tilOgMedDato = it.tom) }
}
