package no.nav.grunn.og.hjelpestonad.infotrygd.util

import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val logger = LoggerFactory.getLogger("no.nav.grunn.og.hjelpestonad.infotrygd.util.InfotrygdFormat")

private val REVERSERT_FNR = """(\d\d)(\d\d)(\d\d)(\d{5})""".toRegex()

/** SA_*- og EV_*-tabellene lagrer F_NR som ÅÅMMDDPPPPP. Funksjonen går begge veier. */
fun String.reverserFnr(): String {
    val (a, b, c, personnummer) =
        requireNotNull(REVERSERT_FNR.matchEntire(this)) { "Ugyldig personident" }.destructured
    return "$c$b$a$personnummer"
}

private val SA_DATO = DateTimeFormatter.ofPattern("ddMMyyyy")

/**
 * Datoer i SA_*-tabellene lagres som tall på formen DDMMÅÅÅÅ, der 0 betyr ingen dato. Noen datoer i
 * Infotrygd er ugyldige fordi de ble ført uten validering, og de gir også null.
 */
fun datoFraSaTabell(verdi: Int): LocalDate? {
    if (verdi == 0) return null
    return try {
        LocalDate.parse("%08d".format(verdi), SA_DATO)
    } catch (e: Exception) {
        logger.warn("Kunne ikke lese dato fra Infotrygd: '$verdi'")
        null
    }
}
