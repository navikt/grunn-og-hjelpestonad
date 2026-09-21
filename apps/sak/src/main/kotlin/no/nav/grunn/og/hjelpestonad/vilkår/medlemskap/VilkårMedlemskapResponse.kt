package no.nav.grunn.og.hjelpestonad.vilkår.medlemskap

import no.nav.grunn.og.hjelpestonad.vilkår.VilkårPeriodeRequest
import no.nav.grunn.og.hjelpestonad.vilkår.Vurdering
import java.time.LocalDate
import java.util.UUID

data class VilkårMedlemskapResponse(
    val id: UUID,
    val behandlingId: UUID,
    val regelverk: Regelverk,
    val vurdering: Vurdering,
    val begrunnelse: String,
    val fraOgMedDato: LocalDate?,
    val tilOgMedDato: LocalDate?,
    val erVilkårOppfylt: Boolean,
)

data class VilkårMedlemskapRequest(
    override val id: UUID? = null,
    val regelverk: Regelverk,
    override val vurdering: Vurdering,
    override val begrunnelse: String = "",
    override val fraOgMedDato: LocalDate? = null,
    override val tilOgMedDato: LocalDate? = null,
) : VilkårPeriodeRequest

fun VilkårMedlemskap.tilResponse() =
    VilkårMedlemskapResponse(
        id = this.id,
        behandlingId = this.behandlingId,
        regelverk = this.regelverk,
        vurdering = this.vurdering,
        begrunnelse = this.begrunnelse,
        fraOgMedDato = this.fraOgMedDato,
        tilOgMedDato = this.tilOgMedDato,
        erVilkårOppfylt = this.erVilkårOppfylt(),
    )
