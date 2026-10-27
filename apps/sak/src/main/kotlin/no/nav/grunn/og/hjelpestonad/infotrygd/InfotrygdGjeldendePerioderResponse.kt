package no.nav.grunn.og.hjelpestonad.infotrygd

data class InfotrygdGjeldendePerioderResponse(
    val perioder: List<GjeldendeInfotrygdPeriode>,
    val diagnosetekster: Map<String, String>,
)

fun List<GjeldendeInfotrygdPeriode>.tilInfotrygdGjeldendePerioderResponse(): InfotrygdGjeldendePerioderResponse =
    InfotrygdGjeldendePerioderResponse(
        perioder = this,
        diagnosetekster = diagnosetekster(map { it.vedtak }),
    )
