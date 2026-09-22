package no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering

import no.nav.grunn.og.hjelpestonad.vilkår.VilkårType

data class Valideringsfeil(
    val vilkårType: VilkårType,
    val melding: String,
)

object VilkårVurderingValidering {
    fun valider(grunnlag: VilkårVurderingStegGrunnlag): List<Valideringsfeil> =
        buildList {
            if (grunnlag.medlemskap.isEmpty()) {
                add(
                    Valideringsfeil(
                        VilkårType.MEDLEM_I_TRYGDEN_ELLER_OMFATTET_AV_EØS_FORORDNINGEN,
                        "Du må vurdere medlemskap i folketrygden.",
                    ),
                )
            }
            if (grunnlag.diagnoser.isEmpty()) {
                add(
                    Valideringsfeil(
                        VilkårType.DIAGNOSE,
                        "Du må vurdere om det foreligger en varig sykdom, skade eller lyte.",
                    ),
                )
            }
            if (grunnlag.institusjon.isEmpty()) {
                add(
                    Valideringsfeil(
                        VilkårType.IKKE_OPPHOLD_I_INSTITUSJON_ELLER_LOVREGULERT_BOFORM,
                        "Du må vurdere opphold i institusjon eller lovregulert boform.",
                    ),
                )
            }
        }
}
