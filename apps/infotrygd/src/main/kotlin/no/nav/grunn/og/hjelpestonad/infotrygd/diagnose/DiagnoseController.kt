package no.nav.grunn.og.hjelpestonad.infotrygd.diagnose

import io.swagger.v3.oas.annotations.Operation
import no.nav.grunn.og.hjelpestonad.infotrygd.stønad.Stønadstype
import no.nav.grunn.og.hjelpestonad.infotrygd.tilgangskontroll.TilgangService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/infotrygd/diagnose")
class DiagnoseController(
    private val diagnoseRepository: DiagnoseRepository,
    private val tilgangService: TilgangService,
) {
    @Operation(summary = "Henter diagnosene registrert i siste måned per person og stønadstype fra Infotrygd")
    @PostMapping
    fun hentDiagnoser(
        @RequestBody request: DiagnoseRequest,
    ): DiagnoserResponse {
        request.valider()
        tilgangService.validerTilgangTilPersoner(request.personidenter)
        return DiagnoserResponse(
            diagnoseRepository.hentSisteDiagnoser(
                personidenter = request.personidenter,
                stønadstyper = request.stønadstyper.ifEmpty { Stønadstype.entries.toSet() },
            ),
        )
    }
}
