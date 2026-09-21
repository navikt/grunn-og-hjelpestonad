package no.nav.grunn.og.hjelpestonad.vilkår.diagnose

import no.nav.grunn.og.hjelpestonad.felles.InsertUpdateRepository
import no.nav.grunn.og.hjelpestonad.vilkår.VilkårPeriodeRepository
import org.springframework.stereotype.Repository

@Repository
interface VilkårDiagnoseRepository :
    VilkårPeriodeRepository<VilkårDiagnose>,
    InsertUpdateRepository<VilkårDiagnose>
