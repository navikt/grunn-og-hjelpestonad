package no.nav.grunn.og.hjelpestonad.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakService
import no.nav.grunn.og.hjelpestonad.infotrygd.InfotrygdFeedClient
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = SendStartBehandlingTilInfotrygdFeedTask.TYPE,
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
    beskrivelse = "Sender startbehandling til Infotrygd-feed",
)
class SendStartBehandlingTilInfotrygdFeedTask(
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val infotrygdFeedClient: InfotrygdFeedClient,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val behandling =
            behandlingService.hentBehandling(behandlingId)
                ?: error("Fant ikke behandling med id=$behandlingId")
        val personIdent = fagsakService.hentAktivIdent(behandling.fagsakId)

        infotrygdFeedClient.sendStartBehandling(personIdent)
    }

    companion object {
        const val TYPE = "SendStartBehandlingTilInfotrygdFeedTask"

        fun opprettTask(behandlingId: UUID): Task = Task(TYPE, behandlingId.toString())
    }
}
