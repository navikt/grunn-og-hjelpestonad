package no.nav.gjenlevende.bs.sak.oppgave

import no.nav.gjenlevende.bs.sak.behandling.BehandlingRepository
import no.nav.gjenlevende.bs.sak.felles.sikkerhet.SikkerhetContext
import no.nav.gjenlevende.bs.sak.infrastruktur.exception.ManglerTilgang
import no.nav.gjenlevende.bs.sak.oppgave.dto.AnsvarligSaksbehandlerDto
import no.nav.gjenlevende.bs.sak.oppgave.dto.SaksbehandlerRolle
import no.nav.gjenlevende.bs.sak.saksbehandler.EntraProxyClient
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AnsvarligSaksbehandlerService(
    private val oppgaveService: OppgaveService,
    private val oppgaveClient: OppgaveClient,
    private val entraProxyClient: EntraProxyClient,
    private val behandlingRepository: BehandlingRepository,
) {
    private val logger = LoggerFactory.getLogger(AnsvarligSaksbehandlerService::class.java)

    fun hentAnsvarligSaksbehandler(behandlingId: UUID): AnsvarligSaksbehandlerDto {
        val innloggetSaksbehandler = SikkerhetContext.hentSaksbehandler()
        val ansvarligSaksbehandler = hentAnsvarligSaksbehandlerIdent(behandlingId)

        if (ansvarligSaksbehandler.isNullOrBlank()) {
            return AnsvarligSaksbehandlerDto(
                fornavn = "",
                etternavn = "",
                rolle = SaksbehandlerRolle.IKKE_SATT,
            )
        }

        val saksbehandlerInfo = entraProxyClient.hentSaksbehandlerInfo(ansvarligSaksbehandler)
        val rolle =
            if (ansvarligSaksbehandler == innloggetSaksbehandler) {
                SaksbehandlerRolle.INNLOGGET_SAKSBEHANDLER
            } else {
                SaksbehandlerRolle.ANNEN_SAKSBEHANDLER
            }

        return AnsvarligSaksbehandlerDto(
            fornavn = saksbehandlerInfo.fornavn,
            etternavn = saksbehandlerInfo.etternavn,
            rolle = rolle,
        )
    }

    fun validerErAnsvarligSaksbehandler(behandlingId: UUID) {
        val innloggetSaksbehandler = SikkerhetContext.hentSaksbehandler()
        val ansvarligIdent = hentAnsvarligSaksbehandlerIdent(behandlingId)

        if (ansvarligIdent.isNullOrBlank() || ansvarligIdent != innloggetSaksbehandler) {
            throw ManglerTilgang("Innlogget saksbehandler $innloggetSaksbehandler er ikke ansvarlig saksbehandler for behandling $behandlingId")
        }
    }

    private fun hentAnsvarligSaksbehandlerIdent(behandlingId: UUID): String? {
        val oppgave = oppgaveService.hentOppgaveForBehandling(behandlingId)

        return if (oppgave != null) {
            val gosysOppgave = oppgaveClient.hentOppgaveM2M(oppgave.gsakOppgaveId)
            gosysOppgave.tilordnetRessurs
        } else {
            logger.info("Ingen oppgave funnet for behandling=$behandlingId, bruker opprettetAv fra behandling")
            val behandling = behandlingRepository.findByIdOrNull(behandlingId) ?: throw IllegalStateException("Finner ikke behandling med id=$behandlingId")

            behandling.sporbar.opprettetAv
        }
    }
}
