package no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter

import no.nav.grunn.og.hjelpestonad.felles.InsertUpdateRepository
import no.nav.grunn.og.hjelpestonad.felles.periode.PeriodeMedBegrunnelseRepository
import org.springframework.stereotype.Repository

@Repository
interface VilkårEkstrautgifterRepository :
    PeriodeMedBegrunnelseRepository<VilkårEkstrautgifter>,
    InsertUpdateRepository<VilkårEkstrautgifter>
