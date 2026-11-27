package no.nav.grunn.og.hjelpestonad.config

import efterlatte.prosessering.Status
import efterlatte.prosessering.TaskRepository
import efterlatte.prosessering.spring.TaskService
import no.nav.grunn.og.hjelpestonad.SpringContextTest
import no.nav.grunn.og.hjelpestonad.logging.Korrelasjonsfilter
import no.nav.grunn.og.hjelpestonad.prosessering.ProsesseringRepository
import no.nav.grunn.og.hjelpestonad.task.lagUuidTaskType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class SpringTransaksjonTaskRepositoryTest : SpringContextTest() {
    @Autowired
    private lateinit var taskRepository: TaskRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var korrelasjonsfilter: Korrelasjonsfilter

    @Autowired
    private lateinit var prosesseringRepository: ProsesseringRepository

    @Test
    fun `bruker adapteren og ikke bibliotekets standardrepository`() {
        assertThat(taskRepository).isInstanceOf(SpringTransaksjonTaskRepository::class.java)
    }

    @Test
    fun `task opprettet fra HTTP lagrer korrelasjons-ID i metadata`() {
        korrelasjonsfilter.doFilter(MockHttpServletRequest(), MockHttpServletResponse()) { _, _ ->
            val taskId =
                taskRepository.iEgenTransaksjon {
                    taskService.opprett(
                        type = lagUuidTaskType("korrelasjonstest"),
                        payload = UUID.randomUUID(),
                        triggerTid = Instant.now().plus(1, ChronoUnit.DAYS),
                    )
                }

            val lagretTask = requireNotNull(taskRepository.finn(taskId.verdi))
            assertThat(lagretTask.metadata).containsEntry("traceId", MDC.get("callId"))
            assertThat(lagretTask.metadata["traceId"]).isNotBlank()
        }
    }

    @Test
    fun `skriving via Spring og fullføring med historikk committes sammen`() {
        val taskId = opprettTaskSomIkkePlukkes()

        taskRepository.iEgenTransaksjon { transaksjon ->
            skrivViaSpring(taskId)
            taskRepository.markerFullført(transaksjon, taskId, node = "testnode")
        }

        assertThat(taskRepository.finn(taskId)?.status).isEqualTo(Status.FULLFØRT)
        val hendelseslogg = prosesseringRepository.hentLogg(taskId)
        assertThat(hendelseslogg.map { it.melding }).containsExactly("test", "KJØRER -> FULLFØRT")
        assertThat(hendelseslogg.last().endretAv).isEqualTo("system")
        assertThat(hendelseslogg.last().node).isEqualTo("testnode")
    }

    @Test
    fun `feil i steget ruller tilbake skriving via Spring og fullføring med historikk`() {
        val taskId = opprettTaskSomIkkePlukkes()

        assertThatThrownBy {
            taskRepository.iEgenTransaksjon { transaksjon ->
                skrivViaSpring(taskId)
                taskRepository.markerFullført(transaksjon, taskId, node = "testnode")
                error("hemmelig-detalj")
            }
        }.isInstanceOf(TaskStegFeiletException::class.java)

        assertThat(taskRepository.finn(taskId)?.status).isEqualTo(Status.KLAR)
        assertThat(prosesseringRepository.hentLogg(taskId)).isEmpty()
    }

    @Test
    fun `strukturert feilhistorikk er skjermet og bevares etter vellykket nytt forsøk`() {
        val taskId = opprettTaskSomIkkePlukkes()
        val opprinneligException =
            IllegalStateException("hemmelig-melding", IllegalArgumentException("hemmelig-årsak")).apply {
                addSuppressed(IllegalStateException("hemmelig-suppressed"))
            }
        val taskStegFeiletException =
            assertThrows<TaskStegFeiletException> {
                taskRepository.iEgenTransaksjon<Unit> { throw opprinneligException }
            }

        taskRepository.markFeilet(
            id = taskId,
            nyStatus = Status.KLAR,
            stoppaarsak = null,
            nesteTriggerTid = Instant.now().plus(1, ChronoUnit.DAYS),
            feilmelding = taskStegFeiletException.message,
            stacktrace = taskStegFeiletException.stackTraceToString(),
            node = "testnode",
            exceptionClass = taskStegFeiletException.javaClass.name,
        )
        taskRepository.iEgenTransaksjon { transaksjon ->
            taskRepository.markerFullført(transaksjon, taskId, node = "testnode")
        }

        val hendelseslogg = prosesseringRepository.hentLogg(taskId)
        assertThat(hendelseslogg).hasSize(2)
        assertThat(hendelseslogg.map { it.node }).containsOnly("testnode")
        assertThat(hendelseslogg.map { it.endretAv }).containsOnly("system")
        val feilhistorikk = jacksonObjectMapper().readTree(hendelseslogg.first().melding)
        assertThat(feilhistorikk["tilStatus"].asText()).isEqualTo("KLAR")
        assertThat(feilhistorikk["antallFeil"].asInt()).isEqualTo(1)
        assertThat(feilhistorikk["feilmelding"].asText()).isEqualTo(taskStegFeiletException.message)
        assertThat(feilhistorikk["exceptionClass"].asText()).isEqualTo(TaskStegFeiletException::class.java.name)
        assertThat(feilhistorikk["stackTrace"].asText())
            .contains(TaskStegFeiletException::class.java.name, "IllegalStateException")
            .doesNotContain("hemmelig-melding", "hemmelig-årsak", "hemmelig-suppressed")
        assertThat(hendelseslogg.last().melding).isEqualTo("KJØRER -> FULLFØRT")
        assertThat(taskRepository.finn(taskId)?.status).isEqualTo(Status.FULLFØRT)
    }

    @Test
    fun `exception som kastes videre har verken cause eller opprinnelig melding`() {
        val feil =
            runCatching {
                taskRepository.iEgenTransaksjon { error("hemmelig-detalj") }
            }.exceptionOrNull()

        assertThat(feil).isInstanceOf(TaskStegFeiletException::class.java).hasNoCause()
        assertThat(feil?.message).doesNotContain("hemmelig-detalj").contains("IllegalStateException")
    }

    /** Trigger-tid i morgen, så motoren ikke plukker tasken mens testen kjører. */
    private fun opprettTaskSomIkkePlukkes(): Long =
        taskRepository.insertIEgenTransaksjon(
            type = "adapter-test",
            payload = UUID.randomUUID().toString(),
            triggerTid = Instant.now().plus(1, ChronoUnit.DAYS),
        )

    /** JdbcTemplate henter tilkoblingen Spring har bundet til tråden, slik repositoryene i stegene gjør. */
    private fun skrivViaSpring(taskId: Long) {
        jdbcTemplate.update(
            "INSERT INTO prosessering_task_hendelse (task_id, type, melding, endret_av, node) VALUES (?, 'KOMMENTAR', 'test', 'test', 'test')",
            taskId,
        )
    }
}
