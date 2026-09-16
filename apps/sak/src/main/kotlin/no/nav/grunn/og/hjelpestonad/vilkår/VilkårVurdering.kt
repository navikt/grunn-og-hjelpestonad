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
}
