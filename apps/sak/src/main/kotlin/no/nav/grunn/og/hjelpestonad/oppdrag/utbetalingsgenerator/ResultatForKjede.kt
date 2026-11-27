package no.nav.grunn.og.hjelpestonad.oppdrag.utbetalingsgenerator

import no.nav.grunn.og.hjelpestonad.oppdrag.utbetalingsgenerator.domain.AndelData
import java.time.YearMonth

internal data class ResultatForKjede(
    val beståendeAndeler: List<AndelData>,
    val nyeAndeler: List<AndelData>,
    val opphørsandel: Pair<AndelData, YearMonth>?,
    val sistePeriodeId: Long,
)
