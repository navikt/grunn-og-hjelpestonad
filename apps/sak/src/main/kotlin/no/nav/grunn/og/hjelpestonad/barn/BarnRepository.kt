package no.nav.grunn.og.hjelpestonad.barn

import no.nav.grunn.og.hjelpestonad.felles.InsertUpdateRepository
import no.nav.grunn.og.hjelpestonad.felles.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BarnRepository :
    RepositoryInterface<BehandlingBarn, UUID>,
    InsertUpdateRepository<BehandlingBarn> {
    fun findByBehandlingId(behandlingId: UUID): List<BehandlingBarn>

    fun deleteByBehandlingId(behandlingId: UUID)
}
