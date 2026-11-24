package no.nav.grunn.og.hjelpestonad.oppdrag

import efterlatte.prosessering.RetryPolicy
import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskStep
import no.nav.grunn.og.hjelpestonad.task.lagTaskType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Poller til OS har kvittert. Ventetiden starter på 10 sekunder og dobles opp til 15 minutter.
 * Samlet planlagt ventetid er høyst 24 timer, uavhengig av miljø og åpningstid.
 */
@Service
class HentOppdragStatusTask(
    private val oppdragIverksettingService: OppdragIverksettingService,
) : TaskStep<HentOppdragStatusTask.HentOppdragStatusTaskData> {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override val type = TYPE
    override val retry = RetryPolicy(maxAntallFeil = VENTETIDER.size + 1) { antallFeil -> VENTETIDER[antallFeil - 1] }

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

        private val VENTETIDER =
            buildList {
                var ventetid = 10.seconds
                var samletVentetid = Duration.ZERO
                while (samletVentetid + ventetid <= 24.hours) {
                    add(ventetid)
                    samletVentetid += ventetid
                    ventetid = (ventetid * 2).coerceAtMost(15.minutes)
                }
            }
    }

    data class HentOppdragStatusTaskData(
        val behandlingId: UUID,
    )
}
