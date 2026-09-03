package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.grunn.og.hjelpestonad.felles.InsertUpdateRepository
import no.nav.grunn.og.hjelpestonad.felles.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface VilkårVurderingRepository :
    RepositoryInterface<VilkårVurdering, UUID>,
    InsertUpdateRepository<VilkårVurdering> {
    fun findByBehandlingId(behandlingId: UUID): List<VilkårVurdering>

    fun findByBehandlingIdAndVilkårType(
        behandlingId: UUID,
        vilkårType: VilkårType,
    ): VilkårVurdering?
}
