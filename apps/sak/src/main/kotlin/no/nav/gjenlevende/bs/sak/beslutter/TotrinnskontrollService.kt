package no.nav.gjenlevende.bs.sak.beslutter

import no.nav.gjenlevende.bs.sak.behandling.BehandlingRepository
import no.nav.gjenlevende.bs.sak.behandling.BehandlingStatus
import no.nav.gjenlevende.bs.sak.beslutter.dto.TotrinnskontrollDto
import no.nav.gjenlevende.bs.sak.beslutter.dto.TotrinnskontrollStatus
import no.nav.gjenlevende.bs.sak.beslutter.dto.TotrinnskontrollStatusDto
import no.nav.gjenlevende.bs.sak.endringshistorikk.BehandlingEndringRepository
import no.nav.gjenlevende.bs.sak.endringshistorikk.EndringType
import no.nav.gjenlevende.bs.sak.felles.sikkerhet.SikkerhetContext
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TotrinnskontrollService(
    private val behandlingEndringRepository: BehandlingEndringRepository,
    private val behandlingRepository: BehandlingRepository,
) {
    fun hentTotrinnskontrollStatus(behandlingId: UUID): TotrinnskontrollStatusDto {
        val behandling =
            behandlingRepository.findByIdOrNull(behandlingId)
                ?: throw IllegalStateException("Finner ikke behandling med id=$behandlingId")

        return when (behandling.status) {
            BehandlingStatus.FATTER_VEDTAK -> finnStatusForVedtakSomSkalFattes(behandlingId)
            BehandlingStatus.UTREDES -> finnStatusForVedtakSomErFattet(behandlingId)
            else -> TotrinnskontrollStatusDto(TotrinnskontrollStatus.UAKTUELT)
        }
    }

    fun hentSaksbehandlerSomSendteTilBeslutter(behandlingId: UUID): String {
        val sisteEndring =
            behandlingEndringRepository.finnSisteForBehandlingMedType(
                behandlingId = behandlingId,
                endringType = EndringType.SENDT_TIL_BESLUTTER,
            ) ?: throw IllegalStateException("Fant ikke saksbehandler som sendte til beslutter")

        return sisteEndring.utførtAv
    }

    fun validerAtBeslutterIkkeErSammeSomSaksbehandler(behandlingId: UUID) {
        val innloggetSaksbehandler = SikkerhetContext.hentSaksbehandler()
        val saksbehandlerSomSendteTilBeslutter = hentSaksbehandlerSomSendteTilBeslutter(behandlingId)

        if (innloggetSaksbehandler == saksbehandlerSomSendteTilBeslutter) {
            throw IllegalStateException("Beslutter kan ikke behandle en behandling som den selv har sendt til beslutter")
        }
    }

    private fun finnStatusForVedtakSomSkalFattes(behandlingId: UUID): TotrinnskontrollStatusDto {
        val sisteEndring =
            behandlingEndringRepository.finnSisteForBehandlingMedType(
                behandlingId = behandlingId,
                endringType = EndringType.SENDT_TIL_BESLUTTER,
            ) ?: return TotrinnskontrollStatusDto(TotrinnskontrollStatus.UAKTUELT)

        val innloggetSaksbehandler = SikkerhetContext.hentSaksbehandler()
        val erSammeSomSaksbehandler = innloggetSaksbehandler == sisteEndring.utførtAv

        return if (erSammeSomSaksbehandler) {
            TotrinnskontrollStatusDto(
                status = TotrinnskontrollStatus.IKKE_AUTORISERT,
                totrinnskontroll =
                    TotrinnskontrollDto(
                        opprettetAv = sisteEndring.utførtAv,
                        opprettetTid = sisteEndring.utførtTid,
                    ),
            )
        } else {
            TotrinnskontrollStatusDto(TotrinnskontrollStatus.KAN_FATTE_VEDTAK)
        }
    }

    private fun finnStatusForVedtakSomErFattet(behandlingId: UUID): TotrinnskontrollStatusDto {
        val sisteUnderkjentEndring =
            behandlingEndringRepository.finnSisteForBehandlingMedType(
                behandlingId = behandlingId,
                endringType = EndringType.BESLUTTER_UNDERKJENT,
            ) ?: return TotrinnskontrollStatusDto(TotrinnskontrollStatus.UAKTUELT)

        return TotrinnskontrollStatusDto(
            status = TotrinnskontrollStatus.TOTRINNSKONTROLL_UNDERKJENT,
            totrinnskontroll =
                TotrinnskontrollDto(
                    opprettetAv = sisteUnderkjentEndring.utførtAv,
                    opprettetTid = sisteUnderkjentEndring.utførtTid,
                    godkjent = false,
                    årsakUnderkjent = sisteUnderkjentEndring.årsakUnderkjent,
                    begrunnelse = sisteUnderkjentEndring.begrunnelseUnderkjent,
                ),
        )
    }
}
