package no.nav.grunn.og.hjelpestonad.oppdrag

import efterlatte.prosessering.RetryPolicy
import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskStep
import no.nav.grunn.og.hjelpestonad.task.lagTaskType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

/**
 * Sender oppdraget for en godkjent behandling til OS, og oppretter [HentOppdragStatusTask] når oppdraget er sendt.
 *
 * Et nytt forsøk etter feil sender det lagrede oppdraget på nytt bare når statusen fortsatt er `OPPRETTET`. Var det
 * sendt før, svarer `familie-oppdrag-backend` med 409, og tasken går videre som om sendingen lyktes. Er statusen
 * `LAGT_PÅ_KØ` eller en kvittering, sendes ingenting.
 */
@Service
class IverksettOppdragTask(
    private val oppdragIverksettingService: OppdragIverksettingService,
    private val clock: Clock = Clock.systemDefaultZone(),
) : TaskStep<IverksettOppdragTask.IverksettOppdragTaskData> {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override val type = TYPE
    override val retry = RetryPolicy(maxAntallFeil = 3) { 15.minutes }

    override fun utfor(kontekst: TaskKontekst<IverksettOppdragTaskData>) {
        val behandlingId = kontekst.payload.behandlingId
        val iverksettingsresultat = oppdragIverksettingService.iverksett(behandlingId, kontekst.payload.saksbehandlerId)

        when (iverksettingsresultat) {
            is Iverksettingsresultat.Sendt -> {
                opprettHentOppdragStatusTask(kontekst, behandlingId)
            }

            is Iverksettingsresultat.AlleredeIverksatt -> {
                // Testendepunktet sender uten å opprette statustask. Har OS kvittert, er statusen hentet fra før.
                if (iverksettingsresultat.oppdragResultat.status == OppdragStatus.LAGT_PÅ_KØ) {
                    opprettHentOppdragStatusTask(kontekst, behandlingId)
                }
            }

            // Ingenting er sendt til OS, så det finnes ingen kvittering å hente. Tasken er ferdig.
            Iverksettingsresultat.IngentingÅSende,
            is Iverksettingsresultat.IngenEndring,
            -> {
                logger.info("Behandling={} er ikke iverksatt mot oppdrag fordi den ikke endrer utbetalingen", behandlingId)
            }
        }
    }

    private fun opprettHentOppdragStatusTask(
        kontekst: TaskKontekst<IverksettOppdragTaskData>,
        behandlingId: UUID,
    ) {
        kontekst.opprettNesteTask(
            type = HentOppdragStatusTask.TYPE,
            payload = HentOppdragStatusTask.HentOppdragStatusTaskData(behandlingId = behandlingId),
            triggerTid = nesteTidspunktIÅpningstid(clock.instant(), VENTETID_FØR_STATUS),
        )
    }

    companion object {
        val TYPE = lagTaskType<IverksettOppdragTaskData>("iverksettOppdrag")
        private val VENTETID_FØR_STATUS: Duration = Duration.ofSeconds(30)
    }

    /**
     * @param saksbehandlerId saksbehandleren som sendte behandlingen til beslutter, som i EF. Står i oppdraget som
     * `saksbehandlerId`, som `familie-oppdrag-backend` også bruker som attestant i OS.
     */
    data class IverksettOppdragTaskData(
        val behandlingId: UUID,
        val saksbehandlerId: String,
    )
}
