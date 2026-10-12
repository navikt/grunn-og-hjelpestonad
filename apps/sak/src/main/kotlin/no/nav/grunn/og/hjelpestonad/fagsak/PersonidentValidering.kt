package no.nav.grunn.og.hjelpestonad.fagsak

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput

const val LENGDE_PERSONIDENT = 11

fun validerErPersonident(personident: String) {
    if (personident.isBlank()) {
        throw UgyldigInput("Personident kan ikke være tom")
    }

    if (personident.length != LENGDE_PERSONIDENT) {
        throw UgyldigInput("Personident må være $LENGDE_PERSONIDENT siffer")
    }

    if (!personident.all { it.isDigit() }) {
        throw UgyldigInput("Personident kan kun inneholde tall")
    }
}
