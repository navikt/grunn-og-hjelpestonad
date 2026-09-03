package no.nav.gjenlevende.bs.sak.felles.sikkerhet

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

object SikkerhetContext {
    private const val SYSTEM_FORKORTELSE = "VL"
    private const val NAVIDENT_CLAIM = "NAVident"

    fun hentSaksbehandlerEllerSystembruker(): String =
        Result
            .runCatching {
                val authentication = SecurityContextHolder.getContext().authentication

                if (authentication is JwtAuthenticationToken) {
                    authentication.token.getClaimAsString(NAVIDENT_CLAIM) ?: SYSTEM_FORKORTELSE
                } else {
                    SYSTEM_FORKORTELSE
                }
            }.getOrElse { SYSTEM_FORKORTELSE }

    fun hentSaksbehandler(): String {
        val result = hentSaksbehandlerEllerSystembruker()

        if (result == SYSTEM_FORKORTELSE) {
            error("Finner ikke NAVident i token")
        }
        return result
    }

    fun hentBrukerToken(): String {
        val authentication = SecurityContextHolder.getContext().authentication

        if (authentication is JwtAuthenticationToken) {
            return authentication.token.tokenValue
        }

        error("Finner ikke brukertoken i security context")
    }

    fun erMaskinTilMaskinToken(): Boolean = hentSaksbehandlerEllerSystembruker() == SYSTEM_FORKORTELSE
}
