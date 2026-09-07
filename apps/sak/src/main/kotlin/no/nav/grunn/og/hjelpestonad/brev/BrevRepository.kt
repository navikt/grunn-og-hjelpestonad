package no.nav.grunn.og.hjelpestonad.brev

import no.nav.grunn.og.hjelpestonad.felles.InsertUpdateRepository
import no.nav.grunn.og.hjelpestonad.felles.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BrevRepository :
    RepositoryInterface<Brev, UUID>,
    InsertUpdateRepository<Brev>
