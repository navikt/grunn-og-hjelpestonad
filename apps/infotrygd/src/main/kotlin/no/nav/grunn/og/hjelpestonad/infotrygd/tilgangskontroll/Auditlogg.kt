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
        fikkTilgang: Boolean,
    ) {
        logger.info(
            "{} ba om tilgang til fnr: {}. Svar: {}",
            saksbehandler,
            personidenter.joinToString(", "),
            if (fikkTilgang) "ja" else "nei",
        )
    }
}
