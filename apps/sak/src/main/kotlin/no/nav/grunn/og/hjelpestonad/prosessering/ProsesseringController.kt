package no.nav.grunn.og.hjelpestonad.prosessering

import efterlatte.prosessering.Status
import efterlatte.prosessering.Task
import efterlatte.prosessering.TaskLogg
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
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
    private val prosesseringTilgang: ProsesseringTilgang,
) {
    @GetMapping("/status-oversikt")
    fun hentStatusoversikt(authentication: Authentication): ResponseEntity<List<ProsesseringStatusTelling>> {
        prosesseringTilgang.harRolleProsessering(authentication)
        return utenMellomlagring(prosesseringService.hentStatusoversikt())
    }

    @GetMapping("/typer")
    fun hentTyper(authentication: Authentication): ResponseEntity<List<ProsesseringTypeTelling>> {
        prosesseringTilgang.harRolleProsessering(authentication)
        return utenMellomlagring(prosesseringService.hentTyper())
    }

    @GetMapping
    fun hentTasks(
        authentication: Authentication,
        @RequestParam(required = false) status: Status?,
        @RequestParam(required = false) type: String?,
        @RequestParam(defaultValue = "1") side: Int,
        @RequestParam(defaultValue = "25") antallPerSide: Int,
    ): ResponseEntity<ProsesseringTaskSide> {
        val navIdent = prosesseringTilgang.harRolleProsessering(authentication)
        return utenMellomlagring(prosesseringService.hentTasks(status, type, side, antallPerSide, navIdent))
    }

    @GetMapping("/{id}")
    fun hentTask(
        authentication: Authentication,
        @PathVariable id: Long,
    ): ResponseEntity<Task> {
        val navIdent = prosesseringTilgang.harRolleProsessering(authentication)
        return utenMellomlagring(prosesseringService.hentTask(id, navIdent))
    }

    @GetMapping("/{id}/logg")
    fun hentLogg(
        authentication: Authentication,
        @PathVariable id: Long,
    ): ResponseEntity<List<TaskLogg>> {
        val navIdent = prosesseringTilgang.harRolleProsessering(authentication)
        return utenMellomlagring(prosesseringService.hentLogg(id, navIdent))
    }

    @PostMapping("/{id}/rekjor")
    fun rekjor(
        authentication: Authentication,
        @PathVariable id: Long,
        @RequestBody endring: ProsesseringEndring,
    ): ResponseEntity<Task> {
        val navIdent = prosesseringTilgang.harRolleProsessering(authentication)
        return utenMellomlagring(prosesseringService.endreTask(id, endring.versjon, ProsesseringHandling.REKJOR, navIdent))
    }

    @PostMapping("/{id}/avbryt")
    fun avbryt(
        authentication: Authentication,
        @PathVariable id: Long,
        @RequestBody endring: ProsesseringEndring,
    ): ResponseEntity<Task> {
        val navIdent = prosesseringTilgang.harRolleProsessering(authentication)
        return utenMellomlagring(prosesseringService.endreTask(id, endring.versjon, ProsesseringHandling.AVBRYT, navIdent))
    }

    @PostMapping("/{id}/avvik")
    fun registrerAvvik(
        authentication: Authentication,
        @PathVariable id: Long,
        @RequestBody avvik: ProsesseringAvvik,
    ): ResponseEntity<Task> {
        val navIdent = prosesseringTilgang.harRolleProsessering(authentication)
        return utenMellomlagring(prosesseringService.endreTask(id, avvik.versjon, ProsesseringHandling.AVVIK, navIdent, avvik.melding))
    }

    private fun <T : Any> utenMellomlagring(body: T): ResponseEntity<T> = ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body)
}
