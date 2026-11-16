package no.nav.grunn.og.hjelpestonad.oppdrag

import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskStep
import no.nav.grunn.og.hjelpestonad.task.lagTaskType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.util.UUID

/**
 * Poller til OS har kvittert. Mens oppdraget ligger på kø, prøver tasken igjen hvert 15. minutt, men bare på hverdager
 * mellom 06 og 21, når OS behandler køen (se [nesteTidspunktIÅpningstid]). Det er samme regel som i familie-ba-sak.
 *
 * Feil status gjør at tasken feiler og stopper etter retries, eventuelt behov for annen funksjonalitet ved feil fikses senere.
 */
@Service
class HentOppdragStatusTask(
    private val oppdragIverksettingService: OppdragIverksettingService,
    private val clock: Clock = Clock.systemDefaultZone(),
) : TaskStep<HentOppdragStatusTask.HentOppdragStatusTaskData> {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override val type = TYPE

    override fun utfor(kontekst: TaskKontekst<HentOppdragStatusTaskData>) {
        val behandlingId = kontekst.payload.behandlingId
        val antallForsøk = kontekst.payload.forsøk
        val oppdragStatusSvar = oppdragIverksettingService.hentStatus(behandlingId)

        when (oppdragStatusSvar.status) {
            OppdragStatus.KVITTERT_OK -> {
                logger.info("Oppdrag for behandling={} er kvittert OK", behandlingId)
            }

            OppdragStatus.LAGT_PÅ_KØ -> {
                if (antallForsøk >= MAKS_FORSØK) {
                    throw IllegalStateException("Fikk ikke kvittering fra OS for behandling=$behandlingId etter $antallForsøk forsøk")
                }

                kontekst.opprettNesteTask<HentOppdragStatusTaskData>(
                    type = TYPE,
                    payload = HentOppdragStatusTaskData(behandlingId = behandlingId, forsøk = antallForsøk + 1),
                    triggerTid = nesteTidspunktIÅpningstid(clock.instant(), VENTETID),
                )
            }

            else -> {
                throw IllegalStateException("Oppdrag for behandling=$behandlingId fikk status=${oppdragStatusSvar.status}")
            }
        }
    }

    companion object {
        val TYPE = lagTaskType<HentOppdragStatusTaskData>("hentOppdragStatus")

        /** Som i familie-ba-sak. 100 forsøk hvert 15. minutt i åpningstiden er nesten to virkedager. */
        const val MAKS_FORSØK = 100
        private val VENTETID: Duration = Duration.ofMinutes(15)
    }

    data class HentOppdragStatusTaskData(
        val behandlingId: UUID,
        val forsøk: Int,
    )
}
