package no.nav.grunn.og.hjelpestonad.infotrygd.tilgangskontroll

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class Auditlogg {
    private val logger = LoggerFactory.getLogger("auditLogger")

    fun forespørselOmTilgang(
        saksbehandler: String,
        personidenter: Set<String>,
        oppslag: Oppslag,
        utfall: Tilgangsutfall,
    ) {
        logger.info(
            "{} ba om tilgang til fnr: {} for å {}. Utfall: {}",
            saksbehandler,
            personidenter.joinToString(", "),
            oppslag.tekst,
            utfall.tekst,
        )
    }
}

enum class Oppslag(
    val tekst: String,
) {
    HENTE_GRUNNSTØNADER("se saker om grunnstønad med vedtak, trygdetid, sats og evaluering i Infotrygd"),
    HENTE_HJELPESTØNADER("se saker om hjelpestønad med vedtak, trygdetid, sats og evaluering i Infotrygd"),
    SJEKKE_OM_FINNES_I_INFOTRYGD("sjekke om personen har vedtak eller saker i Infotrygd"),
    HENTE_SAKER("se saker i Infotrygd"),
}

enum class Tilgangsutfall(
    val tekst: String,
    val cef: String,
) {
    TILGANG("tilgang", "Permit"),
    AVSLAG("avslag", "Deny"),
    FEIL("feil", "Deny"),
}
