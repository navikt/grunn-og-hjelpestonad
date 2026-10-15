package no.nav.grunn.og.hjelpestonad.infotrygd.tilgangskontroll

/** Gir 403. Meldingen vises til saksbehandleren og inneholder aldri personidenter. */
class ManglerTilgangTilPersonFeil(
    val melding: String,
) : RuntimeException(melding)
