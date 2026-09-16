package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.grunn.og.hjelpestonad.felles.sporbar.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.util.UUID

@Table("vilkar_diagnose")
data class VilkårDiagnose(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column("vilkar_vurdering_id")
    val vilkårVurderingId: UUID,
    val diagnose: String,
    val yrkesskade: Boolean = false,
    val yrkesskadeDato: LocalDate? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)
