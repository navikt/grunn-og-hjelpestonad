package no.nav.grunn.og.hjelpestonad.prosessering

import com.zaxxer.hikari.HikariDataSource
import efterlatte.prosessering.Status
import efterlatte.prosessering.TaskLoggType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.DependsOn
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.JdbcTransactionManager
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.transaction.annotation.EnableTransactionManagement
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

@SpringJUnitConfig(ProsesseringRepositoryTest.TestConfig::class)
class ProsesseringRepositoryTest {
    @TestConfiguration
    @EnableTransactionManagement
    open class TestConfig {
        @Bean(destroyMethod = "close")
        open fun dataSource(): HikariDataSource =
            HikariDataSource().apply {
                driverClassName = "org.testcontainers.jdbc.ContainerDatabaseDriver"
                jdbcUrl = "jdbc:tc:postgresql:17:///prosesseringtest"
                username = "test"
                password = "test"
                maximumPoolSize = 3
                minimumIdle = 1
            }

        @Bean(initMethod = "migrate")
        open fun flyway(dataSource: DataSource): Flyway =
            Flyway
                .configure()
                .dataSource(dataSource)
                .placeholders(mapOf("ignoreIfProd" to ""))
                .load()

        @Bean
        @DependsOn("flyway")
        open fun jdbcTemplate(dataSource: DataSource): JdbcTemplate = JdbcTemplate(dataSource)

        @Bean
        open fun transactionManager(dataSource: DataSource): JdbcTransactionManager = JdbcTransactionManager(dataSource)

        @Bean
        open fun objectMapper(): ObjectMapper = jacksonObjectMapper()

        @Bean
        open fun prosesseringRepository(
            jdbcTemplate: JdbcTemplate,
            objectMapper: ObjectMapper,
        ): ProsesseringRepository = ProsesseringRepository(jdbcTemplate, objectMapper)

        @Bean
        open fun prosesseringService(prosesseringRepository: ProsesseringRepository): ProsesseringService = ProsesseringService(prosesseringRepository, "testnode")
    }

    @Autowired
    private lateinit var prosesseringService: ProsesseringService

    @Autowired
    private lateinit var prosesseringRepository: ProsesseringRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @AfterEach
    fun ryddTasks() {
        jdbcTemplate.update("DELETE FROM prosessering_task_hendelse")
        jdbcTemplate.update("DELETE FROM prosessering_task")
    }

    @Test
    fun `oversikten inkluderer tomme statuser og teller alle typer`() {
        opprettTask(Status.STOPPET)
        opprettTask(Status.STOPPET)
        opprettTask(Status.AVBRUTT, "AnnenTask")
        val statusoversikt = prosesseringService.hentStatusoversikt()
        assertThat(statusoversikt).hasSize(5)
        assertThat(statusoversikt.single { it.status == Status.STOPPET }.antall).isEqualTo(2)
        assertThat(statusoversikt.single { it.status == Status.KLAR }.antall).isZero()
        assertThat(prosesseringService.hentTyper()).containsExactly(
            ProsesseringTypeTelling("AnnenTask", 1),
            ProsesseringTypeTelling("TestTask", 2),
        )
    }

    @Test
    fun `filtrering paginering og stabil sortering skjer i databasen`() {
        val forsteTaskId = opprettTask(Status.STOPPET)
        val andreTaskId = opprettTask(Status.STOPPET)
        opprettTask(Status.AVBRUTT)
        opprettTask(Status.STOPPET, "AnnenTask")
        val forsteSide = prosesseringService.hentTasks(Status.STOPPET, "TestTask", 1, 1, "Z123456")
        val andreSide = prosesseringService.hentTasks(Status.STOPPET, "TestTask", 2, 1, "Z123456")
        assertThat(forsteSide.tasks.map { it.id }).containsExactly(andreTaskId)
        assertThat(andreSide.tasks.map { it.id }).containsExactly(forsteTaskId)
        assertThat(forsteSide.antallTotalt).isEqualTo(2)
        assertThat(forsteSide.antallSider).isEqualTo(2)
        assertThat(forsteSide.avkortet).isFalse()
        assertThat(forsteSide.tasks.single().metadata).containsEntry("traceId", "test-trace")
        assertThat(prosesseringService.hentTasks(null, "' OR TRUE --", 1, 25, "Z123456").tasks).isEmpty()
    }

    @Test
    fun `rekjor beholder feiltelling og oppdaterer versjon og trigger`() {
        val taskId = opprettTask(Status.STOPPET)
        val opprinneligTask = prosesseringService.hentTask(taskId, "Z123456")
        val oppdatertTask = prosesseringService.endreTask(taskId, 7, ProsesseringHandling.REKJOR, "Z123456")
        assertThat(oppdatertTask.status).isEqualTo(Status.KLAR)
        assertThat(oppdatertTask.antallFeil).isEqualTo(3)
        assertThat(oppdatertTask.versjon).isEqualTo(8)
        assertThat(oppdatertTask.triggerTid).isBefore(opprinneligTask.triggerTid)
        assertThat(oppdatertTask.plukketTid).isNull()
        assertThat(oppdatertTask.stoppaarsak).isNull()
        assertThat(prosesseringService.hentLogg(taskId, "Z123456").single().endretAv).isEqualTo("Z123456")
        assertThatThrownBy {
            prosesseringService.endreTask(taskId, 7, ProsesseringHandling.AVBRYT, "Z123456")
        }.hasMessageContaining("Noen andre")
    }

    @Test
    fun `avvik lagrer statusendring og begrunnelse i samme historikk`() {
        val taskId = opprettTask(Status.STOPPET)
        val oppdatertTask = prosesseringService.endreTask(taskId, 7, ProsesseringHandling.AVVIK, "Z123456", "Undersøkt manuelt")
        assertThat(oppdatertTask.status).isEqualTo(Status.AVBRUTT)
        val hendelseslogg = prosesseringService.hentLogg(taskId, "Z123456")
        assertThat(hendelseslogg.map { it.type }).containsExactly(TaskLoggType.STATUS_ENDRET, TaskLoggType.AVVIK)
        assertThat(hendelseslogg.last().melding).isEqualTo("Undersøkt manuelt")
    }

    @Test
    fun `feil ved avvik ruller tilbake både status og allerede skrevet historikk`() {
        val taskId = opprettTask(Status.STOPPET)
        jdbcTemplate.execute(
            "ALTER TABLE prosessering_task_hendelse ADD CONSTRAINT test_avvis_avvik CHECK (type <> 'AVVIK')",
        )
        try {
            assertThatThrownBy {
                prosesseringService.endreTask(taskId, 7, ProsesseringHandling.AVVIK, "Z123456", "Undersøkt manuelt")
            }.isInstanceOf(DataIntegrityViolationException::class.java)
        } finally {
            jdbcTemplate.execute("ALTER TABLE prosessering_task_hendelse DROP CONSTRAINT test_avvis_avvik")
        }
        assertThat(prosesseringRepository.hentTask(taskId)?.status).isEqualTo(Status.STOPPET)
        assertThat(prosesseringRepository.hentTask(taskId)?.versjon).isEqualTo(7)
        assertThat(prosesseringRepository.hentLogg(taskId)).isEmpty()
    }

    @Test
    fun `to samtidige operatorer kan ikke endre samme versjon`() {
        val taskId = opprettTask(Status.STOPPET)
        val start = CountDownLatch(1)
        Executors.newFixedThreadPool(2).use { executor ->
            val endringer =
                (1..2).map {
                    executor.submit(
                        Callable {
                            check(start.await(10, TimeUnit.SECONDS))
                            try {
                                prosesseringService.endreTask(taskId, 7, ProsesseringHandling.AVBRYT, "Z123456")
                                200
                            } catch (prosesseringApiException: ProsesseringApiException) {
                                prosesseringApiException.status.value()
                            }
                        },
                    )
                }
            start.countDown()
            assertThat(endringer.map { it.get(15, TimeUnit.SECONDS) }).containsExactlyInAnyOrder(200, 409)
        }
        assertThat(prosesseringRepository.hentLogg(taskId)).hasSize(1)
    }

    private fun opprettTask(
        status: Status,
        type: String = "TestTask",
    ): Long =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO prosessering_task
                (type, status, payload, antall_feil, versjon, trigger_tid, sist_endret_tid, metadata)
            VALUES (?, ?, ?, 3, 7, now() + interval '1 day', '2026-11-18T10:00:00Z', '{"traceId":"test-trace"}')
            RETURNING id
            """.trimIndent(),
            Long::class.java,
            type,
            status.name,
            UUID.randomUUID().toString(),
        )!!
}
