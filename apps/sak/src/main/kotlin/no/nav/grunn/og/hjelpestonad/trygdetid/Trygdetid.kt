package no.nav.grunn.og.hjelpestonad.trygdetid

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import no.nav.grunn.og.hjelpestonad.felles.periode.PeriodeMedBegrunnelse
import no.nav.grunn.og.hjelpestonad.felles.sporbar.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.util.UUID

enum class TrygdetidKilde {
    ALDERSPENSJON,
    UFØRETRYGD,
    MIGRERING,
    INGEN,
}

@Table("trygdetid")
data class Trygdetid(
    @Id
    override val id: UUID = UUID.randomUUID(),
    override val behandlingId: UUID,
    /** Trygdetiden i hele år, fra alderspensjonen eller uføretrygden. Er alltid 40 når kilden er [TrygdetidKilde.INGEN]. */
    @Column("antall_aar")
    val antallÅr: Int,
    val kilde: TrygdetidKilde,
    override val begrunnelse: String = "",
    override val fraOgMedDato: LocalDate? = null,
    override val tilOgMedDato: LocalDate? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    override val sporbar: Sporbar = Sporbar(),
) : PeriodeMedBegrunnelse<Trygdetid> {
    override fun kopierTilBehandling(behandlingId: UUID) = copy(id = UUID.randomUUID(), behandlingId = behandlingId, sporbar = Sporbar())

    override fun kopierMedTidsrom(
        id: UUID,
        fraOgMedDato: LocalDate?,
        tilOgMedDato: LocalDate?,
    ) = copy(id = id, fraOgMedDato = fraOgMedDato, tilOgMedDato = tilOgMedDato)
}

object TrygdetidRegler {
    /** Full trygdetid. Trygdetidsbrøken er antall år / [FULL_TRYGDETID_I_ÅR]. */
    const val FULL_TRYGDETID_I_ÅR = 40

    fun valider(request: TrygdetidRequest) {
        if (request.antallÅr !in 0..FULL_TRYGDETID_I_ÅR) {
            throw UgyldigInput("Trygdetiden må være mellom 0 og $FULL_TRYGDETID_I_ÅR år")
        }
        if (request.kilde == TrygdetidKilde.INGEN && request.antallÅr != FULL_TRYGDETID_I_ÅR) {
            throw UgyldigInput("Trygdetid uten kilde må være $FULL_TRYGDETID_I_ÅR år")
        }
    }
}
