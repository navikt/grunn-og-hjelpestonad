package no.nav.gjenlevende.bs.sak.endringshistorikk

import no.nav.gjenlevende.bs.sak.beslutter.ÅrsakUnderkjent
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class EndringshistorikkService(
    private val behandlingEndringRepository: BehandlingEndringRepository,
) {
    fun registrerEndring(
        behandlingId: UUID,
        endringType: EndringType,
        detaljer: String? = null,
    ) {
        behandlingEndringRepository.insert(
            BehandlingEndring(
                behandlingId = behandlingId,
                endringType = endringType,
                detaljer = detaljer,
            ),
        )
    }

    fun registrerUnderkjennelse(
        behandlingId: UUID,
        årsakUnderkjent: ÅrsakUnderkjent,
        begrunnelse: String,
    ) {
        behandlingEndringRepository.insert(
            BehandlingEndring(
                behandlingId = behandlingId,
                endringType = EndringType.BESLUTTER_UNDERKJENT,
                årsakUnderkjent = årsakUnderkjent,
                begrunnelseUnderkjent = begrunnelse,
            ),
        )
    }

    fun hentEndringshistorikk(behandlingId: UUID): List<BehandlingEndring> = behandlingEndringRepository.finnAlleForBehandling(behandlingId)

    fun hentSisteEndring(behandlingId: UUID): BehandlingEndring? = behandlingEndringRepository.finnSisteForBehandling(behandlingId)
}
