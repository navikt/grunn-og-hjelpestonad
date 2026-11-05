package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.grunn.og.hjelpestonad.felles.periode.PeriodeMedBegrunnelse
import no.nav.grunn.og.hjelpestonad.felles.periode.PeriodeMedBegrunnelseRequest

enum class VilkårType {
    /**
     * Medlemskap i folketrygden etter ftrl. kapittel 2. § 6-1 a fraviker reglene så langt det følger av
     * trygdeforordningen (EF) 883/2004, slik at også den som er omfattet av
     * EØS-forordningen oppfyller vilkåret.
     */
    MEDLEM_I_TRYGDEN_ELLER_OMFATTET_AV_EØS_FORORDNINGEN,

    /** ftrl. § 6-2 — varig sykdom, skade eller lyte, dokumentert ved diagnose. */
    DIAGNOSE,
}

enum class Vurdering {
    JA,
    NEI,
}

interface VilkårPeriode<SELV : VilkårPeriode<SELV>> : PeriodeMedBegrunnelse<SELV> {
    val vurdering: Vurdering

    fun erVilkårOppfylt(): Boolean = vurdering == Vurdering.JA
}

interface VilkårPeriodeRequest : PeriodeMedBegrunnelseRequest {
    val vurdering: Vurdering
}
