package no.nav.gjenlevende.bs.sak.endringshistorikk

import no.nav.gjenlevende.bs.sak.beslutter.ÅrsakUnderkjent
import no.nav.gjenlevende.bs.sak.felles.sikkerhet.SikkerhetContext
import no.nav.gjenlevende.bs.sak.felles.sporbar.SporbarUtils
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.UUID

enum class EndringType {
    BEHANDLING_OPPRETTET,
    SENDT_TIL_BESLUTTER,
    ANGRET_SEND_TIL_BESLUTTER,
    VILKÅR_VURDERING_OPPRETTET,
    VILKÅR_VURDERING_OPPDATERT,
    VEDTAK_LAGRET,
    ÅRSAK_LAGRET,
    ÅRSAK_OPPDATERT,
    BESLUTTER_GODKJENT,
    BESLUTTER_UNDERKJENT,
    BEHANDLING_HENLAGT,
}

@Table("behandling_endring")
data class BehandlingEndring(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    @Column("endring_type")
    val endringType: EndringType,
    @Column("utfort_av")
    val utførtAv: String = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
    @Column("utfort_tid")
    val utførtTid: LocalDateTime = SporbarUtils.now(),
    val detaljer: String? = null,
    @Column("arsak_underkjent")
    val årsakUnderkjent: ÅrsakUnderkjent? = null,
    @Column("begrunnelse_underkjent")
    val begrunnelseUnderkjent: String? = null,
)
