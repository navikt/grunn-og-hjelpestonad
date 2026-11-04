package no.nav.grunn.og.hjelpestonad.infotrygd.security

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

object SikkerhetContext {
    private const val NAVIDENT_CLAIM = "NAVident"
    private const val GROUPS_CLAIM = "groups"

    /** Entra setter disse i stedet for `groups` når brukeren har for mange grupper (overage). */
    private val OVERAGE_CLAIMS = listOf("_claim_names", "hasgroups")

    private fun jwtAuthentication(): JwtAuthenticationToken? = SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken

    fun hentSaksbehandler(): String = jwtAuthentication()?.token?.getClaimAsString(NAVIDENT_CLAIM) ?: error("Finner ikke NAVident i token")

    fun hentBrukerToken(): String = jwtAuthentication()?.token?.tokenValue ?: error("Finner ikke brukertoken i security context")

    /**
     * Gruppene i tokenet. Kaster feil når claimen mangler eller Entra har sendt overage, aldri tom liste.
     * En tom liste ville sett ut som «ingen sperrede grupper» og åpnet sperren i [SperretGruppeIDev].
     */
    fun hentGrupper(): List<String> {
        val token = jwtAuthentication()?.token ?: error("Finner ikke token i security context")
        check(OVERAGE_CLAIMS.none { token.hasClaim(it) }) { "Tokenet har ikke hele gruppelisten (overage)" }
        return token.getClaimAsStringList(GROUPS_CLAIM) ?: error("Finner ikke $GROUPS_CLAIM i token")
    }
}
