package no.nav.grunn.og.hjelpestonad.infotrygd.tilgangskontroll

import no.nav.grunn.og.hjelpestonad.infotrygd.security.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TilgangService(
    private val tilgangsmaskinClient: TilgangsmaskinClient,
    private val auditlogg: Auditlogg,
) {
    private val logger = LoggerFactory.getLogger(TilgangService::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun filtrerPersonerMedTilgang(personidenter: Set<String>): Set<String> {
        validerAntall(personidenter.size)
        if (personidenter.isEmpty()) return emptySet()

        val vurderinger = vurderMotTilgangsmaskin(personidenter)
        val medTilgang = personidenter.filterTo(LinkedHashSet()) { harTilgang(vurderinger[it]) }

        val bortfiltrert = personidenter.size - medTilgang.size
        if (bortfiltrert > 0) {
            logger.info("Filtrerte bort {} av {} personer uten tilgang", bortfiltrert, personidenter.size)
            secureLogger.info(
                "Filtrerte bort {} av {} personer uten tilgang, saksbehandler={}",
                bortfiltrert,
                personidenter.size,
                SikkerhetContext.hentSaksbehandler(),
            )
        }
        return medTilgang
    }

    fun validerTilgangTilPersoner(personidenter: Set<String>) {
        require(personidenter.isNotEmpty()) { "Må oppgi minst én personident" }
        validerAntall(personidenter.size)

        val saksbehandler = SikkerhetContext.hentSaksbehandler()
        val resultat = runCatching {
                val vurderingerFraTilgangsmaskin = vurderMotTilgangsmaskin(personidenter)

                vurderingerFraTilgangsmaskin.validerIngenUtenSvar(personidenter)
                vurderingerFraTilgangsmaskin.validerIngenAvslag(personidenter)
            }
        val utfall = when (resultat.exceptionOrNull()) {
                null -> Tilgangsutfall.TILGANG
                is ManglerTilgangTilPersonFeil -> Tilgangsutfall.AVSLAG
                else -> Tilgangsutfall.FEIL
            }
        auditlogg.forespørselOmTilgang(saksbehandler, personidenter, utfall)

        // Kaster avslag og feil videre. Uten denne linjen blir avslag til tilgang.
        resultat.getOrThrow()
    }

    private fun Map<String, List<Tilgangsvurdering>>.validerIngenAvslag(
        personidenter: Set<String>,
    ) {
        val avslag = personidenter.flatMap { getValue(it) }.filterNot { it.harTilgang }
        if (avslag.isNotEmpty()) {
            logger.info("Mangler tilgang til én eller flere personer")
            secureLogger.info(
                "Mangler tilgang til {} av {} personer, avvisningskoder={}, traceIder={}, saksbehandler={}",
                avslag.map { it.personident }.distinct().size,
                personidenter.size,
                avslag.map { it.avvisningskode }.distinct(),
                avslag.mapNotNull { it.traceId },
                SikkerhetContext.hentSaksbehandler(),
            )
            val avvisningskode = avslag.map { it.avvisningskode }.distinct().singleOrNull()
            throw ManglerTilgangTilPersonFeil(avvisningskode?.feilmelding ?: standardmelding(personidenter.size))
        }
    }

    private fun Map<String, List<Tilgangsvurdering>>.validerIngenUtenSvar(
        personidenter: Set<String>,
    ) {
        val utenSvar = personidenter.count { this[it].isNullOrEmpty() }
        if (utenSvar > 0) {
            logger.warn("Tilgangsmaskinen svarte ikke for {} av {} personer", utenSvar, personidenter.size)
            secureLogger.warn(
                "Tilgangsmaskinen svarte ikke for {} av {} personer, saksbehandler={}",
                utenSvar,
                personidenter.size,
                SikkerhetContext.hentSaksbehandler(),
            )
            throw ManglerTilgangTilPersonFeil(standardmelding(personidenter.size))
        }
    }

    private fun vurderMotTilgangsmaskin(personidenter: Set<String>): Map<String, List<Tilgangsvurdering>> =
        tilgangsmaskinClient
            .sjekkTilgang(personidenter, Regeltype.KJERNE_REGELTYPE)
            .groupBy { it.personident }

    private fun harTilgang(vurderinger: List<Tilgangsvurdering>?): Boolean = !vurderinger.isNullOrEmpty() && vurderinger.all { it.harTilgang }

    private fun validerAntall(antall: Int) {
        require(antall <= MAKS_ANTALL_PERSONER) { "Kan ikke sjekke flere enn $MAKS_ANTALL_PERSONER personer i én forespørsel" }
    }

    private fun standardmelding(antallPersoner: Int): String = if (antallPersoner == 1) MANGLER_TILGANG_MELDING else MANGLER_TILGANG_FLERE_MELDING

    companion object {
        /** Grensen for ett bulk-kall mot tilgangsmaskinen. */
        const val MAKS_ANTALL_PERSONER = 1000

        const val MANGLER_TILGANG_MELDING = "Du har ikke tilgang til denne personen"
        const val MANGLER_TILGANG_FLERE_MELDING = "Du har ikke tilgang til én eller flere av personene"
    }
}
