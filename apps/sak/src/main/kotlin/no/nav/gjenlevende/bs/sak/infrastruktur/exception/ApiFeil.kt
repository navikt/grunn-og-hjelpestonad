package no.nav.gjenlevende.bs.sak.infrastruktur.exception

import org.springframework.http.HttpStatus

class Feil(
    val melding: String,
    val httpStatus: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    throwable: Throwable? = null,
) : RuntimeException(melding, throwable)

class ManglerTilgang(
    val melding: String,
) : RuntimeException(melding)
