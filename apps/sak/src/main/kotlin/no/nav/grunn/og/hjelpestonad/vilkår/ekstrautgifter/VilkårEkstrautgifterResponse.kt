package no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter

import no.nav.grunn.og.hjelpestonad.vilkår.VilkårPeriodeRequest
import no.nav.grunn.og.hjelpestonad.vilkår.Vurdering
import java.time.LocalDate
import java.util.UUID

data class VilkårEkstrautgifterResponse(
    val id: UUID,
    val behandlingId: UUID,
    val vurdering: Vurdering,
    val sats: Sats?,
    val ekstrautgifter: List<Ekstrautgift>,
    val begrunnelse: String,
    val fraOgMedDato: LocalDate?,
    val tilOgMedDato: LocalDate?,
    val erVilkårOppfylt: Boolean,
)

data class VilkårEkstrautgifterRequest(
    override val id: UUID? = null,
    override val vurdering: Vurdering,
    val sats: Sats? = null,
    val ekstrautgifter: List<Ekstrautgift> = emptyList(),
    override val begrunnelse: String = "",
    override val fraOgMedDato: LocalDate? = null,
    override val tilOgMedDato: LocalDate? = null,
) : VilkårPeriodeRequest

val VilkårEkstrautgifterRequest.diagnosekoder: Set<String> get() = ekstrautgifter.flatMapTo(mutableSetOf()) { it.diagnosekoder }

fun VilkårEkstrautgifter.tilResponse() =
    VilkårEkstrautgifterResponse(
        id = id,
        behandlingId = behandlingId,
        vurdering = vurdering,
        sats = sats,
        ekstrautgifter = ekstrautgifter,
        begrunnelse = begrunnelse,
        fraOgMedDato = fraOgMedDato,
        tilOgMedDato = tilOgMedDato,
        erVilkårOppfylt = erVilkårOppfylt(),
    )
