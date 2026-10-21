package no.nav.grunn.og.hjelpestonad.tilgangskontroll

import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakPersonService
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakService
import no.nav.grunn.og.hjelpestonad.felles.sikkerhet.SikkerhetContext
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.ManglerTilgang
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TilgangService(
    private val tilgangsmaskinClient: TilgangsmaskinClient,
    private val fagsakPersonService: FagsakPersonService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
) {
    val logger = LoggerFactory.getLogger(TilgangService::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun validerTilgangTilPerson(personident: String) {
        if (SikkerhetContext.erMaskinTilMaskinToken()) {
            logger.error("Maskintoken på endepunkt med personsjekk")
            throw ManglerTilgangTilPersonFeil(avvisningskode = null)
        }

        val tilaganger = tilgangsmaskinClient.sjekkTilgang(setOf(personident), Regeltype.KJERNE_REGELTYPE)

        val tilgangPåPerson = tilaganger.find { it.personident == personident }
        if (tilgangPåPerson == null) {
            logger.warn("Tilgangsmaskinen svarte ikke for identen")
            secureLogger.warn("Tilgangsmaskinen svarte ikke for identen, saksbehandler=${SikkerhetContext.hentSaksbehandler()}")
            throw ManglerTilgangTilPersonFeil(avvisningskode = null)
        }

        if (!tilgangPåPerson.harTilgang) {
            logger.info("Mangler tilgang til person")
            secureLogger.info(
                "Mangler tilgang til person, avvisningskode=${tilgangPåPerson.avvisningskode}, " +
                    "traceId=${tilgangPåPerson.traceId}, saksbehandler=${SikkerhetContext.hentSaksbehandler()}",
            )
            throw ManglerTilgangTilPersonFeil(tilgangPåPerson.avvisningskode)
        }
    }

    fun validerTilgangTilFagsakPerson(fagsakPersonId: UUID) {
        val fagsakPerson = fagsakPersonService.hentAktivIdent(fagsakPersonId)
        validerTilgangTilPerson(fagsakPerson)
    }

    fun validerTilgangTilFagsak(fagsakId: UUID): Unit = validerTilgangTilPerson(fagsakService.hentAktivIdent(fagsakId))

    fun validerTilgangTilBehandling(behandlingId: UUID) {
        val behandling = behandlingService.hentBehandlingEllerKast(behandlingId)
        val personIdent = fagsakService.hentAktivIdent(behandling.fagsakId)
        validerTilgangTilPerson(personIdent)
    }

    companion object {
        const val MANGLER_TILGANG_MELDING = "Du har ikke tilgang til denne personen"
    }
}

class ManglerTilgangTilPersonFeil(
    val avvisningskode: Avvisningskode?,
) : ManglerTilgang(avvisningskode?.feilmelding ?: TilgangService.MANGLER_TILGANG_MELDING)
