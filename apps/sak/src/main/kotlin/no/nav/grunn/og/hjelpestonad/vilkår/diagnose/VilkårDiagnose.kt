package no.nav.grunn.og.hjelpestonad.vilkår.diagnose

import no.nav.grunn.og.hjelpestonad.felles.sporbar.Sporbar
import no.nav.grunn.og.hjelpestonad.vilkår.VilkårPeriode
import no.nav.grunn.og.hjelpestonad.vilkår.Vurdering
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.util.UUID

@Table("vilkar_diagnose")
data class VilkårDiagnose(
    @Id
    override val id: UUID = UUID.randomUUID(),
    override val behandlingId: UUID,
    override val vurdering: Vurdering,
    override val begrunnelse: String = "",
    override val fraOgMedDato: LocalDate? = null,
    override val tilOgMedDato: LocalDate? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    override val sporbar: Sporbar = Sporbar(),
    val kode: String,
    val tekst: String,
    val erYrkesskade: Boolean = false,
) : VilkårPeriode<VilkårDiagnose> {
    /**
     * Én tidslinje per diagnose. Søker kan ha flere diagnoser samtidig, men hver diagnose har én
     * vurdering per dag. Dette er definisjonen av «samme diagnose», også for vilkårssteget.
     */
    override fun tidslinjenøkkel() = kode

    override fun kopierTilBehandling(behandlingId: UUID) = copy(id = UUID.randomUUID(), behandlingId = behandlingId, sporbar = Sporbar())

    override fun kopierMedTidsrom(
        id: UUID,
        fraOgMedDato: LocalDate?,
        tilOgMedDato: LocalDate?,
    ) = copy(id = id, fraOgMedDato = fraOgMedDato, tilOgMedDato = tilOgMedDato)
}
