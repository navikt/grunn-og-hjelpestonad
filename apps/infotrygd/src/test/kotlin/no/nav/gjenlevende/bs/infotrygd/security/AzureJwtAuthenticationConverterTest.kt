package no.nav.gjenlevende.bs.infotrygd.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

class AzureJwtAuthenticationConverterTest {
    private val converter = AzureJwtAuthenticationConverter()

    @Test
    fun `skal konvertere gyldig token med saksbehandler rolle`() {
        val jwt = JwtTestHelper.opprettSaksbehandlerToken(navIdent = "A123456")

        val resultat = converter.convert(jwt)

        assertNotNull(resultat)
        assertTrue(resultat is JwtAuthenticationToken)

        val authToken = resultat as JwtAuthenticationToken
        assertEquals(jwt, authToken.token)

        val authorities = authToken.authorities.map { it.authority }

        assertEquals(1, authorities.size)
        assertTrue(authorities.contains("ROLE_SAKSBEHANDLER"))
    }

    @Test
    fun `skal konvertere gyldig token med attestering rolle`() {
        val jwt = JwtTestHelper.opprettAttestererToken(navIdent = "B123456")

        val resultat = converter.convert(jwt)

        assertNotNull(resultat)
        val authToken = resultat as JwtAuthenticationToken

        val authorities = authToken.authorities.map { it.authority }

        assertEquals(1, authorities.size)
        assertTrue(authorities.contains("ROLE_ATTESTERING"))
    }

    @Test
    fun `skal konvertere gyldig token med les rolle`() {
        val jwt = JwtTestHelper.opprettLeserToken(navIdent = "V123456")

        val resultat = converter.convert(jwt)

        assertNotNull(resultat)
        val authToken = resultat as JwtAuthenticationToken

        val authorities = authToken.authorities.map { it.authority }

        assertEquals(1, authorities.size)
        assertTrue(authorities.contains("ROLE_LES"))
    }

    @Test
    fun `skal konvertere token med flere roller`() {
        val jwt = JwtTestHelper.opprettSaksbehandlerOgAttestererToken(navIdent = "AB12345")

        val resultat = converter.convert(jwt)

        assertNotNull(resultat)
        val authToken = resultat as JwtAuthenticationToken

        val authorities = authToken.authorities.map { it.authority }

        assertEquals(2, authorities.size)
        assertTrue(authorities.contains("ROLE_SAKSBEHANDLER"))
        assertTrue(authorities.contains("ROLE_ATTESTERING"))
    }

    @Test
    fun `skal konvertere token med alle nåværende BS roller`() {
        val jwt = JwtTestHelper.opprettTokenMedAlleRoller(navIdent = "ADMIN123")

        val resultat = converter.convert(jwt)

        assertNotNull(resultat)
        val authToken = resultat as JwtAuthenticationToken

        val authorities = authToken.authorities.map { it.authority }

        assertEquals(3, authorities.size)
        assertTrue(authorities.contains("ROLE_SAKSBEHANDLER"))
        assertTrue(authorities.contains("ROLE_ATTESTERING"))
        assertTrue(authorities.contains("ROLE_LES"))
    }

    @Test
    fun `skal kaste exception når NAVident claim mangler`() {
        val jwt = JwtTestHelper.opprettTokenUtenNavIdent()

        val exception = assertThrows<ManglendeClaimException> { converter.convert(jwt) }

        assertEquals("Token mangler påkrevd claim: NAVident", exception.message)
    }

    @Test
    fun `skal kaste tilgang exception når groups claim mangler`() {
        val jwt = JwtTestHelper.opprettTokenUtenGrupper()

        val exception = assertThrows<UtilstrekkeligTilgangException> { converter.convert(jwt) }

        assertEquals(
            "Bruker har ikke tilgang til applikasjonen. Mangler roller.",
            exception.message,
        )
    }

    @Test
    fun `skal kaste tilgang exception når bruker ikke har gyldige Azure grupper`() {
        val jwt = JwtTestHelper.opprettTokenMedUgyldigeGrupper()

        val exception = assertThrows<UtilstrekkeligTilgangException> { converter.convert(jwt) }

        assertEquals(
            "Bruker har ikke tilgang til applikasjonen. Mangler roller.",
            exception.message,
        )
    }

    @Test
    fun `skal bevare JWT token i resulterende authentication token`() {
        val jwt =
            JwtTestHelper.opprettGyldigToken(
                navIdent = "TEST123",
                navn = "Test Bruker",
                epost = "test.bruker@nav.no",
            )

        val resultat = converter.convert(jwt) as JwtAuthenticationToken

        assertEquals(jwt, resultat.token)
        assertEquals("TEST123", resultat.token.getClaimAsString("NAVident"))
        assertEquals("Test Bruker", resultat.token.getClaimAsString("name"))
        assertEquals("test.bruker@nav.no", resultat.token.getClaimAsString("preferred_username"))
    }

    @Test
    fun `skal håndtere token med både gyldige og ugyldige Azure grupper`() {
        val jwt =
            JwtTestHelper.opprettGyldigToken(
                navIdent = "MIKSED123",
                azureGrupper =
                    listOf(
                        "5b6745de-b65d-40eb-a6f5-860c8b61c27f", // SAKSBEHANDLER
                        "00000000-0000-0000-0000-000000000000", // Ukjent gruppe
                        "70cfce24-7865-4676-9fdc-b676e90bfc92", // ATTESTERING
                    ),
            )

        val resultat = converter.convert(jwt) as JwtAuthenticationToken

        val authorities = resultat.authorities.map { it.authority }

        assertEquals(2, authorities.size)
        assertTrue(authorities.contains("ROLE_SAKSBEHANDLER"))
        assertTrue(authorities.contains("ROLE_ATTESTERING"))
    }
}
