package no.nav.grunn.og.hjelpestonad.vilkår.rett

import java.time.LocalDate
import java.util.UUID

data class PeriodeMedRettResponse(
    val id: UUID,
    val behandlingId: UUID,
    val fraOgMedDato: LocalDate?,
    val tilOgMedDato: LocalDate?,
)

fun PeriodeMedRett.tilResponse() =
    PeriodeMedRettResponse(
        id = this.id,
        behandlingId = this.behandlingId,
        fraOgMedDato = this.fraOgMedDato,
        tilOgMedDato = this.tilOgMedDato,
    )
