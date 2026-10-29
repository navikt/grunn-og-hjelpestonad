package no.nav.grunn.og.hjelpestonad.config

import efterlatte.prosessering.TaskRepository
import efterlatte.prosessering.Transaksjon
import efterlatte.prosessering.postgres.JdbcTransaksjon
import efterlatte.prosessering.postgres.PostgresTaskRepository
import efterlatte.prosessering.spring.ProsesseringProperties
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.DataSourceUtils
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import javax.sql.DataSource

@Configuration
class ProsesseringConfig {
    /**
     * Erstatter bibliotekets standard-[TaskRepository], som er `@ConditionalOnMissingBean`.
     * Resten av bønnene kommer fra `ProsesseringAutoConfiguration` og bruker denne.
     */
    @Bean
    fun taskRepository(
        dataSource: DataSource,
        transactionManager: PlatformTransactionManager,
        properties: ProsesseringProperties,
    ): TaskRepository =
        SpringTransaksjonTaskRepository(
            delegate = PostgresTaskRepository(dataSource = dataSource, skjema = properties.skjema),
            dataSource = dataSource,
            transactionTemplate = TransactionTemplate(transactionManager),
        )
}

/**
 * Kjører hvert task-steg i en Spring-transaksjon i stedet for bibliotekets egen JDBC-transaksjon.
 *
 * Biblioteket åpner sin egen tilkobling i [TaskRepository.iEgenTransaksjon], og den ser ikke
 * Spring Data JDBC. Her henter vi tilkoblingen Spring har bundet til tråden, slik at det steget
 * skriver via repositoryene og `markerFullført` committes eller rulles tilbake samlet.
 *
 * Exceptions fra steget kan inneholde personopplysninger, for eksempel feilsvar fra dokarkiv.
 * Biblioteket logger exceptionen til vanlig logg, så vi logger den fullstendig til team logs og
 * kaster videre en exception uten `cause` og uten opprinnelig melding.
 */
class SpringTransaksjonTaskRepository(
    private val delegate: TaskRepository,
    private val dataSource: DataSource,
    private val transactionTemplate: TransactionTemplate,
) : TaskRepository by delegate {
    override fun <T> iEgenTransaksjon(block: (Transaksjon) -> T): T =
        try {
            @Suppress("UNCHECKED_CAST")
            transactionTemplate.execute {
                val connection = DataSourceUtils.getConnection(dataSource)
                try {
                    block(JdbcTransaksjon(connection))
                } finally {
                    DataSourceUtils.releaseConnection(connection, dataSource)
                }
            } as T
        } catch (e: Exception) {
            secureLogger.warn("Task-steg feilet", e)
            throw TaskStegFeiletException("Task-steg feilet (${e::class.simpleName}). Se team logs.")
        }

    private companion object {
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}

class TaskStegFeiletException(
    melding: String,
) : RuntimeException(melding)
