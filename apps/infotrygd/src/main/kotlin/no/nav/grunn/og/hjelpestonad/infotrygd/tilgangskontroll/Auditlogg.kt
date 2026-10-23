package no.nav.grunn.og.hjelpestonad.infotrygd.tilgangskontroll

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class Auditlogg {
    private val logger = LoggerFactory.getLogger("auditLogger")

    fun forespørselOmTilgang(
        saksbehandler: String,
        personidenter: Set<String>,
        utfall: Tilgangsutfall,
    ) {
        logger.info(
            "{} ba om tilgang til fnr: {}. Utfall: {}",
            saksbehandler,
            personidenter.joinToString(", "),
            utfall.tekst,
        )
    }
}

enum class Tilgangsutfall(
    val tekst: String,
    val cef: String,
) {
    TILGANG("tilgang", "Permit"),
    AVSLAG("avslag", "Deny"),
    FEIL("feil", "Deny"),
}
