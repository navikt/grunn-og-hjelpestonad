package no.nav.grunn.og.hjelpestonad.oppdrag.utbetalingsgenerator

import no.nav.grunn.og.hjelpestonad.oppdrag.utbetalingsgenerator.domain.AndelData
import no.nav.grunn.og.hjelpestonad.oppdrag.utbetalingsgenerator.domain.Behandlingsinformasjon
import no.nav.grunn.og.hjelpestonad.oppdrag.utbetalingsgenerator.domain.Opphør
import no.nav.grunn.og.hjelpestonad.oppdrag.utbetalingsgenerator.domain.Utbetalingsperiode
import java.math.BigDecimal
import java.time.YearMonth

internal data class UtbetalingsperiodeMal(
    val behandlingsinformasjon: Behandlingsinformasjon,
    val erEndringPåEksisterendePeriode: Boolean = false,
) {
    fun lagPeriodeFraAndel(
        andel: AndelData,
        opphørKjedeFom: YearMonth? = null,
    ): Utbetalingsperiode =
        Utbetalingsperiode(
            erEndringPåEksisterendePeriode = erEndringPåEksisterendePeriode,
            opphør =
                if (erEndringPåEksisterendePeriode) {
                    val opphørDatoFom =
                        opphørKjedeFom?.atDay(1)
                            ?: error("Mangler opphørsdato for kjede")
                    Opphør(opphørDatoFom)
                } else {
                    null
                },
            forrigePeriodeId = andel.forrigePeriodeId,
            periodeId = andel.periodeId ?: error("Mangler periodeId på andel=${andel.id}"),
            datoForVedtak = behandlingsinformasjon.vedtaksdato,
            klassifisering = andel.type.klassifisering,
            vedtakdatoFom = andel.fom.atDay(1),
            vedtakdatoTom = andel.tom?.atEndOfMonth(),
            sats = BigDecimal(andel.beløp),
            satsType = andel.type.satsType,
            utbetalesTil = behandlingsinformasjon.utbetalesTil ?: behandlingsinformasjon.personIdent,
            behandlingId = behandlingsinformasjon.eksternBehandlingId,
            utbetalingsgrad = andel.utbetalingsgrad,
        )
}
