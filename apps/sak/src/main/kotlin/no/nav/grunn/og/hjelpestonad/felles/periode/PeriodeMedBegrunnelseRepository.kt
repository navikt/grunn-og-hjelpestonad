package no.nav.grunn.og.hjelpestonad.felles.periode

import no.nav.grunn.og.hjelpestonad.felles.InsertUpdateRepository
import no.nav.grunn.og.hjelpestonad.felles.RepositoryInterface
import org.springframework.data.repository.NoRepositoryBean
import java.util.UUID

@NoRepositoryBean
interface PeriodeMedBegrunnelseRepository<T : PeriodeMedBegrunnelse<T>> :
    RepositoryInterface<T, UUID>,
    InsertUpdateRepository<T> {
    fun findByBehandlingId(behandlingId: UUID): List<T>

    fun deleteByBehandlingId(behandlingId: UUID)
}
