package no.nav.grunn.og.hjelpestonad.vilkår.institusjon

import no.nav.grunn.og.hjelpestonad.vilkår.VilkårPeriodeRequest
import no.nav.grunn.og.hjelpestonad.vilkår.Vurdering
import java.time.LocalDate
import java.util.UUID

data class VilkårInstitusjonResponse(
    val id: UUID,
    val behandlingId: UUID,
    val oppholdstype: Oppholdstype?,
    val unntakshjemmel: Unntakshjemmel?,
    val vurdering: Vurdering,
    val begrunnelse: String,
    val fraOgMedDato: LocalDate?,
    val tilOgMedDato: LocalDate?,
    val erVilkårOppfylt: Boolean,
)

data class VilkårInstitusjonRequest(
    override val id: UUID? = null,
    val oppholdstype: Oppholdstype? = null,
    val unntakshjemmel: Unntakshjemmel? = null,
    override val vurdering: Vurdering,
    override val begrunnelse: String = "",
    override val fraOgMedDato: LocalDate? = null,
    override val tilOgMedDato: LocalDate? = null,
) : VilkårPeriodeRequest

fun VilkårInstitusjon.tilResponse() =
    VilkårInstitusjonResponse(
        id = this.id,
        behandlingId = this.behandlingId,
        oppholdstype = this.oppholdstype,
        unntakshjemmel = this.unntakshjemmel,
        vurdering = this.vurdering,
        begrunnelse = this.begrunnelse,
        fraOgMedDato = this.fraOgMedDato,
        tilOgMedDato = this.tilOgMedDato,
        erVilkårOppfylt = this.erVilkårOppfylt(),
    )
