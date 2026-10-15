package no.nav.grunn.og.hjelpestonad.infotrygd.security

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

object SikkerhetContext {
    private const val NAVIDENT_CLAIM = "NAVident"

    private fun jwtAuthentication(): JwtAuthenticationToken? = SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken

    fun hentSaksbehandler(): String = jwtAuthentication()?.token?.getClaimAsString(NAVIDENT_CLAIM) ?: error("Finner ikke NAVident i token")

    fun hentBrukerToken(): String = jwtAuthentication()?.token?.tokenValue ?: error("Finner ikke brukertoken i security context")
}
