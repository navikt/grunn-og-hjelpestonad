package no.nav.gjenlevende.bs.sak.endringshistorikk

import java.time.LocalDateTime
import java.util.UUID

data class BehandlingEndringDto(
    val id: UUID,
    val behandlingId: UUID,
    val endringType: EndringType,
    val utførtAv: String,
    val utførtTid: LocalDateTime,
    val detaljer: String?,
)

fun BehandlingEndring.tilDto() =
    BehandlingEndringDto(
        id = this.id,
        behandlingId = this.behandlingId,
        endringType = this.endringType,
        utførtAv = this.utførtAv,
        utførtTid = this.utførtTid,
        detaljer = this.detaljer,
    )
