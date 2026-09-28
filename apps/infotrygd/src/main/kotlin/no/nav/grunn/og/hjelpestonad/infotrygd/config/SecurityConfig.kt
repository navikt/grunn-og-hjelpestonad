package no.nav.grunn.og.hjelpestonad.infotrygd.config

import no.nav.grunn.og.hjelpestonad.infotrygd.security.AzureJwtAuthenticationConverter
import no.nav.grunn.og.hjelpestonad.infotrygd.security.Rolle
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
open class SecurityConfig(
    private val jwtAuthenticationConverter: AzureJwtAuthenticationConverter,
) {
    @Bean
    open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/internal/**",
                        "/actuator/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                    ).permitAll()
                    .anyRequest()
                    .hasAnyRole(*Rolle.entries.map { it.name }.toTypedArray())
            }.oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)
                }
            }.csrf { it.disable() }

        return http.build()
    }
}
