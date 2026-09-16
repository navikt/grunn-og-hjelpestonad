package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.grunn.og.hjelpestonad.felles.InsertUpdateRepository
import no.nav.grunn.og.hjelpestonad.felles.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface VilkårDiagnoseRepository :
    RepositoryInterface<VilkårDiagnose, UUID>,
    InsertUpdateRepository<VilkårDiagnose> {
    fun findByVilkårVurderingId(vilkårVurderingId: UUID): List<VilkårDiagnose>
}
