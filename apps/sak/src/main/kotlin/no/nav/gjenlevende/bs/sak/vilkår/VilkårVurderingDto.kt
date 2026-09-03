package no.nav.gjenlevende.bs.sak.vilkår

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class VilkårVurderingDto(
    val id: UUID?,
    val behandlingId: UUID,
    @JsonProperty("vilkårType")
    val vilkårType: VilkårType,
    val vurdering: Vurdering,
    val begrunnelse: String,
    val erVilkårOppfylt: Boolean,
)

data class VilkårVurderingRequest(
    val vilkårType: VilkårType,
    val vurdering: Vurdering,
    val begrunnelse: String = "",
)

fun VilkårVurdering.tilDto() =
    VilkårVurderingDto(
        id = this.id,
        behandlingId = this.behandlingId,
        vilkårType = this.vilkårType,
        vurdering = this.vurdering,
        begrunnelse = this.begrunnelse,
        erVilkårOppfylt = this.erVilkårOppfylt(),
    )
