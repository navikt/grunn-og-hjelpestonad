package no.nav.grunn.og.hjelpestonad.behandlingsgrunnlag.pdl

import no.nav.grunn.og.hjelpestonad.behandling.Behandling
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakService
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.pdl.PdlService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PdlBehandlingsgrunnlagService(
    private val pdlService: PdlService,
    private val behandlingService: BehandlingService,
    private val ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
    private val fagsakService: FagsakService,
    private val pdlBehandlingsgrunnlagDbService: PdlBehandlingsgrunnlagDbService,
) {
    fun innhentBehandlingsgrunnlagFraPdl(behandlingId: UUID): PdlBehandlingsgrunnlag {
        behandlingService.validerBehandlingErRedigerbar(behandlingId)
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId) ?: error("Fant ikke behandling med id=$behandlingId")
        return innhentBehandlingsgrunnlagFraPdl(behandling)
    }

    fun hent(behandlingId: UUID): PdlBehandlingsgrunnlag? = pdlBehandlingsgrunnlagDbService.hent(behandlingId)

    fun innhentBehandlingsgrunnlagFraPdl(behandling: Behandling): PdlBehandlingsgrunnlag {
        val person = pdlService.hentBehandlingsgrunnlag(fagsakService.hentAktivIdent(behandling.fagsakId))
        return pdlBehandlingsgrunnlagDbService.erstatt(behandling.id, person)
    }
}
