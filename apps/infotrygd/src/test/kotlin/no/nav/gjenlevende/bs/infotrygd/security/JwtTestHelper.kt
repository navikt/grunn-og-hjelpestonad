package no.nav.gjenlevende.bs.infotrygd.security

import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import java.time.temporal.ChronoUnit

object JwtTestHelper {
    private const val TEST_ISSUER = "https://login.microsoftonline.com/test-tenant/v2.0"
    private const val TEST_SUBJECT = "test-subject-123"
    private const val TEST_AUDIENCE = "test-client-id"

    private const val AZURE_GROUP_ID_SAKSBEHANDLER = "5b6745de-b65d-40eb-a6f5-860c8b61c27f"
    private const val AZURE_GROUP_ID_ATTESTERING = "70cfce24-7865-4676-9fdc-b676e90bfc92"
    private const val AZURE_GROUP_ID_LES = "609a78e7-e0bd-491c-a63b-96a09ec62b9b"

    fun opprettGyldigToken(
        navIdent: String = "A123456",
        azureGrupper: List<String> = listOf(AZURE_GROUP_ID_SAKSBEHANDLER),
        navn: String = "Bink Bonk",
        epost: String = "Bink.Bonk@nav.no",
        utløperOm: Long = 3600,
    ): Jwt {
        val nå = Instant.now()
        val utløper = nå.plus(utløperOm, ChronoUnit.SECONDS)

        val headers =
            mapOf(
                "alg" to "RS256",
                "typ" to "JWT",
                "kid" to "test-key-id",
            )

        return Jwt
            .withTokenValue("test-token-value")
            .headers { h -> h.putAll(headers) }
            .issuedAt(nå)
            .expiresAt(utløper)
            .notBefore(nå)
            .issuer(TEST_ISSUER)
            .subject(TEST_SUBJECT)
            .audience(listOf(TEST_AUDIENCE))
            .claim("name", navn)
            .claim("preferred_username", epost)
            .claim("NAVident", navIdent)
            .claim("groups", azureGrupper)
            .build()
    }

    fun opprettTokenUtenNavIdent(): Jwt {
        val nå = Instant.now()
        val utløper = nå.plus(3600, ChronoUnit.SECONDS)

        val headers =
            mapOf(
                "alg" to "RS256",
                "typ" to "JWT",
            )

        return Jwt
            .withTokenValue("test-token-value")
            .headers { header -> header.putAll(headers) }
            .issuedAt(nå)
            .expiresAt(utløper)
            .issuer(TEST_ISSUER)
            .subject(TEST_SUBJECT)
            .audience(listOf(TEST_AUDIENCE))
            .claim("groups", listOf(AZURE_GROUP_ID_SAKSBEHANDLER))
            .build()
    }

    fun opprettTokenUtenGrupper(): Jwt {
        val nå = Instant.now()
        val utløper = nå.plus(3600, ChronoUnit.SECONDS)

        val headers =
            mapOf(
                "alg" to "RS256",
                "typ" to "JWT",
            )

        return Jwt
            .withTokenValue("test-token-value")
            .headers { header -> header.putAll(headers) }
            .issuedAt(nå)
            .expiresAt(utløper)
            .issuer(TEST_ISSUER)
            .subject(TEST_SUBJECT)
            .audience(listOf(TEST_AUDIENCE))
            .claim("NAVident", "A123456")
            .build()
    }

    fun opprettTokenMedUgyldigeGrupper(): Jwt {
        val nå = Instant.now()
        val utløper = nå.plus(3600, ChronoUnit.SECONDS)

        val headers =
            mapOf(
                "alg" to "RS256",
                "typ" to "JWT",
            )

        return Jwt
            .withTokenValue("test-token-value")
            .headers { header -> header.putAll(headers) }
            .issuedAt(nå)
            .expiresAt(utløper)
            .issuer(TEST_ISSUER)
            .subject(TEST_SUBJECT)
            .audience(listOf(TEST_AUDIENCE))
            .claim("NAVident", "A123456")
            .claim("groups", listOf("00000000-0000-0000-0000-000000000000")) // Ukjent AD gruppe
            .build()
    }

    fun opprettSaksbehandlerToken(navIdent: String = "A123456"): Jwt =
        opprettGyldigToken(
            navIdent = navIdent,
            azureGrupper = listOf(AZURE_GROUP_ID_SAKSBEHANDLER),
        )

    fun opprettAttestererToken(navIdent: String = "B123456"): Jwt =
        opprettGyldigToken(
            navIdent = navIdent,
            azureGrupper = listOf(AZURE_GROUP_ID_ATTESTERING),
        )

    fun opprettLeserToken(navIdent: String = "V123456"): Jwt =
        opprettGyldigToken(
            navIdent = navIdent,
            azureGrupper = listOf(AZURE_GROUP_ID_LES),
        )

    fun opprettSaksbehandlerOgAttestererToken(navIdent: String = "AB12345"): Jwt =
        opprettGyldigToken(
            navIdent = navIdent,
            azureGrupper =
                listOf(
                    AZURE_GROUP_ID_SAKSBEHANDLER,
                    AZURE_GROUP_ID_ATTESTERING,
                ),
        )

    fun opprettTokenMedAlleRoller(navIdent: String = "BINKUSMAXIMUS123"): Jwt =
        opprettGyldigToken(
            navIdent = navIdent,
            azureGrupper =
                listOf(
                    AZURE_GROUP_ID_SAKSBEHANDLER,
                    AZURE_GROUP_ID_ATTESTERING,
                    AZURE_GROUP_ID_LES,
                ),
        )
}
