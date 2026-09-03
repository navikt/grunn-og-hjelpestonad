package no.nav.gjenlevende.bs.sak.endringshistorikk

import no.nav.gjenlevende.bs.sak.felles.InsertUpdateRepository
import no.nav.gjenlevende.bs.sak.felles.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BehandlingEndringRepository :
    RepositoryInterface<BehandlingEndring, UUID>,
    InsertUpdateRepository<BehandlingEndring> {
    @Query("SELECT * FROM behandling_endring WHERE behandling_id = :behandlingId ORDER BY utfort_tid DESC")
    fun finnAlleForBehandling(behandlingId: UUID): List<BehandlingEndring>

    @Query("SELECT * FROM behandling_endring WHERE behandling_id = :behandlingId ORDER BY utfort_tid DESC LIMIT 1")
    fun finnSisteForBehandling(behandlingId: UUID): BehandlingEndring?

    @Query("SELECT * FROM behandling_endring WHERE behandling_id = :behandlingId AND endring_type = :endringType ORDER BY utfort_tid DESC LIMIT 1")
    fun finnSisteForBehandlingMedType(
        behandlingId: UUID,
        endringType: EndringType,
    ): BehandlingEndring?
}
