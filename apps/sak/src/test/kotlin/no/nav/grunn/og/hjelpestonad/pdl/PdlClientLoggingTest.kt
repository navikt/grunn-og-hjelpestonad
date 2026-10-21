package no.nav.grunn.og.hjelpestonad.pdl

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.ManglerTilgangIPdlException
import no.nav.grunn.og.hjelpestonad.texas.StubTexasClient
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PdlClientLoggingTest {
    @Test
    fun `GraphQL-tilgangsfeil logger alle feildetaljer og stacktrace bare til secureLogger`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = PdlClient(StubTexasClient(), "http://pdl", "pdlScope", builder)
        server.expect(requestTo("http://pdl/graphql")).andRespond(
            withSuccess(
                """
                {
                  "errors": [
                    {"message": "sensitiv-første-feil", "extensions": {"code": "unauthorized"}},
                    {"message": "sensitiv-andre-feil", "extensions": {"code": "forbidden"}}
                  ]
                }
                """.trimIndent(),
                MediaType.APPLICATION_JSON,
            ),
        )
        val logger = LoggerFactory.getLogger(PdlClient::class.java) as Logger
        val secureLogger = LoggerFactory.getLogger("secureLogger") as Logger
        val vanligLogg = ListAppender<ILoggingEvent>().apply { start() }
        val teamLogg = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(vanligLogg)
        secureLogger.addAppender(teamLogg)
        try {
            val feil =
                assertFailsWith<ManglerTilgangIPdlException> {
                    client.hentPersonDataOBOToken(PdlRequest("query {}", emptyMap()))
                }
            vanligLogg.list.forEach {
                assertFalse(it.formattedMessage.contains("sensitiv"))
                assertNull(it.throwableProxy)
            }
            val detaljert = teamLogg.list.single()
            assertTrue(detaljert.formattedMessage.contains("sensitiv-første-feil"))
            assertTrue(detaljert.formattedMessage.contains("sensitiv-andre-feil"))
            assertTrue(detaljert.formattedMessage.contains("unauthorized"))
            assertEquals(feil.javaClass.name, detaljert.throwableProxy.className)
            assertTrue(detaljert.throwableProxy.stackTraceElementProxyArray.isNotEmpty())
            server.verify()
        } finally {
            logger.detachAppender(vanligLogg)
            secureLogger.detachAppender(teamLogg)
            vanligLogg.stop()
            teamLogg.stop()
        }
    }
}
