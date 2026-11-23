package no.nav.grunn.og.hjelpestonad.prosessering

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

class ProsesseringTilgangTest {
    private val prosesseringTilgang = ProsesseringTilgang("928636f4-fd0d-4149-978e-a6fb68bb19de")

    @Test
    fun `ansatt med prosesseringsgruppen får tilgang`() {
        assertThat(prosesseringTilgang.harRolleProsessering(brukertoken())).isEqualTo("Z123456")
    }

    @Test
    fun `ansatt uten prosesseringsgruppen avvises`() {
        assertThatThrownBy { prosesseringTilgang.harRolleProsessering(brukertoken(grupper = emptyList())) }
            .isInstanceOf(AccessDeniedException::class.java)
    }

    @Test
    fun `maskintoken med gruppen avvises`() {
        val maskintoken =
            JwtAuthenticationToken(
                Jwt
                    .withTokenValue("ikke-et-ekte-token")
                    .header("alg", "RS256")
                    .subject("maskinklient")
                    .claim("oid", "maskinklient")
                    .claim("idtyp", "app")
                    .claim("groups", listOf("928636f4-fd0d-4149-978e-a6fb68bb19de"))
                    .build(),
                emptyList(),
            )
        assertThatThrownBy { prosesseringTilgang.harRolleProsessering(maskintoken) }
            .isInstanceOf(AccessDeniedException::class.java)
    }

    @Test
    fun `brukertoken uten NAVident avvises`() {
        assertThatThrownBy { prosesseringTilgang.harRolleProsessering(brukertoken(navIdent = null)) }
            .isInstanceOf(AccessDeniedException::class.java)
    }

    @Test
    fun `systembruker avvises uavhengig av NAVident og grupper`() {
        val systembrukerClaims =
            listOf(
                mapOf("idtyp" to "app"),
                mapOf("sub" to "samme-id", "oid" to "samme-id"),
                mapOf("scp" to ""),
                mapOf("oid" to ""),
                mapOf("sub" to ""),
            )
        for (claims in systembrukerClaims) {
            val systembruker =
                JwtAuthenticationToken(
                    Jwt
                        .withTokenValue("test")
                        .header("alg", "RS256")
                        .claims {
                            it.putAll(brukertoken().token.claims)
                            it.putAll(claims)
                        }.build(),
                    emptyList(),
                )
            assertThatThrownBy { prosesseringTilgang.harRolleProsessering(systembruker) }
                .isInstanceOf(AccessDeniedException::class.java)
        }
    }

    @Test
    fun `manglende claims og feil claimtyper gir avslag`() {
        for (claimNavn in listOf("sub", "oid", "scp", "NAVident", "groups")) {
            for (claimVerdi in listOf(null, 42)) {
                val ugyldigBrukertoken =
                    JwtAuthenticationToken(
                        Jwt
                            .withTokenValue("test")
                            .header("alg", "RS256")
                            .claims {
                                it.putAll(brukertoken().token.claims)
                                if (claimVerdi == null) it.remove(claimNavn) else it[claimNavn] = claimVerdi
                            }.build(),
                        emptyList(),
                    )
                assertThatThrownBy { prosesseringTilgang.harRolleProsessering(ugyldigBrukertoken) }
                    .isInstanceOf(AccessDeniedException::class.java)
            }
        }
    }

    @Test
    fun `annen autentisering og uautentisert JWT avvises`() {
        val uautentisertBrukertoken = brukertoken().apply { isAuthenticated = false }
        for (authentication in listOf(UsernamePasswordAuthenticationToken("ansatt", "ikke-brukt"), uautentisertBrukertoken)) {
            assertThatThrownBy { prosesseringTilgang.harRolleProsessering(authentication) }
                .isInstanceOf(AccessDeniedException::class.java)
        }
    }

    @Test
    fun `manglende rollekonfigurasjon gir ikke tilgang`() {
        for (prosesseringRolle in listOf("", " ")) {
            assertThatThrownBy { ProsesseringTilgang(prosesseringRolle).harRolleProsessering(brukertoken()) }
                .isInstanceOf(AccessDeniedException::class.java)
        }
    }

    @Test
    fun `gruppen leses fra konfigurasjon`() {
        val annenProsesseringTilgang = ProsesseringTilgang("annen-gruppe")
        assertThat(annenProsesseringTilgang.harRolleProsessering(brukertoken(grupper = listOf("annen-gruppe"))))
            .isEqualTo("Z123456")
        assertThatThrownBy { annenProsesseringTilgang.harRolleProsessering(brukertoken()) }
            .isInstanceOf(AccessDeniedException::class.java)
    }

    private fun brukertoken(
        grupper: List<String> = listOf("928636f4-fd0d-4149-978e-a6fb68bb19de"),
        navIdent: String? = "Z123456",
    ): JwtAuthenticationToken =
        JwtAuthenticationToken(
            Jwt
                .withTokenValue("ikke-et-ekte-token")
                .header("alg", "RS256")
                .subject("bruker-subjekt")
                .claim("oid", "bruker-objekt")
                .claim("scp", "access_as_user")
                .claim("groups", grupper)
                .apply { if (navIdent != null) claim("NAVident", navIdent) }
                .build(),
            emptyList(),
        )
}
