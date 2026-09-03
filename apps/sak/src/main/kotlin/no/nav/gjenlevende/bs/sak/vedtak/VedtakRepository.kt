package no.nav.gjenlevende.bs.sak.vedtak

import no.nav.gjenlevende.bs.sak.felles.InsertUpdateRepository
import no.nav.gjenlevende.bs.sak.felles.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface VedtakRepository :
    RepositoryInterface<Vedtak, UUID>,
    InsertUpdateRepository<Vedtak> {
    fun findByBehandlingId(behandlingId: UUID): Vedtak?

    fun deleteByBehandlingId(behandlingId: UUID)
}
