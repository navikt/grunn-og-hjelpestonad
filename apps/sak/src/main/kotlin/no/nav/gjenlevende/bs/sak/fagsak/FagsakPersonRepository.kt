package no.nav.gjenlevende.bs.sak.fagsak

import no.nav.gjenlevende.bs.sak.fagsak.domain.FagsakPerson
import no.nav.gjenlevende.bs.sak.fagsak.domain.Personident
import no.nav.gjenlevende.bs.sak.felles.InsertUpdateRepository
import no.nav.gjenlevende.bs.sak.felles.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FagsakPersonRepository :
    RepositoryInterface<FagsakPerson, UUID>,
    InsertUpdateRepository<FagsakPerson> {
    @Query(
        """SELECT p.* FROM fagsak_person p WHERE
                EXISTS(SELECT 1 FROM personident WHERE fagsak_person_id = p.id AND ident IN (:identer))""",
    )
    fun findByIdent(identer: Collection<String>): FagsakPerson?

    @Query("SELECT * FROM personident WHERE fagsak_person_id = :personId")
    fun findPersonidenter(personId: UUID): Set<Personident>
}
