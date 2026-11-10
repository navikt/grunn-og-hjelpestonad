package no.nav.grunn.og.hjelpestonad.trygdetid

import no.nav.grunn.og.hjelpestonad.felles.periode.PeriodeMedBegrunnelseRequest
import java.time.LocalDate
import java.util.UUID

/** Uten sporbarhet og uten vurdering. Teller og kilde er det saksbehandler registrerer. */
data class TrygdetidResponse(
    val id: UUID,
    val behandlingId: UUID,
    val teller: Int,
    val kilde: TrygdetidKilde,
    val begrunnelse: String,
    val fraOgMedDato: LocalDate?,
    val tilOgMedDato: LocalDate?,
)

data class TrygdetidRequest(
    override val id: UUID? = null,
    val teller: Int,
    val kilde: TrygdetidKilde,
    override val begrunnelse: String = "",
    override val fraOgMedDato: LocalDate? = null,
    override val tilOgMedDato: LocalDate? = null,
) : PeriodeMedBegrunnelseRequest

fun Trygdetid.tilResponse() =
    TrygdetidResponse(
        id = this.id,
        behandlingId = this.behandlingId,
        teller = this.teller,
        kilde = this.kilde,
        begrunnelse = this.begrunnelse,
        fraOgMedDato = this.fraOgMedDato,
        tilOgMedDato = this.tilOgMedDato,
    )
