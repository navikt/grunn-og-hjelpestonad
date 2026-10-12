package no.nav.grunn.og.hjelpestonad.infrastruktur.exception

import org.springframework.http.HttpStatus

class Feil(
    val melding: String,
    val httpStatus: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    throwable: Throwable? = null,
) : RuntimeException(melding, throwable)

/**
 * Felles type for avslag som gir 403. Meldingen vises til saksbehandleren.
 */
abstract class ManglerTilgang(
    val melding: String,
) : RuntimeException(melding)

class ManglerTilgangIPdlException(
    melding: String,
) : ManglerTilgang(melding)

class IkkeAnsvarligSaksbehandlerFeil(
    melding: String,
) : ManglerTilgang(melding)
