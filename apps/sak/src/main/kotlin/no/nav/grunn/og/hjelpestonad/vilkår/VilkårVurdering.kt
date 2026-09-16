package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.grunn.og.hjelpestonad.felles.sporbar.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.util.UUID

/**
 * Vilkår for grunnstønad etter folketrygdloven kapittel 6.
 */
enum class VilkårType {
    /** ftrl. § 6-2 */
    VARIG_SYKDOM_SKADE_ELLER_LYTE,

    /** ftrl. § 6-3 første ledd */
    NØDVENDIGE_EKSTRAUTGIFTER,

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

@Table("vilkar_vurdering")
data class VilkårVurdering(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    @Column("vilkar_type")
    val vilkårType: VilkårType,
    val vurdering: Vurdering,
    val begrunnelse: String = "",
    val fraOgMedDato: LocalDate? = null,
    val tilOgMedDato: LocalDate? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) {
    fun erVilkårOppfylt(): Boolean = vurdering == Vurdering.JA

    /**
     * Åpen [fraOgMedDato] regnes som uendelig bakover og åpen [tilOgMedDato] som løpende,
     * slik at to perioder uten datoer overlapper hverandre.
     */
    fun overlapper(
        annenFraOgMedDato: LocalDate?,
        annenTilOgMedDato: LocalDate?,
    ): Boolean =
        (fraOgMedDato ?: LocalDate.MIN) <= (annenTilOgMedDato ?: LocalDate.MAX) &&
            (annenFraOgMedDato ?: LocalDate.MIN) <= (tilOgMedDato ?: LocalDate.MAX)
}

/**
 * Diagnose knyttet til en vilkårsperiode for [VilkårType.VARIG_SYKDOM_SKADE_ELLER_LYTE].
 *
 * Diagnose er helseopplysning etter GDPR artikkel 9 og er derfor et eget aggregat med egen
 * repository og eget endepunkt, ikke en del av [VilkårVurdering]. Det holder opplysningen
 * utenfor de generiske vilkårsresponsene og gir et eget punkt for tilgangsstyring og
 * auditspor, jf. ADR-0003. Diagnosen skal aldri havne i logg eller endringshistorikk.
 *
 * At diagnosen bare kan henge på riktig vilkårstype håndheves av databasen gjennom
 * fremmednøkkelen mot `vilkar_vurdering (id, vilkar_type)` og en CHECK på `vilkar_type`.
 * Kolonnen `vilkar_type` settes av databasen og er derfor bevisst utelatt her.
 *
 * Raden har ingen egne datoer — vilkårsperioden den henger på bærer dem.
 */
@Table("vilkar_diagnose")
data class VilkårDiagnose(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column("vilkar_vurdering_id")
    val vilkårVurderingId: UUID,
    val diagnose: String,
    val yrkesskade: Boolean = false,
    val yrkesskadeDato: LocalDate? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)
