package no.nav.grunn.og.hjelpestonad.infrastruktur.exception

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import no.nav.grunn.og.hjelpestonad.pdl.PdlException
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.TypeMismatchException
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.mock.http.MockHttpInputMessage
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.HttpRequestMethodNotSupportedException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ApiExceptionHandlerTest {
    private val handler = ApiExceptionHandler()

    @Test
    fun `håndterte feil og tilgangsfeil logger bare detaljer til secureLogger`() {
        val feil = Feil("sensitiv-testverdi", HttpStatus.BAD_REQUEST, IllegalStateException("sensitiv-årsak"))
        verifiserLogging(feil) {
            assertEquals(HttpStatus.BAD_REQUEST, handler.handleFeil(feil).statusCode)
        }
        val tilgangsfeil = ManglerTilgangIPdlException("sensitiv-testverdi")
        verifiserLogging(tilgangsfeil) {
            assertEquals(HttpStatus.FORBIDDEN, handler.handleManglerTilgang(tilgangsfeil).statusCode)
        }
    }

    @Test
    fun `uventet PDL-feil logges med stacktrace og gir 500-svar uten sensitive opplysninger`() {
        val feil = PdlException("sensitiv-testverdi", IllegalStateException("sensitiv-årsak"))
        verifiserLogging(feil) {
            val response = handler.handleGeneralException(feil)
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
            assertFalse(response.body!!.melding.contains("sensitiv"))
        }
    }

    @Test
    fun `Spring beholder ansvaret for HTTP-feil og sikkerhetsfeil`() {
        val feil = HttpRequestMethodNotSupportedException("TRACE")
        verifiserLogging(feil) {
            assertSame(feil, assertFailsWith<HttpRequestMethodNotSupportedException> { handler.handleGeneralException(feil) })
        }
        val ugyldigBody = HttpMessageNotReadableException("sensitiv-testverdi", MockHttpInputMessage(byteArrayOf()))
        verifiserLogging(ugyldigBody) {
            assertSame(ugyldigBody, assertFailsWith<HttpMessageNotReadableException> { handler.handleGeneralException(ugyldigBody) })
        }
        val ugyldigParameter = TypeMismatchException("sensitiv-testverdi", Int::class.java)
        verifiserLogging(ugyldigParameter) {
            assertSame(ugyldigParameter, assertFailsWith<TypeMismatchException> { handler.handleGeneralException(ugyldigParameter) })
        }
        val tilgangsfeil = AccessDeniedException("sensitiv-testverdi")
        verifiserLogging(tilgangsfeil) {
            assertSame(tilgangsfeil, assertFailsWith<AccessDeniedException> { handler.handleGeneralException(tilgangsfeil) })
        }
        val autentiseringsfeil = BadCredentialsException("sensitiv-testverdi")
        verifiserLogging(autentiseringsfeil) {
            assertSame(autentiseringsfeil, assertFailsWith<BadCredentialsException> { handler.handleGeneralException(autentiseringsfeil) })
        }
    }

    private fun verifiserLogging(
        feil: Throwable,
        håndter: () -> Unit,
    ) {
        val logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java) as Logger
        val secureLogger = LoggerFactory.getLogger("secureLogger") as Logger
        val vanligLogg = ListAppender<ILoggingEvent>().apply { start() }
        val teamLogg = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(vanligLogg)
        secureLogger.addAppender(teamLogg)
        try {
            håndter()
            assertTrue(vanligLogg.list.isNotEmpty())
            vanligLogg.list.forEach {
                assertFalse(it.formattedMessage.contains("sensitiv"))
                assertNull(it.throwableProxy)
            }
            assertEquals(
                feil.message,
                teamLogg.list
                    .single()
                    .throwableProxy.message,
            )
        } finally {
            logger.detachAppender(vanligLogg)
            secureLogger.detachAppender(teamLogg)
            vanligLogg.stop()
            teamLogg.stop()
        }
    }
}
