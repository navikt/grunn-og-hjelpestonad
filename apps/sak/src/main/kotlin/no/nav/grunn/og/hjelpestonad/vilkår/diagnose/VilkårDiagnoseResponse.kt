package no.nav.grunn.og.hjelpestonad.vilkår.diagnose

import no.nav.grunn.og.hjelpestonad.vilkår.VilkårPeriodeRequest
import no.nav.grunn.og.hjelpestonad.vilkår.Vurdering
import java.time.LocalDate
import java.util.UUID

data class VilkårDiagnoseResponse(
    val id: UUID,
    val behandlingId: UUID,
    val diagnose: String,
    val erYrkesskade: Boolean,
    val vurdering: Vurdering,
    val begrunnelse: String,
    val fraOgMedDato: LocalDate?,
    val tilOgMedDato: LocalDate?,
    val erVilkårOppfylt: Boolean,
)

data class VilkårDiagnoseRequest(
    override val id: UUID? = null,
    val diagnose: String,
    val erYrkesskade: Boolean = false,
    override val vurdering: Vurdering,
    override val begrunnelse: String = "",
    override val fraOgMedDato: LocalDate? = null,
    override val tilOgMedDato: LocalDate? = null,
) : VilkårPeriodeRequest

fun VilkårDiagnose.tilResponse() =
    VilkårDiagnoseResponse(
        id = this.id,
        behandlingId = this.behandlingId,
        diagnose = this.diagnose,
        erYrkesskade = this.erYrkesskade,
        vurdering = this.vurdering,
        begrunnelse = this.begrunnelse,
        fraOgMedDato = this.fraOgMedDato,
        tilOgMedDato = this.tilOgMedDato,
        erVilkårOppfylt = this.erVilkårOppfylt(),
    )
