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
    val diagnose: String,
    val erYrkesskade: Boolean = false,
) : VilkårPeriode
