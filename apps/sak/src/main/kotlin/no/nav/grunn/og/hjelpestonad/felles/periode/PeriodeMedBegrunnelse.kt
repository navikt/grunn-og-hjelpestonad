package no.nav.grunn.og.hjelpestonad.felles.periode

import no.nav.familie.tidslinje.Periode
import no.nav.grunn.og.hjelpestonad.felles.sporbar.Sporbar
import java.time.LocalDate
import java.util.UUID

interface Periodisert {
    val fraOgMedDato: LocalDate?
    val tilOgMedDato: LocalDate?
}

fun <T : Periodisert> T.tilPeriode(): Periode<T> =
    Periode(
        fom = this.fraOgMedDato,
        tom = this.tilOgMedDato,
        verdi = this,
    )

interface PeriodeMedBegrunnelse<SELV : PeriodeMedBegrunnelse<SELV>> : Periodisert {
    val id: UUID
    val behandlingId: UUID
    val begrunnelse: String
    val sporbar: Sporbar

    /**
     * Kopi med ny id, gitt behandling og ny sporbarhet. Brukes når en revurdering arver periodene
     * fra forrige behandling. Alle andre felter følger med uendret.
     */
    fun kopierTilBehandling(behandlingId: UUID): SELV

    /**
     * Perioder med samme nøkkel ligger på samme tidslinje og forkorter hverandre ved overlapp.
     * Standard er én tidslinje per type periode. Diagnosevilkåret har én per diagnosekode.
     */
    fun tidslinjenøkkel(): Any = Unit

    fun kopierMedTidsrom(
        id: UUID,
        fraOgMedDato: LocalDate?,
        tilOgMedDato: LocalDate?,
    ): SELV
}

interface PeriodeMedBegrunnelseRequest : Periodisert {
    val id: UUID?
    val begrunnelse: String
}
