package no.nav.gjenlevende.bs.sak.tilgangskontroll

import io.mockk.every
import io.mockk.mockk
import no.nav.gjenlevende.bs.sak.texas.TexasClient
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.net.URI

class TilgangsmaskinClientTest {
    private val tilgangsmaskinUrl = URI.create("http://localhost:8080")
    private val tilgangsmaskinScope = "api://test/.default"

    private val texasClient = mockk<TexasClient>()

    private lateinit var client: TilgangsmaskinClient
    private val tilgangsmaskinWebClient = mockk<WebClient>(relaxed = true)

    @BeforeEach
    fun setup() {
        client =
            TilgangsmaskinClient(
                tilgangsmaskinUrl = tilgangsmaskinUrl,
                tilgangsmaskinScope = tilgangsmaskinScope,
                texasClient = texasClient,
                tilgangsmaskinWebClient = tilgangsmaskinWebClient,
            )
        every { texasClient.hentOboToken(tilgangsmaskinScope) } returns "gyldig-token"
    }

    @Nested
    inner class SjekkTilgangBulk {
        @Test
        fun `saksbehandler har ikke tilgang til brukere - returnerer ikke 204`() {
            val personidenter = listOf("12345678901", "12345678901")
            val expectedResponse = lagBulkRespons(personidenter, status = 403)
            mockkWebclientBulk(expectedResponse)

            val resultat = client.sjekkTilgangBulk(personidenter)
            assertTrue(resultat.resultater.none { it.harTilgang })
        }

        @Test
        fun `saksbehandler har tilgang til brukere - returnerer 204`() {
            val personidenter = listOf("12345678901", "12345678901")
            val expectedResponse = lagBulkRespons(personidenter, status = 204)
            mockkWebclientBulk(expectedResponse)

            val resultat = client.sjekkTilgangBulk(personidenter)
            assertTrue(resultat.resultater.all { it.harTilgang })
        }

        private fun lagBulkRespons(
            personident: List<String>,
            status: Int,
        ): BulkTilgangsResponse =
            BulkTilgangsResponse(
                navIdent = "Z123456",
                resultater =
                    personident
                        .map { pid ->
                            TilgangsResultat(
                                personident = pid,
                                status = status,
                                detaljer = null,
                            )
                        }.toSet(),
            )

        private fun mockkWebclientBulk(
            expectedResponse: BulkTilgangsResponse,
        ) {
            val responseSpec = mockk<WebClient.ResponseSpec>()
            val requestHeadersSpec = mockk<RequestBodyUriSpec>()

            every { tilgangsmaskinWebClient.post() } returns requestHeadersSpec
            every { requestHeadersSpec.uri(any<URI>()) } returns requestHeadersSpec
            every { requestHeadersSpec.bodyValue(any<Set<String>>()) } returns requestHeadersSpec
            every { requestHeadersSpec.headers(any()) } returns requestHeadersSpec
            every { requestHeadersSpec.retrieve() } returns responseSpec
            every { responseSpec.bodyToMono<BulkTilgangsResponse>() } returns Mono.just(expectedResponse)
        }
    }
}
