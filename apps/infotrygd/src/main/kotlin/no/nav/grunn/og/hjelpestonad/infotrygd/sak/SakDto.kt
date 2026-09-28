package no.nav.grunn.og.hjelpestonad.infotrygd.sak

import no.nav.grunn.og.hjelpestonad.infotrygd.util.PersonidentValidator
import java.time.LocalDate

data class PersonidenterRequest(
    val personidenter: Set<String>,
) {
    fun valider() {
        require(personidenter.isNotEmpty()) { "Må oppgi minst én personident" }
        personidenter.forEach(PersonidentValidator::validerPersonident)
    }
}

data class InfotrygdSakResponse(
    val saker: List<InfotrygdSak>,
)

/** Kodene er lagret slik de står i Infotrygd, uten utfylling med mellomrom. */
data class InfotrygdSak(
    val personident: String,
    val id: Long,
    val saksnr: String,
    val saksblokk: String,
    val registrertDato: LocalDate?,
    val mottattDato: LocalDate?,
    val kapittelnr: String,
    val valg: String,
    val undervalg: String,
    val type: String,
    val nivå: String,
    val resultat: String,
    val vedtaksdato: LocalDate?,
    val iverksattdato: LocalDate?,
    val årsakskode: String,
    val behandlendeEnhet: String,
    val registrertAvEnhet: String,
    val tkNr: String,
    val region: String,
)

data class Saktreff(
    val personident: String,
    val kapittelnr: String,
    val valg: String,
)
