package no.nav.grunn.og.hjelpestonad.behandling.oppretteBehandling

import efterlatte.prosessering.spring.TaskService
import no.nav.grunn.og.hjelpestonad.behandling.Behandling
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingResultat
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingStatus
import no.nav.grunn.og.hjelpestonad.behandling.LagBehandleSakOppgaveTask
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringType
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.faktagrunnlag.pdl.PdlFaktagrunnlagService
import no.nav.grunn.og.hjelpestonad.felles.sikkerhet.SikkerhetContext
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.task.SendStartBehandlingTilInfotrygdFeedTask
import no.nav.grunn.og.hjelpestonad.vilkår.KopierVilkårService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OpprettBehandlingService(
    private val behandlingService: BehandlingService,
    private val lagBehandleSakOppgaveTask: LagBehandleSakOppgaveTask,
    private val endringshistorikkService: EndringshistorikkService,
    private val pdlFaktagrunnlagService: PdlFaktagrunnlagService,
    private val taskService: TaskService,
    private val kopierVilkårService: KopierVilkårService,
) {
    @Transactional
    fun opprettBehandling(
        fagsakId: UUID,
        status: BehandlingStatus = BehandlingStatus.UTREDES,
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

        if (forrigeBehandlingId == null) {
            taskService.opprett(SendStartBehandlingTilInfotrygdFeedTask.TYPE, behandling.id)
        } else {
            kopierVilkårService.kopierFraForrigeBehandling(forrigeBehandlingId = forrigeBehandlingId, behandlingId = behandling.id)
        }

        lagBehandleSakOppgaveTask.opprettBehandleSakOppgaveTask(behandling = behandling, saksbehandler = SikkerhetContext.hentSaksbehandler())

        endringshistorikkService.registrerEndring(
            behandlingId = behandling.id,
            endringType = EndringType.BEHANDLING_OPPRETTET,
        )

        pdlFaktagrunnlagService.innhentFaktagrunnlagFraPdl(behandling)

        return behandling
    }
}
