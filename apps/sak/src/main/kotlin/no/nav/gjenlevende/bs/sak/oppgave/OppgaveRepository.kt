package no.nav.gjenlevende.bs.sak.oppgave

import no.nav.gjenlevende.bs.sak.felles.InsertUpdateRepository
import no.nav.gjenlevende.bs.sak.felles.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OppgaveRepository :
    RepositoryInterface<Oppgave, UUID>,
    InsertUpdateRepository<Oppgave> {
    @Query(
        """
        SELECT * FROM oppgave 
        WHERE behandling_id = :behandlingId 
        AND type = :type 
        ORDER BY opprettet_tid DESC 
        LIMIT 1
        """,
    )
    fun findByBehandlingIdAndType(
        behandlingId: UUID,
        type: String,
    ): Oppgave?

    @Query(
        """
        SELECT * FROM oppgave 
        WHERE behandling_id = :behandlingId 
        AND type IN (:types) 
        ORDER BY opprettet_tid DESC 
        LIMIT 1
        """,
    )
    fun finnSisteOppgaveForBehandling(
        behandlingId: UUID,
        types: List<String>,
    ): Oppgave?
}
