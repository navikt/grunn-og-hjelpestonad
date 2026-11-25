package no.nav.grunn.og.hjelpestonad.migrering

import no.nav.grunn.og.hjelpestonad.pdl.PdlService
import org.springframework.stereotype.Service

/**
 * Dobbeltsjekker mot PDL i minnet (uten lagring i databasen) at personen verken er død eller
 * har falsk identitet før migrering. Feil mot PDL propagerer (fail-closed).
 */
@Service
class MigreringPersonValideringService(
    private val pdlService: PdlService,
) {
    fun validerPersonForMigrering(personident: String): MigreringPersonValideringsresultat {
        val personIdentitetOgDoedsfall = pdlService.hentIdentitetOgDoedsfallForMigrering(personident)
        return MigreringPersonValidering.valider(personIdentitetOgDoedsfall)
    }
}
