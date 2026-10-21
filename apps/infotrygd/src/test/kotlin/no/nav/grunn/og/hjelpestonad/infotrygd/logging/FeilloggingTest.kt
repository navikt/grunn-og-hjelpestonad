package no.nav.grunn.og.hjelpestonad.infotrygd.logging

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.util.LogbackMDCAdapter
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.OutputStreamAppender
import ch.qos.logback.core.status.Status
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TeamLogsTestAppender : OutputStreamAppender<ILoggingEvent>() {
    val loggutskrift = ByteArrayOutputStream()

    init {
        outputStream = loggutskrift
    }
}

class FeilloggingTest {
    private val context = LoggerContext()
    private val vanligLogg = ByteArrayOutputStream()
    private lateinit var teamLogg: TeamLogsTestAppender
    private val logger get() = context.getLogger("test.Integrasjon")

    @BeforeEach
    fun start() {
        context.mdcAdapter = LogbackMDCAdapter()
        // Bruk produksjonsoppsettet, men erstatt TCP-transporten med en minnebuffer.
        val konfigurasjon =
            requireNotNull(javaClass.getResource("/logback-spring.xml"))
                .readText()
                .replace("net.logstash.logback.appender.LogstashTcpSocketAppender", TeamLogsTestAppender::class.java.name)
                .replace(Regex("<destination>.*?</destination>"), "")
        JoranConfigurator().apply {
            context = this@FeilloggingTest.context
            doConfigure(ByteArrayInputStream(konfigurasjon.toByteArray()))
        }
        val root = context.getLogger("ROOT")
        (root.getAppender("default-json") as ConsoleAppender<ILoggingEvent>).outputStream = vanligLogg
        teamLogg = root.getAppender("team-logs") as TeamLogsTestAppender
        assertFalse(context.statusManager.copyOfStatusList.any { it.level == Status.ERROR })
    }

    @AfterEach
    fun stopp() {
        context.stop()
    }

    @Test
    fun `feildetaljer og hele stacktracen finnes bare i Team Logs`() {
        val sensitiv = "sensitiv-testverdi"
        val årsak = IllegalArgumentException("Rotårsak $sensitiv")
        val feil = IllegalStateException("Feil $sensitiv", årsak)
        feil.addSuppressed(RuntimeException("Undertrykt $sensitiv"))
        context.mdcAdapter.put("trace_id", "0123456789abcdef0123456789abcdef")
        context.mdcAdapter.put("span_id", "0123456789abcdef")

        logger.error("Kall feilet. Se Team Logs for detaljer.")
        context.mdcAdapter.put("personident", sensitiv)
        context
            .getLogger("secureLogger")
            .atError()
            .setCause(feil)
            .addKeyValue("person", sensitiv)
            .log("Kall feilet for {}", sensitiv)

        val vanlig = vanligLogg.toString(Charsets.UTF_8)
        val team = teamLogg.loggutskrift.toString(Charsets.UTF_8)
        assertFalse(vanlig.contains(sensitiv))
        assertFalse(vanlig.contains("stack_trace"))
        assertFalse(vanlig.contains("personident"))
        assertFalse(vanlig.contains("IllegalStateException"))
        assertTrue(vanlig.contains("Se Team Logs"))
        assertTrue(vanlig.contains("0123456789abcdef0123456789abcdef"))
        assertTrue(vanlig.contains("0123456789abcdef"))
        assertTrue(team.contains("Kall feilet for $sensitiv"))
        assertTrue(team.contains("Caused by: java.lang.IllegalArgumentException"))
        assertTrue(team.contains("Suppressed: java.lang.RuntimeException"))
        assertTrue(team.contains("FeilloggingTest"))
        assertTrue(team.contains("personident"))
        assertTrue(team.contains("\"person\":\"$sensitiv\""))
    }

    @Test
    fun `secureLogger skriver ikke til vanlig logg uansett loggnivå`() {
        val secureLogger = context.getLogger("secureLogger")
        secureLogger.warn("Sensitiv warning")
        secureLogger.info("Sensitiv infomelding", RuntimeException("Sensitiv exception"))

        assertFalse(vanligLogg.toString(Charsets.UTF_8).contains("Sensitiv"))
        val team = teamLogg.loggutskrift.toString(Charsets.UTF_8)
        assertTrue(team.contains("Sensitiv warning"))
        assertTrue(team.contains("Sensitiv infomelding"))
        assertTrue(team.contains("Sensitiv exception"))
    }

    @Test
    fun `Springs detaljer om ugyldige requests finnes bare i Team Logs`() {
        logger.warn("Forespørselen kunne ikke behandles")
        context
            .getLogger("org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver")
            .warn("Resolved [ugyldig parameter: sensitiv-testverdi]")

        assertTrue(vanligLogg.toString(Charsets.UTF_8).contains("Forespørselen kunne ikke behandles"))
        assertFalse(vanligLogg.toString(Charsets.UTF_8).contains("sensitiv-testverdi"))
        assertTrue(teamLogg.loggutskrift.toString(Charsets.UTF_8).contains("sensitiv-testverdi"))
    }

    @Test
    fun `vanlige logger sendes til både stdout og Team Logs som i EF-appen`() {
        logger.info("Tjenesten er klar")
        assertTrue(vanligLogg.toString(Charsets.UTF_8).contains("Tjenesten er klar"))
        assertTrue(teamLogg.loggutskrift.toString(Charsets.UTF_8).contains("Tjenesten er klar"))
    }
}
