package no.nav.grunn.og.hjelpestonad.vilkår.medlemskap

import no.nav.grunn.og.hjelpestonad.felles.InsertUpdateRepository
import no.nav.grunn.og.hjelpestonad.vilkår.VilkårPeriodeRepository
import org.springframework.stereotype.Repository

@Repository
interface VilkårMedlemskapRepository :
    VilkårPeriodeRepository<VilkårMedlemskap>,
    InsertUpdateRepository<VilkårMedlemskap>
