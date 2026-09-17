package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.grunn.og.hjelpestonad.felles.InsertUpdateRepository
import no.nav.grunn.og.hjelpestonad.felles.RepositoryInterface
import org.springframework.data.repository.NoRepositoryBean
import java.util.UUID

@NoRepositoryBean
interface VilkårPeriodeRepository<T : VilkårPeriode<T>> :
    RepositoryInterface<T, UUID>,
    InsertUpdateRepository<T> {
    fun findByBehandlingId(behandlingId: UUID): List<T>
}
