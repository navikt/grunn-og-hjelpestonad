package no.nav.gjenlevende.bs.sak.vedtak

import no.nav.gjenlevende.bs.sak.behandling.BehandlingResultat
import no.nav.gjenlevende.bs.sak.felles.sikkerhet.SikkerhetContext
import no.nav.gjenlevende.bs.sak.felles.sporbar.SporbarUtils
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.MappedCollection
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

data class Vedtak(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val resultatType: ResultatType,
    val begrunnelse: String? = null,
    @MappedCollection(idColumn = "vedtak_id", keyColumn = "id")
    val barnetilsynperioder: List<Barnetilsynperiode>,
    val saksbehandlerIdent: String,
    @Column("opphor_fom")
    val opphørFom: YearMonth? = null,
    val beslutterIdent: String? = null,
    val opprettetTid: LocalDateTime = SporbarUtils.now(),
    val opprettetAv: String = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
)

data class Barnetilsynperiode(
    @Id
    val id: UUID = UUID.randomUUID(),
    val datoFra: YearMonth,
    val datoTil: YearMonth,
    val utgifter: BigDecimal,
    val barn: List<UUID>,
    val periodetype: PeriodetypeBarnetilsyn,
    val aktivitetstype: AktivitetstypeBarnetilsyn,
)

data class BarnetilsynBeregningRequest(
    val barnetilsynBeregning: List<BarnetilsynBeregning>,
)

data class BarnetilsynBeregning(
    val datoFra: YearMonth,
    val datoTil: YearMonth,
    val utgifter: BigDecimal,
    val barn: List<UUID>,
    val periodetype: PeriodetypeBarnetilsyn,
)

data class BeløpsperioderDto(
    val datoFra: YearMonth,
    val datoTil: YearMonth,
    val utgifter: BigDecimal,
    val antallBarn: Int,
    val beløp: Int,
    val periodetype: PeriodetypeBarnetilsyn,
)

enum class ResultatType {
    INNVILGET,
    AVSLÅTT,
    HENLAGT,
    OPPHØR,
}

fun ResultatType.tilBehandlingResultat(): BehandlingResultat =
    when (this) {
        ResultatType.INNVILGET -> BehandlingResultat.INNVILGET
        ResultatType.AVSLÅTT -> BehandlingResultat.AVSLÅTT
        ResultatType.HENLAGT -> BehandlingResultat.HENLAGT
        ResultatType.OPPHØR -> BehandlingResultat.OPPHØRT
    }

enum class PeriodetypeBarnetilsyn {
    ORDINÆR,
    INGEN_STØNAD,
}

enum class AktivitetstypeBarnetilsyn {
    I_ARBEID,
    FORBIGÅENDE_SYKDOM,
    IKKE_RELEVANT,
}

data class VedtakDto(
    val resultatType: ResultatType,
    val begrunnelse: String? = null,
    val barnetilsynperioder: List<Barnetilsynperiode>,
    val saksbehandlerIdent: String? = null,
    val opphørFom: YearMonth? = null,
    val beslutterIdent: String? = null,
)

fun VedtakDto.tilVedtak(behandlingId: UUID): Vedtak =
    Vedtak(
        behandlingId = behandlingId,
        resultatType = this.resultatType,
        begrunnelse = this.begrunnelse,
        barnetilsynperioder = this.barnetilsynperioder.sortedBy { it.datoFra },
        saksbehandlerIdent = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
        opphørFom = this.opphørFom,
        beslutterIdent = this.beslutterIdent,
    )

fun Vedtak.tilDto(): VedtakDto =
    VedtakDto(
        resultatType = this.resultatType,
        begrunnelse = this.begrunnelse,
        barnetilsynperioder = this.barnetilsynperioder.sortedBy { it.datoFra },
        saksbehandlerIdent = this.saksbehandlerIdent,
        opphørFom = this.opphørFom,
        beslutterIdent = this.beslutterIdent,
    )

data class HistoriskVedtakResponse(
    val barnetilsynperioder: List<Barnetilsynperiode>,
    val fraErFørTidligsteVedtak: Boolean = false,
)
