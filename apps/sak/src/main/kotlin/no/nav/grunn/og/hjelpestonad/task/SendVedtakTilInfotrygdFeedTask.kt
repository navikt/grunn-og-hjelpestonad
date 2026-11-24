package no.nav.grunn.og.hjelpestonad.task

import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskStep
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakService
import no.nav.grunn.og.hjelpestonad.infotrygd.InfotrygdFeedClient
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.TilkjentYtelseService
import no.nav.grunn.og.hjelpestonad.vedtak.ResultatType
import no.nav.grunn.og.hjelpestonad.vedtak.VedtakService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SendVedtakTilInfotrygdFeedTask(
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService,
    private val fagsakService: FagsakService,
    private val infotrygdFeedClient: InfotrygdFeedClient,
    private val tilkjentYtelseService: TilkjentYtelseService,
) : TaskStep<UUID> {
    override val type = TYPE

    override fun utfor(kontekst: TaskKontekst<UUID>) {
        val behandlingId = kontekst.payload
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
            tilkjentYtelseService
                .hentTilkjentYtelse(behandlingId)
                ?.andelerTilkjentYtelse
                ?.minOfOrNull { it.fom }
                ?: error("Fant ingen stønadsperiode for innvilget vedtak med behandlingId=$behandlingId")
        val personIdent = fagsakService.hentAktivIdent(behandling.fagsakId)

        infotrygdFeedClient.sendVedtak(personIdent, datoStartNyGrunnstønad)
    }

    companion object {
        val TYPE = lagUuidTaskType("SendVedtakTilInfotrygdFeedTask")
    }
}
