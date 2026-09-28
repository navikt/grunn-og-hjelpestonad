package no.nav.grunn.og.hjelpestonad.infotrygd.sak

import java.time.LocalDate

data class InfotrygdSakerResponse(
    val saker: List<InfotrygdSakResponse>,
)

/** Kodene er lagret slik de står i Infotrygd, uten utfylling med mellomrom. */
data class InfotrygdSakResponse(
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

data class SaktreffResponse(
    val personident: String,
    val kapittelnr: String,
    val valg: String,
)
