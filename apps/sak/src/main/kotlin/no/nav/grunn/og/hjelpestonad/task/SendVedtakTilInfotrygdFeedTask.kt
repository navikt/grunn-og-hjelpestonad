package no.nav.grunn.og.hjelpestonad.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakService
import no.nav.grunn.og.hjelpestonad.infotrygd.InfotrygdFeedClient
import no.nav.grunn.og.hjelpestonad.vedtak.PeriodetypeBarnetilsyn
import no.nav.grunn.og.hjelpestonad.vedtak.ResultatType
import no.nav.grunn.og.hjelpestonad.vedtak.VedtakService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = SendVedtakTilInfotrygdFeedTask.TYPE,
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
    beskrivelse = "Sender innvilget vedtak til Infotrygd-feed",
)
class SendVedtakTilInfotrygdFeedTask(
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService,
    private val fagsakService: FagsakService,
    private val infotrygdFeedClient: InfotrygdFeedClient,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val behandling =
            behandlingService.hentBehandling(behandlingId)
                ?: error("Fant ikke behandling med id=$behandlingId")
        val vedtak =
            vedtakService.hentVedtak(behandlingId)
                ?: error("Fant ikke vedtak for behandling med id=$behandlingId")

        check(vedtak.resultatType == ResultatType.INNVILGET) {
            "Kan ikke sende vedtak med resultat ${vedtak.resultatType} til Infotrygd-feed"
        }

        val datoStartNyGrunnstønad =
            vedtak.grunnstønadPerioder
                .asSequence()
                .filter { it.periodetype == PeriodetypeBarnetilsyn.ORDINÆR }
                .minOfOrNull { it.datoFra }
                ?.atDay(1)
                ?: error("Fant ingen stønadsperiode for innvilget vedtak med behandlingId=$behandlingId")
        val personIdent = fagsakService.hentAktivIdent(behandling.fagsakId)

        infotrygdFeedClient.sendVedtak(personIdent, datoStartNyGrunnstønad)
    }

    companion object {
        const val TYPE = "SendVedtakTilInfotrygdFeedTask"

        fun opprettTask(behandlingId: UUID): Task = Task(TYPE, behandlingId.toString())
    }
}
