package no.nav.grunn.og.hjelpestonad.vilkår.diagnose

import no.nav.grunn.og.hjelpestonad.felles.InsertUpdateRepository
import no.nav.grunn.og.hjelpestonad.felles.periode.PeriodeMedBegrunnelseRepository
import org.springframework.stereotype.Repository

@Repository
interface VilkårDiagnoseRepository :
    PeriodeMedBegrunnelseRepository<VilkårDiagnose>,
    InsertUpdateRepository<VilkårDiagnose>
