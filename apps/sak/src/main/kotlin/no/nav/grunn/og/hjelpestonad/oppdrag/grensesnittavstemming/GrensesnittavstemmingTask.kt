package no.nav.grunn.og.hjelpestonad.oppdrag.grensesnittavstemming

import efterlatte.prosessering.RetryPolicy
import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskStep
import no.nav.grunn.og.hjelpestonad.oppdrag.GrunnOgHjelpFagsystem
import no.nav.grunn.og.hjelpestonad.oppdrag.OppdragClient
import no.nav.grunn.og.hjelpestonad.task.lagTaskType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.time.Duration

/**
 * Avstemmer vanligvis mandagens oppdrag tirsdag kl. 08 norsk tid, og fredagens til søndagens oppdrag mandag kl. 08.
 * Vinduet går fra midnatt inkludert til midnatt ekskludert. Taskmotoren styrer kjøretidspunktet med triggerTid.
 */
@Service
class GrensesnittavstemmingTask(
    private val oppdragClient: OppdragClient,
) : TaskStep<GrensesnittavstemmingTaskData> {
    override val type = TYPE

    // Som i BA: et feilet kall kan ha sendt deler av eller hele avstemmingen. Undersøk før manuell omkjøring.
    // Reaper kan fortsatt gjenopprette tasken etter krasj. Se ADR-0019 om valget og aksepterte begrensninger.
    override val retry = RetryPolicy(maxAntallFeil = 1, backoff = { Duration.ZERO })

    override fun utfor(kontekst: TaskKontekst<GrensesnittavstemmingTaskData>) {
        val grensesnittavstemmingTaskData = kontekst.payload
        logger.info(
            "Starter grensesnittavstemming id={} fra={} til={}",
            grensesnittavstemmingTaskData.avstemmingId,
            grensesnittavstemmingTaskData.fraDato,
            grensesnittavstemmingTaskData.tilDato,
        )
        oppdragClient.grensesnittavstem(
            grensesnittavstemmingTaskData.tilGrensesnittavstemmingRequest(GrunnOgHjelpFagsystem.IT28),
        )

        val nesteGrensesnittavstemmingTaskData = nesteGrensesnittavstemmingVindu(grensesnittavstemmingTaskData)
        kontekst.opprettNesteTask(
            type = TYPE,
            payload = nesteGrensesnittavstemmingTaskData,
            triggerTid = tidspunktForGrensesnittavstemming(nesteGrensesnittavstemmingTaskData.tilDato),
        )
        logger.info("Grensesnittavstemming id={} sendt, neste vindu opprettet i task-transaksjonen", grensesnittavstemmingTaskData.avstemmingId)
    }

    companion object {
        val TYPE = lagTaskType<GrensesnittavstemmingTaskData>("grensesnittavstemming")
        private val logger = LoggerFactory.getLogger(GrensesnittavstemmingTask::class.java)
    }
}
