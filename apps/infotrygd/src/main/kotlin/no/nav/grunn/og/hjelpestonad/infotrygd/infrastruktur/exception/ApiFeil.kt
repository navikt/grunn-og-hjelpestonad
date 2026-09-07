package no.nav.grunn.og.hjelpestonad.infotrygd.infrastruktur.exception

import org.springframework.http.HttpStatus

class ApiFeil(
    val feilmelding: String,
    val httpStatus: HttpStatus = HttpStatus.BAD_REQUEST,
) : RuntimeException(feilmelding)
