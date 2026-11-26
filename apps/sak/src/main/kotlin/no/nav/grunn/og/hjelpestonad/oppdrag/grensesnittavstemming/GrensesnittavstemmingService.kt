package no.nav.grunn.og.hjelpestonad.oppdrag.grensesnittavstemming

import efterlatte.prosessering.TaskId
import efterlatte.prosessering.spring.TaskService
import org.springframework.stereotype.Service

@Service
class GrensesnittavstemmingService(
    private val taskService: TaskService,
) {
    /**
     * Eksplisitt oppstart, uten kobling til deploy eller HTTP. Operatørinngang avtales før aktivering.
     * Kontroller at ingen kjede allerede er aktiv. En stoppet task må håndteres separat etter undersøkt feil.
     */
    fun opprettFørsteTask(grensesnittavstemmingTaskData: GrensesnittavstemmingTaskData): TaskId =
        taskService.opprettIEgenTransaksjon(
            type = GrensesnittavstemmingTask.TYPE,
            payload = grensesnittavstemmingTaskData,
            triggerTid = tidspunktForGrensesnittavstemming(grensesnittavstemmingTaskData.tilDato),
        )
}
