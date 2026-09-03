package no.nav.gjenlevende.bs.sak.iverksett.brev

import no.nav.gjenlevende.bs.sak.felles.InsertUpdateRepository
import no.nav.gjenlevende.bs.sak.felles.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface JournalpostForBehandlingRepository :
    RepositoryInterface<JournalpostForBehandling, UUID>,
    InsertUpdateRepository<JournalpostForBehandling> {
    fun findAllByBehandlingId(behandlingId: UUID): List<JournalpostForBehandling>
}
