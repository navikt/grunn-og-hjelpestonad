package no.nav.grunn.og.hjelpestonad.vilkår.rett

import no.nav.grunn.og.hjelpestonad.felles.sporbar.Sporbar
import no.nav.grunn.og.hjelpestonad.vilkår.Periodisert
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.util.UUID

@Table("periode_med_rett")
data class PeriodeMedRett(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    override val fraOgMedDato: LocalDate? = null,
    override val tilOgMedDato: LocalDate? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) : Periodisert
