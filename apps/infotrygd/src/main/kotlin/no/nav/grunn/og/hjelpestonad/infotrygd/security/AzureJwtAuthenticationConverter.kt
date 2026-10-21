package no.nav.grunn.og.hjelpestonad.infotrygd.security

import org.slf4j.LoggerFactory
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
class AzureJwtAuthenticationConverter : Converter<Jwt, AbstractAuthenticationToken> {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    companion object {
        private const val NAVIDENT_CLAIM = "NAVident"
        private const val GROUPS_CLAIM = "groups"
    }

    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val navIdent = jwt.getClaimAsString(NAVIDENT_CLAIM)

        if (navIdent.isNullOrBlank()) {
            val feil = ManglendeClaimException("Token mangler påkrevd claim: $NAVIDENT_CLAIM")
            logger.warn("JWT mangler påkrevd claim: $NAVIDENT_CLAIM")
            secureLogger.warn("JWT mangler påkrevd claim: $NAVIDENT_CLAIM", feil)
            throw feil
        }

        val grupper = jwt.getClaimAsStringList(GROUPS_CLAIM) ?: emptyList()

        val roller = Rolle.fraAzureGrupper(grupper)

        if (roller.isEmpty()) {
            val feil = UtilstrekkeligTilgangException("Bruker har ikke tilgang til applikasjonen. Mangler roller.")
            logger.warn("Bruker har ingen gyldige roller")
            secureLogger.warn("Bruker $navIdent har ingen gyldige roller. Grupper i token: ${grupper.joinToString(", ")}", feil)
            throw feil
        }

        val authorities = roller.map { SimpleGrantedAuthority(it.authority()) }

        logger.debug("Autentisert bruker $navIdent med roller: ${roller.joinToString(", ")}")

        return JwtAuthenticationToken(jwt, authorities)
    }
}

class ManglendeClaimException(
    message: String,
) : AuthenticationException(message)

class UtilstrekkeligTilgangException(
    message: String,
) : AuthenticationException(message)
