package no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering

import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnose
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskap

enum class Valideringsnøkkel {
    MEDLEM_I_TRYGDEN_ELLER_OMFATTET_AV_EØS_FORORDNINGEN,
    DIAGNOSE,
    NØDVENDIGE_EKSTRAUTGIFTER,
    TRYGDETID,
}

data class Valideringsfeil(
    val nøkkel: Valideringsnøkkel,
    val melding: String,
)

object VilkårVurderingValidering {
    fun valider(vurderteInngangsvilkår: VurderteInngangsvilkår): List<Valideringsfeil> = valider(vurderteInngangsvilkår.medlemskap, vurderteInngangsvilkår.diagnoser)

    fun valider(
        medlemskap: List<VilkårMedlemskap>,
        diagnoser: List<VilkårDiagnose>,
    ): List<Valideringsfeil> =
        buildList {
            if (medlemskap.isEmpty()) {
                add(
                    Valideringsfeil(
                        Valideringsnøkkel.MEDLEM_I_TRYGDEN_ELLER_OMFATTET_AV_EØS_FORORDNINGEN,
                        "Du må vurdere medlemskap i folketrygden.",
                    ),
                )
            }
            if (diagnoser.isEmpty()) {
                add(
                    Valideringsfeil(
                        Valideringsnøkkel.DIAGNOSE,
                        "Du må vurdere om det foreligger en varig sykdom, skade eller lyte.",
                    ),
                )
            }
        }
}
