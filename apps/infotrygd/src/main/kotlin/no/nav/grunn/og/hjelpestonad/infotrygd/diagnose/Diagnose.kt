package no.nav.grunn.og.hjelpestonad.infotrygd.diagnose

import no.nav.grunn.og.hjelpestonad.infotrygd.stønad.Stønadstype
import no.nav.grunn.og.hjelpestonad.infotrygd.util.PersonidentValidator
import java.time.LocalDateTime
import java.time.YearMonth

/** @param stønadstyper tom betyr alle stønadstyper */
data class DiagnoseRequest(
    val personidenter: Set<String>,
    val stønadstyper: Set<Stønadstype> = emptySet(),
) {
    fun valider() {
        require(personidenter.isNotEmpty()) { "Må oppgi minst én personident" }
        personidenter.forEach(PersonidentValidator::validerPersonident)
    }
}

data class DiagnoserResponse(
    val diagnoser: List<InfotrygdDiagnose>,
)

data class InfotrygdDiagnose(
    val personident: String,
    val stønadstype: Stønadstype,
    /** Måneden diagnosen ble notert i Infotrygd (999999 − ev05_aar_mnd_seq). */
    val månedDiagnoseNotertIInfotrygd: YearMonth,
    /** De fire første tegnene i ev10_text. */
    val diagnosekode: String,
    val datoEndretIKilde: LocalDateTime,
)
