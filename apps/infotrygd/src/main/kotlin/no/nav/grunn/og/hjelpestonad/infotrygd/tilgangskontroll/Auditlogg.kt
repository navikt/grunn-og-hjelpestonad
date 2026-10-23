package no.nav.grunn.og.hjelpestonad.infotrygd.tilgangskontroll

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Auditlogg for oppslag mot tilgangsmaskinen. Loggen går til team logs i dev.
 * Skal erstattes av ArcSight (CEF) før prod, se ADR-0009.
 */
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

/** [cef] er verdien i `flexString1` (Decision) når auditloggen flyttes til ArcSight. */
enum class Tilgangsutfall(
    val tekst: String,
    val cef: String,
) {
    TILGANG("tilgang", "Permit"),
    AVSLAG("avslag", "Deny"),
    FEIL("feil", "Deny"),
}
