package no.nav.grunn.og.hjelpestonad.infotrygd.util

import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val logger = LoggerFactory.getLogger("no.nav.grunn.og.hjelpestonad.infotrygd.util.InfotrygdFormat")

private val REVERSERT_FNR = """(\d\d)(\d\d)(\d\d)(\d{5})""".toRegex()

/**
 * F_NR i sa_sak_10, sa_status_15, sa_hendelse_20 og ev_data_10 lagres som ÅÅMMDDPPPPP, og
 * S01_PERSONKEY/EV01_PERSONKEY er TK_NR fulgt av samme reverserte fnr. T_LOPENR_FNR.PERSONNR er vanlig
 * fnr. Funksjonen går begge veier.
 */
fun String.reverserFnr(): String {
    val (a, b, c, personnummer) =
        requireNotNull(REVERSERT_FNR.matchEntire(this)) { "Ugyldig personident" }.destructured
    return "$c$b$a$personnummer"
}

private val SA_DATO = DateTimeFormatter.ofPattern("ddMMyyyy")

/**
 * Leser datoer lagret som tall på formen DDMMÅÅÅÅ, der 0 betyr ingen dato. Noen datoer i Infotrygd er
 * ugyldige fordi de ble ført uten validering, og de gir også null.
 *
 * Formatet varierer mellom SA- og EV-tabellene, så sjekk kolonnen før bruk:
 * - DDMMÅÅÅÅ: s10_*dato i sa_sak_10, s20_dato_dannet i sa_hendelse_20 og ev10_mottatt_dato i ev_data_10
 * - ÅÅÅÅMMDD: s15_status_dato i sa_status_15
 * - 99999999 − ÅÅÅÅMMDD (synkende, for sortering): s20_aksjonsdato_seq i sa_hendelse_20
 * - 999999 − ÅÅÅÅMM (synkende, for sortering): ev05_aar_mnd_seq i ev_data_10
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
