package no.nav.grunn.og.hjelpestonad.vilkår.institusjon

import no.nav.grunn.og.hjelpestonad.felles.sporbar.Sporbar
import no.nav.grunn.og.hjelpestonad.vilkår.VilkårPeriode
import no.nav.grunn.og.hjelpestonad.vilkår.Vurdering
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.util.UUID

enum class Oppholdstype {
    HELSE_OG_OMSORGSINSTITUSJON,
    SPESIALISTHELSETJENESTEN,
    PSYKISK_HELSEVERN,
    FENGSEL_ELLER_ANNEN_LOVREGULERT_BOFORM,
}

enum class Unntakshjemmel {
    EKSTRAUTGIFTER_IKKE_DEKKET_AV_INSTITUSJONEN,
    KORTTIDSOPPHOLD,
    BARN_UNDER_18_I_SPESIALISTHELSETJENESTEN,
    ANNET_UNNTAK_I_FORSKRIFT,
}

@Table("vilkar_institusjon")
data class VilkårInstitusjon(
    @Id
    override val id: UUID = UUID.randomUUID(),
    override val behandlingId: UUID,
    override val vurdering: Vurdering,
    override val begrunnelse: String = "",
    override val fraOgMedDato: LocalDate? = null,
    override val tilOgMedDato: LocalDate? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    override val sporbar: Sporbar = Sporbar(),
    val oppholdstype: Oppholdstype? = null,
    val unntakshjemmel: Unntakshjemmel? = null,
) : VilkårPeriode<VilkårInstitusjon> {
    override fun kopierMedTidsrom(
        id: UUID,
        fraOgMedDato: LocalDate?,
        tilOgMedDato: LocalDate?,
    ) = copy(id = id, fraOgMedDato = fraOgMedDato, tilOgMedDato = tilOgMedDato)
}
