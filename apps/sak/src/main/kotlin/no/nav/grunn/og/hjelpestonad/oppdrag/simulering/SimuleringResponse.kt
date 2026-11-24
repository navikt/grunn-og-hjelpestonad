package no.nav.grunn.og.hjelpestonad.oppdrag.simulering

import java.time.LocalDateTime

/** Status som lagres i `simulering`. Har ikke `IKKE_SIMULERT`: mangler det en simulering, finnes det ingen rad. */
enum class Simuleringsstatus {
    /** Oppdrag svarte. Svaret lagres uendret og oppsummeres først ved GET. */
    SIMULERT,

    /** Behandlingen endrer ikke utbetalingen, så den ble ikke sendt til Oppdrag. */
    INGEN_ENDRING,

    /** Oppdrag svarte ikke. Lagres uten respons. Eldre rader kan ha respons, og da gir GET FEILET uten oppsummering. */
    FEILET,
}

/** Status i svaret til frontend. [IKKE_SIMULERT] finnes bare her, fordi den aldri lagres. */
enum class SimuleringsstatusResponse {
    /** Behandlingen har ingen lagret simulering. */
    IKKE_SIMULERT,

    /** Oppdrag svarte, og svaret kan oppsummeres. */
    SIMULERT,

    /** Behandlingen endrer ikke utbetalingen, så den ble ikke sendt til Oppdrag. */
    INGEN_ENDRING,

    /** Oppdrag svarte ikke, eller det lagrede svaret kan ikke oppsummeres. */
    FEILET,
}

/** Gir statusen i svaret til frontend. `when` er uttømmende: en ny [Simuleringsstatus] gir kompileringsfeil her. */
fun Simuleringsstatus.tilSimuleringsstatusResponse(): SimuleringsstatusResponse =
    when (this) {
        Simuleringsstatus.SIMULERT -> SimuleringsstatusResponse.SIMULERT
        Simuleringsstatus.INGEN_ENDRING -> SimuleringsstatusResponse.INGEN_ENDRING
        Simuleringsstatus.FEILET -> SimuleringsstatusResponse.FEILET
    }

/** Svaret til frontend. Uten fødselsnummer: [Simuleringsoppsummering] har ikke `mottakerNummer`. */
data class SimuleringResponse(
    val status: SimuleringsstatusResponse,
    val simulertTidspunkt: LocalDateTime?,
    val oppsummering: Simuleringsoppsummering?,
)
