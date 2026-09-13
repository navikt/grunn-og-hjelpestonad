package no.nav.grunn.og.hjelpestonad.beslutter

import no.nav.grunn.og.hjelpestonad.behandling.BehandlingRepository
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingStatus
import no.nav.grunn.og.hjelpestonad.beslutter.dto.TotrinnskontrollDto
import no.nav.grunn.og.hjelpestonad.beslutter.dto.TotrinnskontrollStatus
import no.nav.grunn.og.hjelpestonad.beslutter.dto.TotrinnskontrollStatusResponse
import no.nav.grunn.og.hjelpestonad.endringshistorikk.BehandlingEndringRepository
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringType
import no.nav.grunn.og.hjelpestonad.felles.sikkerhet.SikkerhetContext
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TotrinnskontrollService(
    private val behandlingEndringRepository: BehandlingEndringRepository,
    private val behandlingRepository: BehandlingRepository,
) {
    fun hentTotrinnskontrollStatus(behandlingId: UUID): TotrinnskontrollStatusResponse {
        val behandling =
            behandlingRepository.findByIdOrNull(behandlingId)
                ?: throw IllegalStateException("Finner ikke behandling med id=$behandlingId")

        return when (behandling.status) {
            BehandlingStatus.FATTER_VEDTAK -> finnStatusForVedtakSomSkalFattes(behandlingId)
            BehandlingStatus.UTREDES -> finnStatusForVedtakSomErFattet(behandlingId)
            else -> TotrinnskontrollStatusResponse(TotrinnskontrollStatus.UAKTUELT)
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

    private fun finnStatusForVedtakSomSkalFattes(behandlingId: UUID): TotrinnskontrollStatusResponse {
        val sisteEndring =
            behandlingEndringRepository.finnSisteForBehandlingMedType(
                behandlingId = behandlingId,
                endringType = EndringType.SENDT_TIL_BESLUTTER,
            ) ?: return TotrinnskontrollStatusResponse(TotrinnskontrollStatus.UAKTUELT)

        val innloggetSaksbehandler = SikkerhetContext.hentSaksbehandler()
        val erSammeSomSaksbehandler = innloggetSaksbehandler == sisteEndring.utførtAv

        return if (erSammeSomSaksbehandler) {
            TotrinnskontrollStatusResponse(
                status = TotrinnskontrollStatus.IKKE_AUTORISERT,
                totrinnskontroll =
                    TotrinnskontrollDto(
                        opprettetAv = sisteEndring.utførtAv,
                        opprettetTid = sisteEndring.utførtTid,
                    ),
            )
        } else {
            TotrinnskontrollStatusResponse(TotrinnskontrollStatus.KAN_FATTE_VEDTAK)
        }
    }

    private fun finnStatusForVedtakSomErFattet(behandlingId: UUID): TotrinnskontrollStatusResponse {
        val sisteUnderkjentEndring =
            behandlingEndringRepository.finnSisteForBehandlingMedType(
                behandlingId = behandlingId,
                endringType = EndringType.BESLUTTER_UNDERKJENT,
            ) ?: return TotrinnskontrollStatusResponse(TotrinnskontrollStatus.UAKTUELT)

        return TotrinnskontrollStatusResponse(
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
