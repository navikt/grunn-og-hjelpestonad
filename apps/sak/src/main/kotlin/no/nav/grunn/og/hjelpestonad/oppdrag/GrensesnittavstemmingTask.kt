package no.nav.grunn.og.hjelpestonad.oppdrag

import efterlatte.prosessering.RetryPolicy
import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskStep
import no.nav.grunn.og.hjelpestonad.task.lagTaskType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID
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
        val avstemmingsvindu = kontekst.payload
        logger.info(
            "Starter grensesnittavstemming id={} fra={} til={}",
            avstemmingsvindu.avstemmingId,
            avstemmingsvindu.fraDato,
            avstemmingsvindu.tilDato,
        )
        oppdragClient.grensesnittavstem(
            GrensesnittavstemmingRequest(
                fagsystem = GrunnOgHjelpFagsystem.IT28.kode,
                fra = avstemmingsvindu.fraDato.atStartOfDay(),
                til = avstemmingsvindu.tilDato.atStartOfDay(),
                avstemmingId = avstemmingsvindu.avstemmingId,
            ),
        )

        val nesteAvstemmingsvindu = nesteGrensesnittavstemmingVindu(avstemmingsvindu, UUID.randomUUID())
        kontekst.opprettNesteTask(
            type = TYPE,
            payload = nesteAvstemmingsvindu,
            triggerTid = tidspunktForGrensesnittavstemming(nesteAvstemmingsvindu.tilDato),
        )
        logger.info("Grensesnittavstemming id={} sendt, neste vindu opprettet i task-transaksjonen", avstemmingsvindu.avstemmingId)
    }

    companion object {
        val TYPE = lagTaskType<GrensesnittavstemmingTaskData>("grensesnittavstemming")
        private val logger = LoggerFactory.getLogger(GrensesnittavstemmingTask::class.java)
    }
}
