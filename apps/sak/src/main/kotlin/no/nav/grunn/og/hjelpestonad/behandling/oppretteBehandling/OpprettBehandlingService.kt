package no.nav.grunn.og.hjelpestonad.behandling.oppretteBehandling

import no.nav.grunn.og.hjelpestonad.behandling.Behandling
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingResultat
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingStatus
import no.nav.grunn.og.hjelpestonad.behandling.LagBehandleSakOppgaveTask
import no.nav.grunn.og.hjelpestonad.behandlingsgrunnlag.pdl.PdlBehandlingsgrunnlagService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringType
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.felles.sikkerhet.SikkerhetContext
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OpprettBehandlingService(
    private val behandlingService: BehandlingService,
    private val lagBehandleSakOppgaveTask: LagBehandleSakOppgaveTask,
    private val endringshistorikkService: EndringshistorikkService,
    private val pdlBehandlingsgrunnlagService: PdlBehandlingsgrunnlagService,
) {
    @Transactional
    fun opprettBehandling(
        fagsakId: UUID,
        status: BehandlingStatus = BehandlingStatus.OPPRETTET,
    ): Behandling {
        if (behandlingService.finnesÅpenBehandling(fagsakId)) {
            throw Feil("Finnes åpen behandling")
        }

        val forrigeBehandlingId = behandlingService.finnSisteIverksatteBehandling(fagsakId)?.id

        val behandling =
            behandlingService.opprett(
                Behandling(
                    fagsakId = fagsakId,
                    status = status,
                    resultat = BehandlingResultat.IKKE_SATT,
                    forrigeBehandlingId = forrigeBehandlingId,
                ),
            )

        lagBehandleSakOppgaveTask.opprettBehandleSakOppgaveTask(behandling = behandling, saksbehandler = SikkerhetContext.hentSaksbehandler())

        endringshistorikkService.registrerEndring(
            behandlingId = behandling.id,
            endringType = EndringType.BEHANDLING_OPPRETTET,
        )

        pdlBehandlingsgrunnlagService.innhentOgLagre(behandling)

        return behandling
    }
}
