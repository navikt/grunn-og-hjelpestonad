package no.nav.grunn.og.hjelpestonad.prosessering

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

class ProsesseringTilgangTest {
    private val prosesseringTilgang = ProsesseringTilgang()

    @Test
    fun `stubben nekter tilgang selv med riktig gruppe`() {
        assertThatThrownBy { prosesseringTilgang.krevTilgang(brukertoken()) }
            .isInstanceOf(AccessDeniedException::class.java)
    }

    @Disabled("Rød sone: aktiver når tilgangskontrollen er implementert og gjennomgått")
    @Test
    fun `ansatt med prosesseringsgruppen får tilgang`() {
        assertThat(prosesseringTilgang.krevTilgang(brukertoken())).isEqualTo("Z123456")
    }

    @Disabled("Rød sone: aktiver når tilgangskontrollen er implementert og gjennomgått")
    @Test
    fun `ansatt uten prosesseringsgruppen avvises`() {
        assertThatThrownBy { prosesseringTilgang.krevTilgang(brukertoken(grupper = emptyList())) }
            .isInstanceOf(AccessDeniedException::class.java)
    }

    @Disabled("Rød sone: aktiver når tilgangskontrollen er implementert og gjennomgått")
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
            )
        assertThatThrownBy { prosesseringTilgang.krevTilgang(maskintoken) }
            .isInstanceOf(AccessDeniedException::class.java)
    }

    @Disabled("Rød sone: aktiver når tilgangskontrollen er implementert og gjennomgått")
    @Test
    fun `brukertoken uten NAVident avvises`() {
        assertThatThrownBy { prosesseringTilgang.krevTilgang(brukertoken(navIdent = null)) }
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
        )
}
