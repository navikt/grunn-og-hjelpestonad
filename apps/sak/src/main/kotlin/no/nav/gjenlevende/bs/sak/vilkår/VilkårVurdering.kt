package no.nav.gjenlevende.bs.sak.vilkår

import no.nav.gjenlevende.bs.sak.felles.sporbar.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

enum class VilkårType {
    INNGANGSVILKÅR,
    AKTIVITET,
    INNTEKT,
    ALDER_PÅ_BARN,
    DOKUMENTASJON_TILSYNSUTGIFTER,
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
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) {
    fun erVilkårOppfylt(): Boolean = vurdering == Vurdering.JA
}
