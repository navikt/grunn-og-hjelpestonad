package no.nav.grunn.og.hjelpestonad.behandling

import no.nav.grunn.og.hjelpestonad.felles.InsertUpdateRepository
import no.nav.grunn.og.hjelpestonad.felles.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BehandlingRepository :
    RepositoryInterface<Behandling, UUID>,
    InsertUpdateRepository<Behandling> {
    fun findAllByFagsakId(fagsakId: UUID): List<Behandling>

    fun existsByFagsakIdAndStatusIsNot(
        fagsakId: UUID,
        behandlingStatus: BehandlingStatus,
    ): Boolean

    @Query(
        """
        SELECT * FROM behandling
        WHERE fagsak_id = :fagsakId
          AND resultat IN ('OPPHØRT', 'INNVILGET')
          AND status = 'FERDIGSTILT'
        ORDER BY opprettet_tid DESC
        LIMIT 1
    """,
    )
    fun finnSisteIverksatteBehandling(fagsakId: UUID): Behandling?

    @Query(
        """
        SELECT * FROM behandling
        WHERE fagsak_id = :fagsakId
          AND resultat IN ('OPPHØRT', 'INNVILGET')
          AND status = 'FERDIGSTILT'
        ORDER BY opprettet_tid DESC
    """,
    )
    fun finnAlleIverksatteBehandlinger(fagsakId: UUID): List<Behandling>
}
