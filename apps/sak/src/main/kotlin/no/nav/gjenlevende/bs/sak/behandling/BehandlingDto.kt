package no.nav.gjenlevende.bs.sak.behandling

import no.nav.gjenlevende.bs.sak.endringshistorikk.BehandlingEndring
import java.time.LocalDateTime
import java.util.UUID

data class BehandlingDto(
    val id: UUID,
    val fagsakId: UUID,
    val forrigeBehandlingId: UUID? = null,
    val status: BehandlingStatus,
    val sistEndret: LocalDateTime,
    val sistEndretAv: String,
    val opprettet: LocalDateTime,
    val opprettetAv: String,
    val resultat: BehandlingResultat,
)

fun Behandling.tilDto(sisteEndring: BehandlingEndring? = null): BehandlingDto =
    BehandlingDto(
        id = this.id,
        fagsakId = this.fagsakId,
        forrigeBehandlingId = this.forrigeBehandlingId,
        status = this.status,
        sistEndret = sisteEndring?.utførtTid ?: this.sporbar.endret.endretTid,
        sistEndretAv = sisteEndring?.utførtAv ?: this.sporbar.opprettetAv,
        opprettet = this.sporbar.opprettetTid,
        opprettetAv = this.sporbar.opprettetAv,
        resultat = this.resultat,
    )
