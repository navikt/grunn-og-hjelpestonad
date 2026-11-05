package no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering

enum class NesteStegEtterInngangsvilkår {
    /** Det finnes perioder med oppfylte inngangsvilkår, som må vurderes for sats og trygdetid. */
    SATS_OG_TRYGDETID,

    /**
     * Ingen rett i noen måned. Sats og trygdetid hoppes over.
     */
    SIMULERING,
    ;

    companion object {
        fun utled(perioder: List<PeriodeMedOppfylteInngangsvilkår>): NesteStegEtterInngangsvilkår = if (perioder.isEmpty()) SIMULERING else SATS_OG_TRYGDETID
    }
}
