package no.nav.grunn.og.hjelpestonad.vilkår.rett

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PeriodeMedRettService(
    private val periodeMedRettRepository: PeriodeMedRettRepository,
) {
    fun hentPerioderMedRett(behandlingId: UUID): List<PeriodeMedRett> =
        periodeMedRettRepository
            .findByBehandlingId(behandlingId)
            .sortedWith(compareBy(nullsFirst()) { it.fraOgMedDato })
}
