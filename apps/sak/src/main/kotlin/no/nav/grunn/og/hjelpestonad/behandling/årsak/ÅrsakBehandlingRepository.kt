package no.nav.grunn.og.hjelpestonad.behandling.årsak

import no.nav.grunn.og.hjelpestonad.felles.InsertUpdateRepository
import no.nav.grunn.og.hjelpestonad.felles.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ÅrsakBehandlingRepository :
    RepositoryInterface<ÅrsakBehandling, UUID>,
    InsertUpdateRepository<ÅrsakBehandling>
