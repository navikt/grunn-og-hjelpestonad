package no.nav.grunn.og.hjelpestonad.felles.periode

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput

internal const val MAKS_LENGDE_BEGRUNNELSE = 2000

internal fun validerBegrunnelse(begrunnelse: String) {
    if (begrunnelse.length > MAKS_LENGDE_BEGRUNNELSE) {
        throw UgyldigInput("Begrunnelse kan ikke være lengre enn $MAKS_LENGDE_BEGRUNNELSE tegn.")
    }
}
