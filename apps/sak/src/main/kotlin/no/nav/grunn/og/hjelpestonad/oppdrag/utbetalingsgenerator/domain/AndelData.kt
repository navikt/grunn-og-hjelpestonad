package no.nav.grunn.og.hjelpestonad.oppdrag.utbetalingsgenerator.domain

import java.time.YearMonth

/** Midlertidig kopi fra navikt/familie-utbetalingsgenerator, commit 384875f (PR #193). */
data class AndelData(
    val id: String,
    val fom: YearMonth,
    val tom: YearMonth?,
    val beløp: Int,
    val personIdent: String,
    val type: Ytelsestype,
    val periodeId: Long?,
    val forrigePeriodeId: Long?,
    val kildeBehandlingId: String?,
    val utbetalingsgrad: Int? = null,
)

fun AndelData.representererSammePeriodeSom(andelData: AndelData): Boolean = this.periodeId == andelData.periodeId && this.forrigePeriodeId == andelData.forrigePeriodeId && this.fom == andelData.fom

interface Ytelsestype {
    val satsType: Utbetalingsperiode.SatsType
    val klassifisering: String
}

internal fun List<AndelData>.uten0beløp(): List<AndelData> = this.filter { it.beløp != 0 }

internal fun List<AndelData>.groupByIdentOgType(): Map<IdentOgType, List<AndelData>> = groupBy { IdentOgType(it.personIdent, it.type) }.mapValues { it.value.sortedBy { it.fom } }
