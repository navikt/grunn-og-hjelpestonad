package no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering

import no.nav.familie.tidslinje.filtrerIkkeNull
import no.nav.familie.tidslinje.utvidelser.slåSammenLikePerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.grunn.og.hjelpestonad.vilkår.rett.PeriodeMedRett

object PeriodeMedRettUtleder {
    fun utled(grunnlag: VilkårVurderingStegGrunnlag): List<PeriodeMedRett> {
        val feil = VilkårVurderingValidering.valider(grunnlag)
        if (feil.isNotEmpty()) throw VilkårValideringFeil(feil)

        return grunnlag.medlemskap
            .erOppfyltTidslinje(grunnlag.diagnoser)
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
}
