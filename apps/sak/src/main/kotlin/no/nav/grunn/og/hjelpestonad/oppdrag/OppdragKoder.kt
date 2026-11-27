package no.nav.grunn.og.hjelpestonad.oppdrag

import no.nav.grunn.og.hjelpestonad.oppdrag.utbetalingsgenerator.domain.Fagsystem
import no.nav.grunn.og.hjelpestonad.oppdrag.utbetalingsgenerator.domain.Utbetalingsperiode
import no.nav.grunn.og.hjelpestonad.oppdrag.utbetalingsgenerator.domain.Ytelsestype

enum class GrunnOgHjelpYtelsestype(
    override val klassifisering: String,
    override val satsType: Utbetalingsperiode.SatsType,
) : Ytelsestype {
    GRUNNSTØNAD("GHGSORMG", Utbetalingsperiode.SatsType.MND),
}

/**
 * IT28 og GHGSORMG er midlertidige koder for dev (ADR-0011 og ADR-0019).
 * De byttes når teamet som har ansvar for Oppdrag leverer nye koder, estimert rundt januar 2027.
 */
enum class GrunnOgHjelpFagsystem(
    override val kode: String,
    override val gyldigeSatstyper: Set<Ytelsestype>,
) : Fagsystem {
    IT28("IT28", setOf(GrunnOgHjelpYtelsestype.GRUNNSTØNAD)),
}
