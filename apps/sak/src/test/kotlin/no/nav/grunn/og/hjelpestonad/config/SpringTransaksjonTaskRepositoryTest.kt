package no.nav.grunn.og.hjelpestonad.config

import efterlatte.prosessering.Status
import efterlatte.prosessering.TaskRepository
import no.nav.grunn.og.hjelpestonad.SpringContextTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class SpringTransaksjonTaskRepositoryTest : SpringContextTest() {
    @Autowired
    private lateinit var taskRepository: TaskRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `bruker adapteren og ikke bibliotekets standardrepository`() {
        assertThat(taskRepository).isInstanceOf(SpringTransaksjonTaskRepository::class.java)
    }

    @Test
    fun `skriving via Spring i steget og markerFullført committes sammen`() {
        val taskId = opprettTaskSomIkkePlukkes()

        taskRepository.iEgenTransaksjon { transaksjon ->
            skrivViaSpring(taskId)
            taskRepository.markerFullført(transaksjon, taskId)
        }

        assertThat(taskRepository.finn(taskId)?.status).isEqualTo(Status.FULLFØRT)
        assertThat(antallRaderSkrevetViaSpring(taskId)).isEqualTo(1)
    }

    @Test
    fun `feil i steget ruller tilbake både skriving via Spring og markerFullført`() {
        val taskId = opprettTaskSomIkkePlukkes()

        assertThatThrownBy {
            taskRepository.iEgenTransaksjon { transaksjon ->
                skrivViaSpring(taskId)
                taskRepository.markerFullført(transaksjon, taskId)
                error("hemmelig-detalj")
            }
        }.isInstanceOf(TaskStegFeiletException::class.java)

        assertThat(taskRepository.finn(taskId)?.status).isEqualTo(Status.KLAR)
        assertThat(antallRaderSkrevetViaSpring(taskId)).isZero()
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

    private fun antallRaderSkrevetViaSpring(taskId: Long): Int? =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM prosessering_task_hendelse WHERE task_id = ?",
            Int::class.java,
            taskId,
        )
}
