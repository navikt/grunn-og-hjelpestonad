package no.nav.grunn.og.hjelpestonad.prosessering

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.intercept.RequestAuthorizationContext

@Configuration
class ProsesseringSecurityConfig(
    private val prosesseringTilgang: ProsesseringTilgang,
) {
    @Bean
    @Order(1)
    fun prosesseringSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/api/prosessering/**")
            .authorizeHttpRequests { it.anyRequest().access(harTilgangTilProsessering()) }
            .oauth2ResourceServer { it.jwt {} }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .csrf { it.disable() }
        return http.build()
    }

    private fun harTilgangTilProsessering(): AuthorizationManager<RequestAuthorizationContext> =
        AuthorizationManager { authenticationSupplier, _ ->
            val authentication = authenticationSupplier.get()
            try {
                prosesseringTilgang.harRolleProsessering(authentication)
                AuthorizationDecision(true)
            } catch (_: AccessDeniedException) {
                AuthorizationDecision(false)
            }
        }
}
