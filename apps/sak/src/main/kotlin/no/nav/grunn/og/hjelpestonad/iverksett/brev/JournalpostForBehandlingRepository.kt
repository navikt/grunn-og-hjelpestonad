package no.nav.grunn.og.hjelpestonad.iverksett.brev

import no.nav.grunn.og.hjelpestonad.felles.InsertUpdateRepository
import no.nav.grunn.og.hjelpestonad.felles.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface JournalpostForBehandlingRepository :
    RepositoryInterface<JournalpostForBehandling, UUID>,
    InsertUpdateRepository<JournalpostForBehandling> {
    fun findAllByBehandlingId(behandlingId: UUID): List<JournalpostForBehandling>
}
