package no.nav.grunn.og.hjelpestonad.faktagrunnlag.pdl

import no.nav.grunn.og.hjelpestonad.behandling.Behandling
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakService
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.pdl.PdlService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PdlFaktagrunnlagService(
    private val pdlService: PdlService,
    private val behandlingService: BehandlingService,
    private val ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
    private val fagsakService: FagsakService,
    private val pdlFaktagrunnlagDbService: PdlFaktagrunnlagDbService,
) {
    fun innhentFaktagrunnlagFraPdl(behandlingId: UUID): PdlFaktagrunnlag {
        behandlingService.validerBehandlingErRedigerbar(behandlingId)
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId) ?: error("Fant ikke behandling med id=$behandlingId")
        return innhentFaktagrunnlagFraPdl(behandling)
    }

    fun hent(behandlingId: UUID): PdlFaktagrunnlag? = pdlFaktagrunnlagDbService.hent(behandlingId)

    fun innhentFaktagrunnlagFraPdl(behandling: Behandling): PdlFaktagrunnlag {
        val person = pdlService.hentFaktagrunnlag(fagsakService.hentAktivIdent(behandling.fagsakId))
        return pdlFaktagrunnlagDbService.erstatt(behandling.id, person)
    }
}
