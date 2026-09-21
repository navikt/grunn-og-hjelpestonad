package no.nav.grunn.og.hjelpestonad.vilkår.institusjon

import no.nav.grunn.og.hjelpestonad.felles.InsertUpdateRepository
import no.nav.grunn.og.hjelpestonad.vilkår.VilkårPeriodeRepository
import org.springframework.stereotype.Repository

@Repository
interface VilkårInstitusjonRepository :
    VilkårPeriodeRepository<VilkårInstitusjon>,
    InsertUpdateRepository<VilkårInstitusjon>
