package no.nav.grunn.og.hjelpestonad.prosessering

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

@Component
@Profile("dev & !prod & !local-mock")
class ProsesseringTilgang {
    @Suppress("UNUSED_PARAMETER")
    fun krevTilgang(authentication: Authentication): String {
        // TODO: Skriv selv: krev brukertoken, NAVident og prosessering.rolle. Avvis maskintoken.
        LoggerFactory.getLogger(javaClass).warn("Prosesserings-API avviste kall: tilgangskontrollen er ikke implementert")
        throw AccessDeniedException("Prosesserings-API er stengt inntil tilgangskontrollen er implementert")
    }
}
