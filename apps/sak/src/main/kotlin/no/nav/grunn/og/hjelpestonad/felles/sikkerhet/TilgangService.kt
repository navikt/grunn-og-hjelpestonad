package no.nav.grunn.og.hjelpestonad.felles.sikkerhet

import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.ManglerTilgang
import no.nav.grunn.og.hjelpestonad.pdl.PdlService
import no.nav.grunn.og.hjelpestonad.tilgangskontroll.Avvisningskode
import no.nav.grunn.og.hjelpestonad.tilgangskontroll.TilgangsResultat
import no.nav.grunn.og.hjelpestonad.tilgangskontroll.TilgangsmaskinClient
import no.nav.grunn.og.hjelpestonad.unleash.FeatureToggle
import no.nav.grunn.og.hjelpestonad.unleash.UnleashService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TilgangService(
    private val tilgangsmaskinClient: TilgangsmaskinClient,
    private val pdlService: PdlService,
    private val unleashService: UnleashService,
) {
    private val logger = LoggerFactory.getLogger(TilgangService::class.java)

    //    TODO: Fjern før prodsetting
    fun erTilgangsmaskinToggelet(): Boolean {
        val featureToggles = unleashService.hentFeatureToggles()
        return featureToggles[FeatureToggle.TOGGLE_TILGANGSMASKIN_I_DEV.toggleName] ?: true
    }

    fun validerTilgangTilPersonMedRelasjoner(personident: String) {
        if (!erTilgangsmaskinToggelet()) {
            logger.info("Tilgangsmaskin er ikke toggelet, hopper over tilgangskontroll")
            return
        }

        val barnPersonidenter = pdlService.hentBarnPersonidenter(personident)
        val foreldreAvBarn = barnPersonidenter.flatMap { pdlService.hentForeldrePersonidenter(it) }

        val allePersonidenter = (listOf(personident) + barnPersonidenter + foreldreAvBarn).distinct()

        logger.info("Validerer tilgang til ${allePersonidenter.size} person(er) via tilgangsmaskin")

        val respons =
            tilgangsmaskinClient.sjekkTilgangBulk(
                personidenter = allePersonidenter,
            )

        val avvistePersoner = respons.resultater.filter { !it.harTilgang }

        if (avvistePersoner.isNotEmpty()) {
            val avvisningsdetaljer = avvistePersoner.map { it.tilAvvisningsdetaljer() }
            logger.warn("Tilgang avvist for saksbehandler ${respons.navIdent} årsak: $avvisningsdetaljer")

            val begrunnelse = avvisningsdetaljer.firstOrNull()?.begrunnelse ?: "Ukjent årsak"

            throw ManglerTilgang(
                melding = "Mangler tilgang til opplysningene. Årsak: $begrunnelse",
            )
        }
    }

    private fun TilgangsResultat.tilAvvisningsdetaljer(): Avvisningsdetaljer {
        val detaljer = detaljer as? Map<*, *>
        return Avvisningsdetaljer(
            personident = personident,
            avvisningskode =
                detaljer
                    ?.get("title")
                    ?.toString()
                    ?.let { runCatching { Avvisningskode.valueOf(it) }.getOrNull() },
            begrunnelse = detaljer?.get("begrunnelse")?.toString(),
        )
    }

    private data class Avvisningsdetaljer(
        val personident: String,
        val avvisningskode: Avvisningskode?,
        val begrunnelse: String?,
    )
}
