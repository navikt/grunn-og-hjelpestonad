package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.familie.tidslinje.Periode
import no.nav.grunn.og.hjelpestonad.felles.sporbar.Sporbar
import java.time.LocalDate
import java.util.UUID

enum class VilkårType {
    /**
     * Medlemskap i folketrygden etter ftrl. kapittel 2. § 6-1 a fraviker reglene så langt det følger av
     * trygdeforordningen (EF) 883/2004, slik at også den som er omfattet av
     * EØS-forordningen oppfyller vilkåret.
     */
    MEDLEM_I_TRYGDEN_ELLER_OMFATTET_AV_EØS_FORORDNINGEN,

    /** ftrl. § 6-2 — varig sykdom, skade eller lyte, dokumentert ved diagnose. */
    DIAGNOSE,

    /**
     * ftrl. § 6-8. Navnet er bevisst negert. § 6-8 er en bortfallsbestemmelse, og uten
     * negeringen ville JA betydd at vilkåret ikke er oppfylt — motsatt av alle andre
     * vilkår. Ikke «rett opp» navnet.
     */
    IKKE_OPPHOLD_I_INSTITUSJON_ELLER_LOVREGULERT_BOFORM,
}

enum class Vurdering {
    JA,
    NEI,
}

interface Periodisert {
    val fraOgMedDato: LocalDate?
    val tilOgMedDato: LocalDate?
}

fun <T : Periodisert> T.tilPeriode(): Periode<T> =
    Periode(
        fom = this.fraOgMedDato,
        tom = this.tilOgMedDato,
        verdi = this,
    )

interface VilkårPeriode<SELV : VilkårPeriode<SELV>> : Periodisert {
    val id: UUID
    val behandlingId: UUID
    val vurdering: Vurdering
    val begrunnelse: String
    val sporbar: Sporbar

    fun erVilkårOppfylt(): Boolean = vurdering == Vurdering.JA

    fun kopierMedTidsrom(
        id: UUID,
        fraOgMedDato: LocalDate?,
        tilOgMedDato: LocalDate?,
    ): SELV
}

interface VilkårPeriodeRequest : Periodisert {
    val id: UUID?
    val vurdering: Vurdering
    val begrunnelse: String
}
