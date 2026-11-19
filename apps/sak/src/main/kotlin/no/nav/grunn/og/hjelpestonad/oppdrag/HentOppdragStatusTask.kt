package no.nav.grunn.og.hjelpestonad.oppdrag

import efterlatte.prosessering.RetryPolicy
import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskStep
import no.nav.grunn.og.hjelpestonad.task.lagTaskType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

/**
 * Poller til OS har kvittert. Samme task prøver igjen hvert 10. sekund ved manglende kvittering eller feil.
 * Motoren stopper den etter 100 feil.
 */
@Service
class HentOppdragStatusTask(
    private val oppdragIverksettingService: OppdragIverksettingService,
) : TaskStep<HentOppdragStatusTask.HentOppdragStatusTaskData> {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override val type = TYPE
    override val retry = RetryPolicy(maxAntallFeil = MAKS_FORSØK) { 10.seconds }

    override fun utfor(kontekst: TaskKontekst<HentOppdragStatusTaskData>) {
        val behandlingId = kontekst.payload.behandlingId
        val oppdragStatusSvar = oppdragIverksettingService.hentStatus(behandlingId)

        when (oppdragStatusSvar.status) {
            OppdragStatus.KVITTERT_OK -> {
                logger.info("Oppdrag for behandling={} er kvittert OK", behandlingId)
            }

            OppdragStatus.LAGT_PÅ_KØ -> {
                throw IllegalStateException("Venter på kvittering fra OS for behandling=$behandlingId")
            }

            else -> {
                throw IllegalStateException("Oppdrag for behandling=$behandlingId fikk status=${oppdragStatusSvar.status}")
            }
        }
    }

    companion object {
        val TYPE = lagTaskType<HentOppdragStatusTaskData>("hentOppdragStatus")

        const val MAKS_FORSØK = 100
    }

    data class HentOppdragStatusTaskData(
        val behandlingId: UUID,
    )
}
