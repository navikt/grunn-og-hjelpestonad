package no.nav.grunn.og.hjelpestonad.brev

import no.nav.grunn.og.hjelpestonad.brev.domain.Brevmottaker
import no.nav.grunn.og.hjelpestonad.felles.InsertUpdateRepository
import no.nav.grunn.og.hjelpestonad.felles.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BrevmottakerRepository :
    RepositoryInterface<Brevmottaker, UUID>,
    InsertUpdateRepository<Brevmottaker> {
    fun findAllByBehandlingId(behandlingId: UUID): List<Brevmottaker>

    fun deleteAllByBehandlingId(behandlingId: UUID)
}
