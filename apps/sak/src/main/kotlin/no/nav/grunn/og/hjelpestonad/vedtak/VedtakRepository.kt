package no.nav.grunn.og.hjelpestonad.vedtak

import no.nav.grunn.og.hjelpestonad.felles.InsertUpdateRepository
import no.nav.grunn.og.hjelpestonad.felles.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface VedtakRepository :
    RepositoryInterface<Vedtak, UUID>,
    InsertUpdateRepository<Vedtak> {
    fun findByBehandlingId(behandlingId: UUID): Vedtak?

    fun deleteByBehandlingId(behandlingId: UUID)
}
