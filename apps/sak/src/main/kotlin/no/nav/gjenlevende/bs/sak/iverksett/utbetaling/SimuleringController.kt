package no.nav.gjenlevende.bs.sak.iverksett.utbetaling

import no.nav.gjenlevende.bs.sak.behandling.BehandlingService
import org.springframework.core.env.Environment
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/simulering"])
@PreAuthorize("hasRole('SAKSBEHANDLER')")
class SimuleringController(
    private val behandlingService: BehandlingService,
    private val simuleringService: SimuleringService,
    private val environment: Environment,
) {
    @PostMapping("/{behandlingId}")
    fun simulerForBehandling(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<String> {
        requireNotNull(behandlingService.hentBehandling(behandlingId))
        simuleringService.simuler(behandlingId)
        return ResponseEntity.ok("OK")
    }

    @GetMapping("/{behandlingId}/resultat")
    fun hentSimuleringsresultat(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<SimuleringResultatDto> {
        if (environment.matchesProfiles("dev")) {
            return ResponseEntity.ok(mockSimuleringsrespons().tilResultatDto())
        }

        val simulering =
            simuleringService.hentSimulering(behandlingId)
                ?: return ResponseEntity.notFound().build()

        return when (simulering.status) {
            SimuleringStatus.VENTER -> {
                ResponseEntity.noContent().build()
            }

            SimuleringStatus.FERDIG -> {
                simulering.respons?.let { ResponseEntity.ok(it.tilResultatDto()) }
                    ?: ResponseEntity.internalServerError().build()
            }

            SimuleringStatus.FEILET -> {
                ResponseEntity.internalServerError().build()
            }
        }
    }

    private fun mockSimuleringsrespons(): SimuleringResponse {
        val iDag = LocalDate.now()
        return SimuleringResponse(
            perioder =
                listOf(
                    SimuleringPeriode(
                        fom = iDag.minusMonths(1).withDayOfMonth(1),
                        tom = iDag.minusMonths(1).withDayOfMonth(iDag.minusMonths(1).lengthOfMonth()),
                        utbetalinger =
                            listOf(
                                SimuleringUtbetaling(
                                    fagsystem = "GJENLEVENDE_BS",
                                    sakId = "mock-sak-123",
                                    utbetalesTil = 12345678901L,
                                    stønadstype = "GJENLEVENDE_BARNETILSYN",
                                    tidligereUtbetalt = 0,
                                    nyttBeløp = 4000,
                                ),
                            ),
                    ),
                    SimuleringPeriode(
                        fom = iDag.withDayOfMonth(1),
                        tom = iDag.withDayOfMonth(iDag.lengthOfMonth()),
                        utbetalinger =
                            listOf(
                                SimuleringUtbetaling(
                                    fagsystem = "GJENLEVENDE_BS",
                                    sakId = "mock-sak-123",
                                    utbetalesTil = 12345678901L,
                                    stønadstype = "GJENLEVENDE_BARNETILSYN",
                                    tidligereUtbetalt = 0,
                                    nyttBeløp = 5000,
                                ),
                            ),
                    ),
                ),
        )
    }
}
