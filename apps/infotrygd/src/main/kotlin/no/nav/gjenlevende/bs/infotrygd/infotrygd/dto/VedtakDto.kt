package no.nav.gjenlevende.bs.infotrygd.infotrygd.dto

import java.time.LocalDate

enum class StønadType(
    val kodeRutine: String,
) {
    BARNETILSYN("GB"),
    SKOLEPENGER("GU"),
}

data class VedtakPeriodeRequest(
    val personident: String,
)

data class BarnInfo(
    val personLøpenummer: Long,
    val fom: LocalDate,
    val tom: LocalDate?,
)

data class PeriodeResponse(
    val stønadType: StønadType,
    val fom: LocalDate,
    val tom: LocalDate?,
    val vedtakId: Long,
    val stønadId: Long,
    val barn: List<BarnInfo> = emptyList(),
)

data class VedtakPeriodeResponse(
    val personident: String,
    val barnetilsyn: List<PeriodeResponse> = emptyList(),
    val skolepenger: List<PeriodeResponse> = emptyList(),
) {
    val harPerioder: Boolean
        get() = barnetilsyn.isNotEmpty() || skolepenger.isNotEmpty()
}
