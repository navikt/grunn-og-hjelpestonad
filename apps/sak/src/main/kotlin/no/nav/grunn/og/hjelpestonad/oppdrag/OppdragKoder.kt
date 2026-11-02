package no.nav.grunn.og.hjelpestonad.oppdrag

import no.nav.familie.felles.utbetalingsgenerator.domain.Fagsystem
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode
import no.nav.familie.felles.utbetalingsgenerator.domain.Ytelsestype

enum class GrunnOgHjelpYtelsestype(
    override val klassifisering: String,
    override val satsType: Utbetalingsperiode.SatsType,
) : Ytelsestype {
    GRUNNSTØNAD("GH", Utbetalingsperiode.SatsType.MND),
}

/**
 * IT28 og GH er midlertidige koder for dev (ADR-0011, `tmp/todo-it28-oppdrag.md`).
 * De byttes når økonomi gir prod-koder.
 */
enum class GrunnOgHjelpFagsystem(
    override val kode: String,
    override val gyldigeSatstyper: Set<Ytelsestype>,
) : Fagsystem {
    IT28("IT28", setOf(GrunnOgHjelpYtelsestype.GRUNNSTØNAD)),
}
