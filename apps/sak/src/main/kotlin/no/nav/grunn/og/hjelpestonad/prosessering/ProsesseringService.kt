package no.nav.grunn.og.hjelpestonad.prosessering

import efterlatte.prosessering.Status
import efterlatte.prosessering.TaskLogg
import efterlatte.prosessering.TaskLoggType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

@Service
class ProsesseringService(
    private val prosesseringRepository: ProsesseringRepository,
    @param:Value("\${efterlatte-prosessering.node}") private val node: String,
) {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun hentStatusoversikt(): List<ProsesseringStatusTelling> = prosesseringRepository.hentStatusoversikt()

    fun hentTyper(): List<ProsesseringTypeTelling> = prosesseringRepository.hentTyper()

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    fun hentTasks(
        status: Status?,
        type: String?,
        side: Int,
        antallPerSide: Int,
        navIdent: String,
    ): ProsesseringTaskSide {
        if (side < 1 || antallPerSide !in 1..100) {
            throw ProsesseringApiException(HttpStatus.BAD_REQUEST, "Side må være minst 1 og antallPerSide må være mellom 1 og 100")
        }
        val prosesseringTaskSide = prosesseringRepository.hentTasks(status, type, side, antallPerSide)
        secureLogger.info("Prosessering: utleverte tasks={} til NAVident={}", prosesseringTaskSide.tasks.map { it.id }, navIdent)
        return prosesseringTaskSide
    }

    fun hentTask(
        taskId: Long,
        navIdent: String,
    ): ProsesseringTask {
        val prosesseringTask = krevTask(taskId)
        secureLogger.info("Prosessering: utleverte task={} til NAVident={}", taskId, navIdent)
        return prosesseringTask
    }

    fun hentLogg(
        taskId: Long,
        navIdent: String,
    ): List<TaskLogg> {
        krevTask(taskId)
        val taskLogg = prosesseringRepository.hentLogg(taskId)
        secureLogger.info("Prosessering: utleverte hendelseslogg for task={} til NAVident={}", taskId, navIdent)
        return taskLogg
    }

    @Transactional
    fun endreTask(
        taskId: Long,
        versjon: Long,
        handling: ProsesseringHandling,
        navIdent: String,
        avviksmelding: String? = null,
    ): ProsesseringTask {
        if (versjon < 0 || (handling == ProsesseringHandling.AVVIK && avviksmelding.isNullOrBlank())) {
            throw ProsesseringApiException(HttpStatus.BAD_REQUEST, "Versjon må være minst 0 og avvik må ha en melding")
        }
        val prosesseringTask = krevTask(taskId, laasForEndring = true)
        if (prosesseringTask.versjon != versjon) {
            throw ProsesseringApiException(HttpStatus.CONFLICT, "Noen andre har endret tasken. Hent den på nytt.")
        }
        if (!handling.erTillattFra(prosesseringTask.status)) {
            throw ProsesseringApiException(
                HttpStatus.CONFLICT,
                "Handlingen $handling er ikke tillatt når tasken har status ${prosesseringTask.status}",
            )
        }
        if (!prosesseringRepository.endreStatus(taskId, versjon, handling.nyStatus)) {
            throw ProsesseringApiException(HttpStatus.CONFLICT, "Noen andre har endret tasken. Hent den på nytt.")
        }
        prosesseringRepository.leggTilHendelse(
            taskId = taskId,
            type = TaskLoggType.STATUS_ENDRET,
            melding = "${prosesseringTask.status} -> ${handling.nyStatus}",
            endretAv = navIdent,
            node = node,
        )
        if (handling == ProsesseringHandling.AVVIK) {
            prosesseringRepository.leggTilHendelse(
                taskId = taskId,
                type = TaskLoggType.AVVIK,
                melding = requireNotNull(avviksmelding),
                endretAv = navIdent,
                node = node,
            )
        }
        val oppdatertTask = krevTask(taskId)
        secureLogger.info("Prosessering: handling={} utført for task={} av NAVident={}", handling, taskId, navIdent)
        return oppdatertTask
    }

    private fun krevTask(
        taskId: Long,
        laasForEndring: Boolean = false,
    ): ProsesseringTask =
        prosesseringRepository.hentTask(taskId, laasForEndring)
            ?: throw ProsesseringApiException(HttpStatus.NOT_FOUND, "Tasken finnes ikke")
}

class ProsesseringApiException(
    val status: HttpStatus,
    val detail: String,
) : RuntimeException(detail)
