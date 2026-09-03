package no.nav.gjenlevende.bs.sak.vilkår

import no.nav.gjenlevende.bs.sak.felles.InsertUpdateRepository
import no.nav.gjenlevende.bs.sak.felles.RepositoryInterface
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
