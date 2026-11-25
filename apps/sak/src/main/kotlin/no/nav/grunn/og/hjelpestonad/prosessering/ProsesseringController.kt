package no.nav.grunn.og.hjelpestonad.prosessering

import efterlatte.prosessering.Status
import efterlatte.prosessering.Task
import efterlatte.prosessering.TaskLogg
import no.nav.grunn.og.hjelpestonad.felles.sikkerhet.SikkerhetContext
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/prosessering/task")
class ProsesseringController(
    private val prosesseringService: ProsesseringService,
) {
    @GetMapping("/status-oversikt")
    fun hentStatusoversikt(): ResponseEntity<List<ProsesseringStatusTelling>> = utenMellomlagring(prosesseringService.hentStatusoversikt())

    @GetMapping("/typer")
    fun hentTyper(): ResponseEntity<List<ProsesseringTypeTelling>> = utenMellomlagring(prosesseringService.hentTyper())

    @GetMapping
    fun hentTasks(
        @RequestParam(required = false) status: Status?,
        @RequestParam(required = false) type: String?,
        @RequestParam(defaultValue = "1") side: Int,
        @RequestParam(defaultValue = "25") antallPerSide: Int,
    ): ResponseEntity<ProsesseringTaskSide> {
        val navIdent = SikkerhetContext.hentSaksbehandler()
        return utenMellomlagring(prosesseringService.hentTasks(status, type, side, antallPerSide, navIdent))
    }

    @GetMapping("/{id}")
    fun hentTask(
        @PathVariable id: Long,
    ): ResponseEntity<Task> {
        val navIdent = SikkerhetContext.hentSaksbehandler()
        return utenMellomlagring(prosesseringService.hentTask(id, navIdent))
    }

    @GetMapping("/{id}/logg")
    fun hentLogg(
        @PathVariable id: Long,
    ): ResponseEntity<List<TaskLogg>> {
        val navIdent = SikkerhetContext.hentSaksbehandler()
        return utenMellomlagring(prosesseringService.hentLogg(id, navIdent))
    }

    @PostMapping("/{id}/rekjor")
    fun rekjor(
        @PathVariable id: Long,
        @RequestBody endring: ProsesseringEndring,
    ): ResponseEntity<Task> {
        val navIdent = SikkerhetContext.hentSaksbehandler()
        return utenMellomlagring(prosesseringService.endreTask(id, endring.versjon, ProsesseringHandling.REKJOR, navIdent))
    }

    @PostMapping("/{id}/avbryt")
    fun avbryt(
        @PathVariable id: Long,
        @RequestBody endring: ProsesseringEndring,
    ): ResponseEntity<Task> {
        val navIdent = SikkerhetContext.hentSaksbehandler()
        return utenMellomlagring(prosesseringService.endreTask(id, endring.versjon, ProsesseringHandling.AVBRYT, navIdent))
    }

    @PostMapping("/{id}/avvik")
    fun registrerAvvik(
        @PathVariable id: Long,
        @RequestBody avvik: ProsesseringAvvik,
    ): ResponseEntity<Task> {
        val navIdent = SikkerhetContext.hentSaksbehandler()
        return utenMellomlagring(prosesseringService.endreTask(id, avvik.versjon, ProsesseringHandling.AVVIK, navIdent, avvik.melding))
    }

    private fun <T : Any> utenMellomlagring(body: T): ResponseEntity<T> = ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body)
}
