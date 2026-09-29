package no.nav.grunn.og.hjelpestonad.infotrygd.periode

import io.swagger.v3.oas.annotations.Operation
import no.nav.grunn.og.hjelpestonad.infotrygd.stønad.Stønadstype
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/infotrygd/perioder")
class PeriodeController(
    private val periodeService: PeriodeService,
) {
    @Operation(summary = "Henter perioder (delytelser) for grunnstønad og hjelpestønad fra Infotrygd")
    @PostMapping
    fun hentPerioder(
        @RequestBody request: PeriodeRequest,
    ): PerioderResponse {
        request.valider()
        val perioder = periodeService.hentPerioder(request)
        return PerioderResponse(
            grunnstønad = perioder.getOrDefault(Stønadstype.GRUNNSTØNAD, emptyList()),
            hjelpestønad = perioder.getOrDefault(Stønadstype.HJELPESTØNAD, emptyList()),
        )
    }
}
