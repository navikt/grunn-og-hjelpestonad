package no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering

import no.nav.familie.tidslinje.filtrerIkkeNull
import no.nav.familie.tidslinje.utvidelser.slåSammenLikePerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.grunn.og.hjelpestonad.felles.periode.Periodisert
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnose
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskap
import java.time.LocalDate

/**
 * En periode der både medlemskap- og diagnosevilkåret er oppfylt.
 */
data class PeriodeMedOppfylteInngangsvilkår(
    override val fraOgMedDato: LocalDate?,
    override val tilOgMedDato: LocalDate?,
) : Periodisert

object PerioderMedOppfylteInngangsvilkårUtleder {
    fun utled(vurderteInngangsvilkår: VurderteInngangsvilkår): List<PeriodeMedOppfylteInngangsvilkår> = utled(medlemskap = vurderteInngangsvilkår.medlemskap, diagnoser = vurderteInngangsvilkår.diagnoser)

    fun utled(
        medlemskap: List<VilkårMedlemskap>,
        diagnoser: List<VilkårDiagnose>,
    ): List<PeriodeMedOppfylteInngangsvilkår> =
        medlemskap
            .erOppfyltTidslinje(diagnoser)
            .slåSammenLikePerioder()
            .tilPerioder()
            .filtrerIkkeNull()
            .map { PeriodeMedOppfylteInngangsvilkår(fraOgMedDato = it.fom, tilOgMedDato = it.tom) }
}
