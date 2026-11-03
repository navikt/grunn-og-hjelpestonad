package no.nav.grunn.og.hjelpestonad.oppdrag

import no.nav.familie.felles.utbetalingsgenerator.Utbetalingsgenerator
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelData
import no.nav.familie.felles.utbetalingsgenerator.domain.Behandlingsinformasjon
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag
import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.AndelTilkjentYtelse
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

data class Oppdragsgrunnlag(
    val behandlingId: UUID,
    val eksternBehandlingId: Long,
    val eksternFagsakId: Long,
    val personIdent: String,
    val saksbehandlerId: String,
    val vedtaksdato: LocalDate,
    val andeler: List<AndelTilkjentYtelse>,
)

/**
 * Lager oppdrag for simulering eller iverksetting av en førstegangsbehandling.
 * Kjedingsstøtte mot tidligere andeler (tabellen `utbetalingsandel` i ADR-0011) er ikke bygget.
 */
fun lagUtbetalingsoppdrag(grunnlag: Oppdragsgrunnlag): Utbetalingsoppdrag {
    if (grunnlag.andeler.none { it.beløp != 0 }) {
        throw UgyldigInput("Oppdrag krever minst én andel med beløp ulik 0")
    }

    val nyeAndeler =
        grunnlag.andeler.map { andel ->
            AndelData(
                id = andel.id.toString(),
                fom = YearMonth.from(andel.fom),
                tom = YearMonth.from(andel.tom),
                beløp = andel.beløp,
                personIdent = grunnlag.personIdent,
                type = GrunnOgHjelpYtelsestype.GRUNNSTØNAD,
                periodeId = null,
                forrigePeriodeId = null,
                kildeBehandlingId = null,
            )
        }

    val behandlingsinformasjon =
        Behandlingsinformasjon(
            saksbehandlerId = grunnlag.saksbehandlerId,
            behandlingId = grunnlag.behandlingId.toString(),
            eksternBehandlingId = grunnlag.eksternBehandlingId,
            eksternFagsakId = grunnlag.eksternFagsakId,
            fagsystem = GrunnOgHjelpFagsystem.IT28,
            personIdent = grunnlag.personIdent,
            vedtaksdato = grunnlag.vedtaksdato,
            opphørAlleKjederFra = null,
        )

    return Utbetalingsgenerator()
        .lagUtbetalingsoppdrag(
            behandlingsinformasjon = behandlingsinformasjon,
            nyeAndeler = nyeAndeler,
            forrigeAndeler = emptyList(),
            sisteAndelPerKjede = emptyMap(),
        ).utbetalingsoppdrag
}
