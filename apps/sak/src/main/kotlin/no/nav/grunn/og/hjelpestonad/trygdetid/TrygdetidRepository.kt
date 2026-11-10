package no.nav.grunn.og.hjelpestonad.trygdetid

import no.nav.grunn.og.hjelpestonad.felles.InsertUpdateRepository
import no.nav.grunn.og.hjelpestonad.felles.periode.PeriodeMedBegrunnelseRepository
import org.springframework.stereotype.Repository

@Repository
interface TrygdetidRepository :
    PeriodeMedBegrunnelseRepository<Trygdetid>,
    InsertUpdateRepository<Trygdetid>
