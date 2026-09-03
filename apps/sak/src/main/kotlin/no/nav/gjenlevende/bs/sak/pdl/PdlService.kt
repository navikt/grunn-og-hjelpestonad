package no.nav.gjenlevende.bs.sak.pdl

import no.nav.gjenlevende.bs.sak.fagsak.FagsakPersonService
import no.nav.gjenlevende.bs.sak.felles.sikkerhet.SikkerhetContext
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PdlService(
    private val pdlClient: PdlClient,
    private val fagsakPersonService: FagsakPersonService,
) {
    private val logger = LoggerFactory.getLogger(PdlService::class.java)

    fun hentPersonMedFagsakPersonId(fagsakPersonId: UUID): Person? {
        val ident = fagsakPersonService.hentAktivIdent(fagsakPersonId)
        return hentPersonFraPdl(ident)
    }

    fun hentPersonMedPersonIdent(personident: String?): Person? {
        if (personident == null) throw PdlException("Personident er null, kan ikke hente navn fra PDL")
        return hentPersonFraPdl(personident)
    }

    private fun hentPersonFraPdl(personident: String): Person? {
        val request =
            PdlRequest(
                query = graphqlQuery("/pdl/hent_navn_og_foedselsdato.graphql"),
                variables = mapOf("ident" to personident),
            )
        val data: HentPersonData =
            if (SikkerhetContext.erMaskinTilMaskinToken()) {
                pdlClient.hentPersonDataMaskinToken(
                    request = request,
                ) ?: throw PdlException("Fant ingen person i PDL for ident")
            } else {
                pdlClient.hentPersonDataOBOToken(
                    request = request,
                ) ?: throw PdlException("Fant ingen person i PDL for ident")
            }
        val hentPerson =
            data.hentPerson
                ?: throw PdlException("Fant ingen person i PDL")
        val navnListe = hentPerson.navn
        if (navnListe.isEmpty()) {
            logger.warn("Personen har ingen navn registrert i PDL")
            return null
        }
        val fødselsdato = hentPerson.foedselsdato
        if (fødselsdato.isEmpty()) {
            logger.warn("Personen har ingen fødselsdato registrert i PDL")
            return null
        }

        return Person(navnListe.first(), fødselsdato.first().foedselsdato)
    }

    fun hentBarnPersonidenter(personident: String): List<String> =
        hentFamilieRelasjoner(personident)
            ?.filter { it.relatertPersonsRolle == Familierolle.BARN }
            ?.mapNotNull { it.relatertPersonsIdent }
            ?: emptyList()

    fun hentForeldrePersonidenter(personident: String): List<String> =
        hentFamilieRelasjoner(personident)
            ?.filter { it.relatertPersonsRolle in listOf(Familierolle.FAR, Familierolle.MOR, Familierolle.MEDMOR) }
            ?.mapNotNull { it.relatertPersonsIdent }
            ?: emptyList()

    private fun hentFamilieRelasjoner(personident: String): List<ForelderBarnRelasjon>? {
        val request =
            PdlRequest(
                query = graphqlQuery("/pdl/hent_familie_relasjoner.graphql"),
                variables = mapOf("ident" to personident),
            )
        val data: FamilieRelasjonerResponse = pdlClient.hentFamilieRelasjoner(request = request) ?: return null

        return data.hentPerson?.forelderBarnRelasjon
    }

    fun graphqlQuery(path: String) =
        PdlService::class.java
            .getResource(path)
            ?.readText()
            ?.graphqlCompatible() ?: throw PdlException("Kunne ikke lese graphql query fra path: $path")

    private fun String.graphqlCompatible(): String = StringUtils.normalizeSpace(this.replace("\n", ""))
}
