package no.nav.grunn.og.hjelpestonad.behandling.årsak

import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingStatus
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingSteg
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringType
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ÅrsakBehandlingService(
    private val årsakBehandlingRepository: ÅrsakBehandlingRepository,
    private val behandlingService: BehandlingService,
    private val endringshistorikkService: EndringshistorikkService,
    private val ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
) {
    fun hentÅrsakBehandling(behandlingId: UUID): ÅrsakBehandling? = årsakBehandlingRepository.findById(behandlingId).orElse(null)

    @Transactional
    fun lagreÅrsakForBehandling(
        behandlingId: UUID,
        årsakBehandlingRequest: ÅrsakBehandlingRequest,
    ): ÅrsakBehandling {
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
        behandlingService.validerKanEndreSteg(behandlingId, BehandlingSteg.ÅRSAK)
        val eksisterendeÅrsak = hentÅrsakBehandling(behandlingId) ?: return lagreNyÅrsak(behandlingId, årsakBehandlingRequest)

        val oppdatertÅrsak =
            eksisterendeÅrsak.copy(
                kravdato = årsakBehandlingRequest.kravdato,
                årsak = årsakBehandlingRequest.årsak,
                beskrivelse = årsakBehandlingRequest.beskrivelse,
            )
        if (oppdatertÅrsak == eksisterendeÅrsak) {
            return eksisterendeÅrsak
        }
        return oppdaterÅrsak(behandlingId, oppdatertÅrsak)
    }

    private fun lagreNyÅrsak(
        behandlingId: UUID,
        årsakBehandlingRequest: ÅrsakBehandlingRequest,
    ): ÅrsakBehandling {
        behandlingService.oppdaterBehandlingStatus(
            behandlingId = behandlingId,
            status = BehandlingStatus.UTREDES,
        )
        val lagretÅrsak =
            årsakBehandlingRepository.insert(
                ÅrsakBehandling(
                    behandlingId = behandlingId,
                    kravdato = årsakBehandlingRequest.kravdato,
                    årsak = årsakBehandlingRequest.årsak,
                    beskrivelse = årsakBehandlingRequest.beskrivelse,
                ),
            )
        endringshistorikkService.registrerEndring(
            behandlingId = behandlingId,
            endringType = EndringType.ÅRSAK_LAGRET,
            detaljer = "Årsak: ${årsakBehandlingRequest.årsak}",
        )
        behandlingService.settNåværendeSteg(behandlingId, BehandlingSteg.INNGANGSVILKÅR)
        return lagretÅrsak
    }

    private fun oppdaterÅrsak(
        behandlingId: UUID,
        oppdatertÅrsak: ÅrsakBehandling,
    ): ÅrsakBehandling {
        behandlingService.oppdaterBehandlingStatus(
            behandlingId = behandlingId,
            status = BehandlingStatus.UTREDES,
        )
        val lagretÅrsak = årsakBehandlingRepository.update(oppdatertÅrsak)
        endringshistorikkService.registrerEndring(
            behandlingId = behandlingId,
            endringType = EndringType.ÅRSAK_OPPDATERT,
            detaljer = "Årsak: ${oppdatertÅrsak.årsak}",
        )
        behandlingService.settNåværendeSteg(behandlingId, BehandlingSteg.INNGANGSVILKÅR)
        return lagretÅrsak
    }
}
