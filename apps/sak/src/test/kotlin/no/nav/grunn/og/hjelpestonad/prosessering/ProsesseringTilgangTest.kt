package no.nav.grunn.og.hjelpestonad.prosessering

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

class ProsesseringTilgangTest {
    private val prosesseringTilgang = ProsesseringTilgang()

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
    fun `tidligere midlertidig prosesseringsgruppe gir ikke tilgang`() {
        assertThatThrownBy {
            prosesseringTilgang.harRolleProsessering(brukertoken(grupper = listOf("928636f4-fd0d-4149-978e-a6fb68bb19de")))
        }.isInstanceOf(AccessDeniedException::class.java)
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
                    .claim("groups", listOf("bbda72af-39ba-48bc-992b-341161bf21f1"))
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
    fun `prosesserings-API gir tilgang med riktig rolle uten konfigurasjon`() {
        AnnotationConfigApplicationContext(ProsesseringTilgang::class.java).use { applicationContext ->
            val prosesseringTilgangUtenKonfigurasjon = applicationContext.getBean(ProsesseringTilgang::class.java)
            assertThat(prosesseringTilgangUtenKonfigurasjon.harRolleProsessering(brukertoken())).isEqualTo("Z123456")
        }
    }

    @Test
    fun `andre applikasjonsroller gir ikke prosesseringstilgang`() {
        val grupperUtenProsesseringsrolle =
            listOf(
                "7ce9d1d2-d149-4324-832b-8d459762a102",
                "84c4a287-abd6-46c1-bf93-dbf90f1a326d",
                "a181921e-2a55-4198-896b-0086cc805278",
            )
        assertThatThrownBy {
            prosesseringTilgang.harRolleProsessering(brukertoken(grupper = grupperUtenProsesseringsrolle))
        }.isInstanceOf(AccessDeniedException::class.java)
    }

    @Test
    fun `gyldig prosesseringsgruppe sammen med ugyldig gruppetype avvises`() {
        val ugyldigBrukertoken =
            JwtAuthenticationToken(
                Jwt
                    .withTokenValue("test")
                    .header("alg", "RS256")
                    .claims { it.putAll(brukertoken().token.claims) }
                    .claim("groups", listOf("bbda72af-39ba-48bc-992b-341161bf21f1", 42))
                    .build(),
                emptyList(),
            )
        assertThatThrownBy { prosesseringTilgang.harRolleProsessering(ugyldigBrukertoken) }
            .isInstanceOf(AccessDeniedException::class.java)
    }

    private fun brukertoken(
        grupper: List<String> = listOf("bbda72af-39ba-48bc-992b-341161bf21f1"),
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
