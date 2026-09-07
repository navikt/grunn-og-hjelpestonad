package no.nav.grunn.og.hjelpestonad.iverksett.utbetaling

import no.nav.grunn.og.hjelpestonad.felles.InsertUpdateRepository
import no.nav.grunn.og.hjelpestonad.felles.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SimuleringRepository :
    RepositoryInterface<Simulering, UUID>,
    InsertUpdateRepository<Simulering>
