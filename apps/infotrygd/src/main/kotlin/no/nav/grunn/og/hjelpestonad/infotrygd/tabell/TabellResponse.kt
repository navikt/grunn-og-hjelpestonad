package no.nav.grunn.og.hjelpestonad.infotrygd.tabell

data class TabellKolonnerResponse(
    val tabell: String,
    val kolonner: List<String>,
)

data class TabellAntallRaderResponse(
    val tabell: String,
    val antallRader: Long,
)
