package no.nav.gjenlevende.bs.sak.brev

import no.nav.gjenlevende.bs.sak.felles.InsertUpdateRepository
import no.nav.gjenlevende.bs.sak.felles.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BrevRepository :
    RepositoryInterface<Brev, UUID>,
    InsertUpdateRepository<Brev>
