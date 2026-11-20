package no.nav.grunn.og.hjelpestonad.prosessering

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
@Profile("dev & !prod & !local-mock")
class ProsesseringTilgang(
    @param:Value("\${prosessering.rolle}") private val prosesseringRolle: String,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        require(prosesseringRolle.isNotBlank()) { "prosessering.rolle må være satt" }
    }

    fun harRolleProsessering(authentication: Authentication): String {
        if (authentication !is JwtAuthenticationToken || !authentication.isAuthenticated) {
            avvis("Mangler autentisert brukertoken")
        }
        val claims = authentication.token.claims
        val subjekt = claims["sub"] as? String
        val objektId = claims["oid"] as? String
        val delegerteTilganger = claims["scp"] as? String
        if (
            claims["idtyp"] == "app" ||
            subjekt.isNullOrBlank() ||
            objektId.isNullOrBlank() ||
            subjekt == objektId ||
            delegerteTilganger.isNullOrBlank()
        ) {
            avvis("Systembruker eller token uten delegert brukertilgang")
        }
        val navIdent = claims["NAVident"] as? String
        if (navIdent.isNullOrBlank()) {
            avvis("Brukertoken mangler NAVident")
        }
        val grupper = claims["groups"] as? List<*>
        if (grupper == null || grupper.any { it !is String } || prosesseringRolle !in grupper) {
            avvis("Bruker mangler prosesseringsgruppen")
        }
        return navIdent
    }

    private fun avvis(aarsak: String): Nothing {
        logger.warn("Prosesserings-API avviste kall: {}", aarsak)
        throw AccessDeniedException("Du har ikke tilgang til prosesserings-API-et")
    }
}
