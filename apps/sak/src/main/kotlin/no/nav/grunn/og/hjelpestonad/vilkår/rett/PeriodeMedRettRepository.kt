package no.nav.grunn.og.hjelpestonad.vilkår.rett

import no.nav.grunn.og.hjelpestonad.felles.InsertUpdateRepository
import no.nav.grunn.og.hjelpestonad.felles.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PeriodeMedRettRepository :
    RepositoryInterface<PeriodeMedRett, UUID>,
    InsertUpdateRepository<PeriodeMedRett> {
    fun findByBehandlingId(behandlingId: UUID): List<PeriodeMedRett>

    fun deleteByBehandlingId(behandlingId: UUID)
}
