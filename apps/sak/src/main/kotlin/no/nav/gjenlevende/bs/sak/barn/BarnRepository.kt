package no.nav.gjenlevende.bs.sak.barn

import no.nav.gjenlevende.bs.sak.felles.InsertUpdateRepository
import no.nav.gjenlevende.bs.sak.felles.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BarnRepository :
    RepositoryInterface<BehandlingBarn, UUID>,
    InsertUpdateRepository<BehandlingBarn> {
    fun findByBehandlingId(behandlingId: UUID): List<BehandlingBarn>

    fun deleteByBehandlingId(behandlingId: UUID)
}
