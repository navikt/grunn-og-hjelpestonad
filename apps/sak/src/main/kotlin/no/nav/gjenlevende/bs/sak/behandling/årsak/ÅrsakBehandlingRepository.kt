package no.nav.gjenlevende.bs.sak.behandling.årsak

import no.nav.gjenlevende.bs.sak.felles.InsertUpdateRepository
import no.nav.gjenlevende.bs.sak.felles.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ÅrsakBehandlingRepository :
    RepositoryInterface<ÅrsakBehandling, UUID>,
    InsertUpdateRepository<ÅrsakBehandling>
