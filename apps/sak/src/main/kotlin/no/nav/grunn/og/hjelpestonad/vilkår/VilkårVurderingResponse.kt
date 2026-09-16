package no.nav.grunn.og.hjelpestonad.vilkår

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.util.UUID

data class VilkårVurderingResponse(
    val id: UUID,
    val behandlingId: UUID,
    @JsonProperty("vilkårType")
    val vilkårType: VilkårType,
    val vurdering: Vurdering,
    val begrunnelse: String,
    val fraOgMedDato: LocalDate?,
    val tilOgMedDato: LocalDate?,
    val erVilkårOppfylt: Boolean,
)

data class VilkårVurderingRequest(
    val id: UUID? = null,
    val vilkårType: VilkårType,
    val vurdering: Vurdering,
    val begrunnelse: String = "",
    val fraOgMedDato: LocalDate? = null,
    val tilOgMedDato: LocalDate? = null,
)

fun VilkårVurdering.tilResponse() =
    VilkårVurderingResponse(
        id = this.id,
        behandlingId = this.behandlingId,
        vilkårType = this.vilkårType,
        vurdering = this.vurdering,
        begrunnelse = this.begrunnelse,
        fraOgMedDato = this.fraOgMedDato,
        tilOgMedDato = this.tilOgMedDato,
        erVilkårOppfylt = this.erVilkårOppfylt(),
    )
