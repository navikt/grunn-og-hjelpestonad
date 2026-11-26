package no.nav.grunn.og.hjelpestonad.vedtak

import no.nav.grunn.og.hjelpestonad.behandling.BehandlingResultat
import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import no.nav.grunn.og.hjelpestonad.felles.sikkerhet.SikkerhetContext
import no.nav.grunn.og.hjelpestonad.felles.sporbar.SporbarUtils
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

data class Vedtak(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val resultatType: ResultatType,
    val begrunnelse: String? = null,
    val saksbehandlerIdent: String,
    @Column("opphor_fom_maned")
    val opphørFom: YearMonth? = null,
    val beslutterIdent: String? = null,
    val opprettetTid: LocalDateTime = SporbarUtils.now(),
    val opprettetAv: String = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
)

enum class ResultatType {
    INNVILGET,
    AVSLÅTT,
    HENLAGT,
    OPPHØR,
}

fun utledVedtaksresultat(harBeregnedeAndeler: Boolean): ResultatType = if (harBeregnedeAndeler) ResultatType.INNVILGET else ResultatType.AVSLÅTT

/** Gjelder førstegangsbehandling. Revurdering og opphør krever sammenligning med forrige behandling. */
fun validerVedtaksresultatMotBeregning(
    resultatType: ResultatType,
    harBeregnedeAndeler: Boolean,
) {
    when (resultatType) {
        ResultatType.INNVILGET -> {
            if (!harBeregnedeAndeler) {
                throw UgyldigInput("Vedtaket er en innvilgelse, men beregningen har ingen andeler. Kontroller vedtaksresultatet og grunnlaget før du sender til beslutter.")
            }
        }

        ResultatType.AVSLÅTT -> {
            if (harBeregnedeAndeler) {
                throw UgyldigInput("Vedtaket er et avslag, men beregningen har andeler. Kontroller vedtaksresultatet og grunnlaget før du sender til beslutter.")
            }
        }

        ResultatType.HENLAGT, ResultatType.OPPHØR -> {
            Unit
        }
    }
}

/** Om vedtaket kan gi nye andeler eller opphøre andeler i OS, og derfor skal iverksettes mot oppdrag. */
fun ResultatType.endrerUtbetaling(): Boolean =
    when (this) {
        ResultatType.INNVILGET, ResultatType.OPPHØR -> true
        ResultatType.AVSLÅTT, ResultatType.HENLAGT -> false
    }

fun ResultatType.tilBehandlingResultat(): BehandlingResultat =
    when (this) {
        ResultatType.INNVILGET -> BehandlingResultat.INNVILGET
        ResultatType.AVSLÅTT -> BehandlingResultat.AVSLÅTT
        ResultatType.HENLAGT -> BehandlingResultat.HENLAGT
        ResultatType.OPPHØR -> BehandlingResultat.OPPHØRT
    }

data class VedtakResponse(
    val resultatType: ResultatType,
    val begrunnelse: String? = null,
    val saksbehandlerIdent: String? = null,
    val opphørFom: YearMonth? = null,
    val beslutterIdent: String? = null,
)

fun Vedtak.tilResponse(): VedtakResponse =
    VedtakResponse(
        resultatType = this.resultatType,
        begrunnelse = this.begrunnelse,
        saksbehandlerIdent = this.saksbehandlerIdent,
        opphørFom = this.opphørFom,
        beslutterIdent = this.beslutterIdent,
    )
