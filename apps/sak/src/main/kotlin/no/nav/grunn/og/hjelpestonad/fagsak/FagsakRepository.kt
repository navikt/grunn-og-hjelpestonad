package no.nav.grunn.og.hjelpestonad.fagsak

import no.nav.grunn.og.hjelpestonad.fagsak.domain.Fagsak
import no.nav.grunn.og.hjelpestonad.fagsak.domain.StønadType
import no.nav.grunn.og.hjelpestonad.felles.InsertUpdateRepository
import no.nav.grunn.og.hjelpestonad.felles.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FagsakRepository :
    RepositoryInterface<Fagsak, UUID>,
    InsertUpdateRepository<Fagsak> {
    @Query("SELECT * FROM fagsak WHERE fagsak_person_id = :fagsakPersonId AND stonadstype = :stønadstype")
    fun findByFagsakPersonIdAndStønadstype(
        fagsakPersonId: UUID,
        stønadstype: StønadType,
    ): Fagsak?
}
