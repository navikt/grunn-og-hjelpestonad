package no.nav.grunn.og.hjelpestonad.felles.periode

import java.time.format.DateTimeFormatter

/** Teksten i endringshistorikken: endringsdetaljen og perioden. */
fun historikktekst(
    endringsdetalj: String,
    periode: Periodisert,
): String = "$endringsdetalj, Periode: ${formaterPeriode(periode)}"

private fun formaterPeriode(periode: Periodisert): String {
    if (periode.fraOgMedDato == null && periode.tilOgMedDato == null) {
        return "Hele behandlingsperioden"
    }

    val fra = periode.fraOgMedDato?.format(DATOFORMAT) ?: "Fra fødsel"
    val til = periode.tilOgMedDato?.format(DATOFORMAT) ?: "løpende"
    return "$fra – $til"
}

private val DATOFORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
