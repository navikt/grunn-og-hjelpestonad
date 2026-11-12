package no.nav.grunn.og.hjelpestonad.oppdrag

import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskStep
import no.nav.grunn.og.hjelpestonad.task.lagTaskType
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.util.UUID

/**
 * Sender oppdraget for en godkjent behandling til OS, og oppretter [HentOppdragStatusTask] når oppdraget er sendt.
 *
 * Et nytt forsøk etter feil sender det lagrede oppdraget på nytt. Var det sendt før, svarer `familie-oppdrag-backend`
 * med 409, og tasken går videre som om sendingen lyktes.
 */
@Service
class IverksettOppdragTask(
    private val oppdragIverksettingService: OppdragIverksettingService,
    private val clock: Clock = Clock.systemDefaultZone(),
) : TaskStep<IverksettOppdragTask.IverksettOppdragTaskData> {
    override val type = TYPE

    override fun utfor(kontekst: TaskKontekst<IverksettOppdragTaskData>) {
        val behandlingId = kontekst.payload.behandlingId
        val iverksettingsresultat = oppdragIverksettingService.iverksett(behandlingId, kontekst.payload.saksbehandlerId)

        if (iverksettingsresultat?.sendt == true) {
            kontekst.opprettNesteTask(
                type = HentOppdragStatusTask.TYPE,
                payload = HentOppdragStatusTask.HentOppdragStatusTaskData(behandlingId = behandlingId, forsøk = 1),
                triggerTid = nesteTidspunktIÅpningstid(clock.instant(), VENTETID_FØR_STATUS),
            )
        }
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
