package no.nav.grunn.og.hjelpestonad.felles.periode

import no.nav.grunn.og.hjelpestonad.felles.NORSK_DATOFORMAT

/** Teksten i endringshistorikken: endringsdetaljen og perioden. */
fun historikktekst(
    endringsdetalj: String,
    periode: Periodisert,
): String = "$endringsdetalj, Periode: ${formaterPeriode(periode)}"

internal fun formaterPeriode(periode: Periodisert): String {
    if (periode.fraOgMedDato == null && periode.tilOgMedDato == null) {
        return "Hele behandlingsperioden"
    }

    val fra = periode.fraOgMedDato?.format(NORSK_DATOFORMAT) ?: "Fra fødsel"
    val til = periode.tilOgMedDato?.format(NORSK_DATOFORMAT) ?: "løpende"
    return "$fra – $til"
}
