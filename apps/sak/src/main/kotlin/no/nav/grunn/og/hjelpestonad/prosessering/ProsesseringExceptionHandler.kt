package no.nav.grunn.og.hjelpestonad.prosessering

import org.springframework.context.annotation.Profile
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice(assignableTypes = [ProsesseringController::class])
@Order(Ordered.HIGHEST_PRECEDENCE)
@Profile("dev", "local-mock")
class ProsesseringExceptionHandler {
    @ExceptionHandler(ProsesseringApiException::class)
    fun håndterProsesseringsfeil(prosesseringApiException: ProsesseringApiException): ResponseEntity<ProblemDetail> = problemDetail(prosesseringApiException.status, prosesseringApiException.detail)

    @ExceptionHandler(HttpMessageNotReadableException::class, MethodArgumentTypeMismatchException::class)
    fun håndterUgyldigForespoersel(): ResponseEntity<ProblemDetail> = problemDetail(HttpStatus.BAD_REQUEST, "Ugyldig forespørsel. Kontroller filter, paginering og påkrevde felt.")

    private fun problemDetail(
        status: HttpStatus,
        detail: String,
    ): ResponseEntity<ProblemDetail> =
        ResponseEntity
            .status(status)
            .cacheControl(CacheControl.noStore())
            .body(ProblemDetail.forStatusAndDetail(status, detail))
}
