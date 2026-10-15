package no.nav.grunn.og.hjelpestonad.infotrygd

import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.DiagnoseRequest

data class PersonidentRequest(
    val personident: String,
)

/** @param stønadstyper tom betyr alle stønadstyper */
data class InfotrygdDiagnoseRequest(
    val personident: String,
    val stønadstyper: Set<DiagnoseRequest.Stønadstyper> = emptySet(),
)
