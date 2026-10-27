package no.nav.grunn.og.hjelpestonad.task

import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskStep
import efterlatte.prosessering.TaskType
import efterlatte.prosessering.strengType
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakService
import no.nav.grunn.og.hjelpestonad.infotrygd.InfotrygdFeedClient
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SendStartBehandlingTilInfotrygdFeedTask(
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val infotrygdFeedClient: InfotrygdFeedClient,
) : TaskStep<String> {
    override val type: TaskType<String> = TYPE

    override fun utfor(kontekst: TaskKontekst<String>) {
        val behandlingId = UUID.fromString(kontekst.payload)
        val behandling =
            behandlingService.hentBehandling(behandlingId)
                ?: error("Fant ikke behandling med id=$behandlingId")
        val personIdent = fagsakService.hentAktivIdent(behandling.fagsakId)

        infotrygdFeedClient.sendStartBehandling(personIdent)
    }

    companion object {
        val TYPE: TaskType<String> = strengType("SendStartBehandlingTilInfotrygdFeedTask")
    }
}
