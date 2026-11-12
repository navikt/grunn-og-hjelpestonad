package no.nav.grunn.og.hjelpestonad.infrastruktur.http

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientResponseException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class EksternTjenesteKallerTest {
    private val logger = LoggerFactory.getLogger("no.nav.grunn.og.hjelpestonad.infrastruktur.http.EksternTjenesteKaller") as Logger
    private val secureLogger = LoggerFactory.getLogger("secureLogger") as Logger
    private val vanligLogg = ListAppender<ILoggingEvent>()
    private val teamLogg = ListAppender<ILoggingEvent>()

    @BeforeEach
    fun startLogging() {
        vanligLogg.start()
        teamLogg.start()
        logger.addAppender(vanligLogg)
        secureLogger.addAppender(teamLogg)
    }

    @AfterEach
    fun stoppLogging() {
        logger.detachAppender(vanligLogg)
        secureLogger.detachAppender(teamLogg)
        vanligLogg.stop()
        teamLogg.stop()
    }

    @Test
    fun `validerer ressursen og sender data og melding til suksesshandleren uten å logge innholdet`() {
        val resultat =
            kallEksternTjenesteMedRessurs(
                tjeneste = "testtjeneste",
                formål = "Hent status",
                hentRessurs = { Ressurs(data = "sensitiv-data", status = "SUKSESS", melding = "sensitiv-melding") },
                vedSuksess = { svar -> "${svar.data}: ${svar.melding}" },
            )

        assertEquals("sensitiv-data: sensitiv-melding", resultat)
        assertTrue(
            vanligLogg.list
                .last()
                .formattedMessage
                .contains("vellykket"),
        )
        assertFalse(vanligLogg.list.any { it.formattedMessage.contains("sensitiv") })
        assertTrue(teamLogg.list.isEmpty())
    }

    @Test
    fun `forventet HTTP-status returnerer handlerens resultat uten feillogg`() {
        val årsak = httpFeil(409)

        val resultat =
            kallEksternTjeneste<String>(
                tjeneste = "testtjeneste",
                formål = "Send oppdrag",
                håndterHttpStatus =
                    mapOf(
                        HttpStatus.CONFLICT to { e ->
                            assertSame(årsak, e)
                            "allerede sendt"
                        },
                    ),
            ) { throw årsak }

        assertEquals("allerede sendt", resultat)
        assertEquals(Level.INFO, vanligLogg.list.last().level)
        assertFalse(vanligLogg.list.any { it.formattedMessage.contains("vellykket") })
        assertTrue(teamLogg.list.isEmpty())
    }

    @Test
    fun `forventet HTTP-status kan kaste en egen feil uten å bli mappet til bad gateway`() {
        val årsak = httpFeil(404)
        val forventet = Feil("Fant ikke oppdrag", HttpStatus.NOT_FOUND, årsak)

        val feil =
            assertFailsWith<Feil> {
                kallEksternTjeneste(
                    tjeneste = "testtjeneste",
                    formål = "Hent status",
                    håndterHttpStatus = mapOf(HttpStatus.NOT_FOUND to { throw forventet }),
                ) { throw årsak }
            }

        assertSame(forventet, feil)
        assertFalse(vanligLogg.list.any { it.level == Level.ERROR })
        assertTrue(teamLogg.list.isEmpty())
    }

    @ParameterizedTest
    @ValueSource(ints = [500, 599])
    fun `uventede HTTP-feil gir bad gateway og logger detaljer bare til secureLogger`(status: Int) {
        val årsak = httpFeil(status)

        val feil =
            assertFailsWith<Feil> {
                kallEksternTjeneste("testtjeneste", "Hent status") { throw årsak }
            }

        assertEquals(HttpStatus.BAD_GATEWAY, feil.httpStatus)
        assertSame(årsak, feil.cause)
        assertEquals(1, vanligLogg.list.count { it.level == Level.ERROR })
        vanligLogg.list.forEach {
            assertFalse(it.formattedMessage.contains("sensitiv"))
            assertNull(it.throwableProxy)
        }
        val detaljert = teamLogg.list.single()
        assertTrue(detaljert.formattedMessage.contains("sensitiv-body"))
        assertEquals(årsak.javaClass.name, detaljert.throwableProxy.className)
    }

    @Test
    fun `transportfeil gir bad gateway med opprinnelig årsak`() {
        val årsak = ResourceAccessException("sensitiv-uri")

        val feil =
            assertFailsWith<Feil> {
                kallEksternTjeneste("testtjeneste", "Hent status") { throw årsak }
            }

        assertEquals(HttpStatus.BAD_GATEWAY, feil.httpStatus)
        assertSame(årsak, feil.cause)
        assertEquals(1, vanligLogg.list.count { it.level == Level.ERROR })
        assertFalse(vanligLogg.list.any { it.formattedMessage.contains("sensitiv") || it.throwableProxy != null })
        assertEquals(
            "sensitiv-uri",
            teamLogg.list
                .single()
                .throwableProxy.message,
        )
    }

    @ParameterizedTest
    @MethodSource("ugyldigeRessurser")
    fun `ugyldig ressurs gir bad gateway og bare en feillogg`(ressurs: Ressurs<String>?) {
        var suksessKalt = false

        val feil =
            assertFailsWith<Feil> {
                kallEksternTjenesteMedRessurs(
                    tjeneste = "testtjeneste",
                    formål = "Hent status",
                    hentRessurs = { ressurs },
                    vedSuksess = { suksessKalt = true },
                )
            }

        assertEquals(HttpStatus.BAD_GATEWAY, feil.httpStatus)
        assertFalse(suksessKalt)
        assertEquals(1, vanligLogg.list.count { it.level == Level.ERROR })
        assertFalse(vanligLogg.list.any { it.formattedMessage.contains("vellykket") })
        assertEquals(
            Feil::class.java.name,
            teamLogg.list
                .single()
                .throwableProxy.className,
        )
    }

    @Test
    fun `feil i suksesshandleren logges en gang og kastes uendret`() {
        val årsak = IllegalStateException("sensitiv-feil")

        val feil =
            assertFailsWith<IllegalStateException> {
                kallEksternTjenesteMedRessurs(
                    tjeneste = "testtjeneste",
                    formål = "Hent status",
                    hentRessurs = { Ressurs(data = "data", status = "SUKSESS") },
                    vedSuksess = { throw årsak },
                )
            }

        assertSame(årsak, feil)
        assertEquals(1, vanligLogg.list.count { it.level == Level.ERROR })
        assertFalse(vanligLogg.list.any { it.formattedMessage.contains("vellykket") || it.formattedMessage.contains("sensitiv") })
        assertEquals(
            "sensitiv-feil",
            teamLogg.list
                .single()
                .throwableProxy.message,
        )
    }

    private fun httpFeil(status: Int) = RestClientResponseException("sensitiv-uri", status, "sensitiv-status", null, "sensitiv-body".toByteArray(), Charsets.UTF_8)

    companion object {
        @JvmStatic
        fun ugyldigeRessurser(): List<Ressurs<String>?> =
            listOf(
                null,
                Ressurs(data = null, status = "SUKSESS"),
                Ressurs(data = "data", status = "FEILET", melding = "sensitiv-melding"),
            )
    }
}
