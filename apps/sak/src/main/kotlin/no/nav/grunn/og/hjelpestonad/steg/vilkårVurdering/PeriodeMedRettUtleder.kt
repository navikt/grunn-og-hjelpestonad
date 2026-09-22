package no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering

import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.filtrerIkkeNull
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.slåSammenLikePerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.grunn.og.hjelpestonad.vilkår.erOppfyltTidslinje
import no.nav.grunn.og.hjelpestonad.vilkår.rett.PeriodeMedRett

object PeriodeMedRettUtleder {
    fun utled(grunnlag: VilkårVurderingStegGrunnlag): List<PeriodeMedRett> {
        val feil = VilkårVurderingValidering.valider(grunnlag)
        if (feil.isNotEmpty()) throw VilkårValideringFeil(feil)

        return alleVilkårOppfyltTidslinje(grunnlag)
            .slåSammenLikePerioder()
            .tilPerioder()
            .filtrerIkkeNull()
            .map {
                PeriodeMedRett(
                    behandlingId = grunnlag.behandlingId,
                    fraOgMedDato = it.fom,
                    tilOgMedDato = it.tom,
                )
            }
    }

    private fun alleVilkårOppfyltTidslinje(grunnlag: VilkårVurderingStegGrunnlag): Tidslinje<Boolean> =
        grunnlag.medlemskap
            .erOppfyltTidslinje(grunnlag.diagnoser)
            .kombinerMed(grunnlag.institusjon.erOppfyltTidslinje()) { medlemskapOgDiagnose, institusjon ->
                if (medlemskapOgDiagnose == true && institusjon == true) true else null
            }
}
