package no.nav.gjenlevende.bs.sak.pdl

import no.nav.gjenlevende.bs.sak.infrastruktur.exception.ManglerTilgang
import no.nav.gjenlevende.bs.sak.texas.TexasClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class PdlClient(
    private val texasClient: TexasClient,
    @Value("\${PDL_URL}")
    private val pdlUrl: String,
    @Value("\${PDL_SCOPE}")
    private val pdlScope: String,
) {
    private val logger = LoggerFactory.getLogger(PdlClient::class.java)
    val pdlRestClient =
        RestClient
            .builder()
            .baseUrl(pdlUrl)
            .defaultHeader("Content-Type", "application/json")
            .build()

    fun hentPersonDataOBOToken(
        request: PdlRequest,
    ): HentPersonData? {
        logger.info("Utfører PDL-operasjon: hentPersonData")
        try {
            val pdlResponse =
                pdlRestClient
                    .post()
                    .uri("/graphql")
                    .headers { it.addAll(lagPdlOnBehalfOfHeaders()) }
                    .body(request)
                    .retrieve()
                    .body(PdlResponseHentPersonData::class.java)

            håndterPdlErrors(pdlResponse?.errors, "hentPersonData")

            return pdlResponse?.data
        } catch (e: Exception) {
            when (e) {
                is PdlException, is ManglerTilgang -> {
                    throw e
                }

                else -> {
                    logger.error("Feil ved kall til PDL", e)
                    throw PdlException("Teknisk feil ved henting av navn fra PDL", e)
                }
            }
        }
    }

    fun hentPersonDataMaskinToken(
        request: PdlRequest,
    ): HentPersonData? {
        logger.info("Utfører PDL-operasjon: hentPersonData")
        try {
            val pdlResponse =
                pdlRestClient
                    .post()
                    .uri("/graphql")
                    .headers { it.addAll(lagPdlMaskinTilMaskinToken()) }
                    .body(request)
                    .retrieve()
                    .body(PdlResponseHentPersonData::class.java)

            håndterPdlErrors(pdlResponse?.errors, "hentPersonData")

            return pdlResponse?.data
        } catch (e: Exception) {
            when (e) {
                is PdlException, is ManglerTilgang -> {
                    throw e
                }

                else -> {
                    logger.error("Feil ved kall til PDL", e)
                    throw PdlException("Teknisk feil ved henting av navn fra PDL", e)
                }
            }
        }
    }

    fun hentFamilieRelasjoner(request: PdlRequest): FamilieRelasjonerResponse? {
        logger.info("Utfører PDL-operasjon: hentFamilieRelasjoner")
        try {
            val pdlResponse =
                pdlRestClient
                    .post()
                    .uri("/graphql")
                    .headers { it.addAll(lagPdlOnBehalfOfHeaders()) }
                    .body(request)
                    .retrieve()
                    .body(PdlResponseFamilierelasjoner::class.java)

            håndterPdlErrors(pdlResponse?.errors, "hentFamilieRelasjoner")
            return pdlResponse?.data
        } catch (e: Exception) {
            when (e) {
                is PdlException, is ManglerTilgang -> {
                    throw e
                }

                else -> {
                    logger.error("Feil ved kall til PDL", e)
                    throw PdlException("Teknisk feil ved henting av navn fra PDL", e)
                }
            }
        }
    }

    private fun lagPdlOnBehalfOfHeaders(): HttpHeaders =
        HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("Tema", "EYO")
            set("behandlingsnummer", "B373")
            set("Authorization", "Bearer ${texasClient.hentOboToken(pdlScope)}")
        }

    private fun lagPdlMaskinTilMaskinToken(): HttpHeaders =
        HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("Tema", "EYO")
            set("behandlingsnummer", "B373")
            set("Authorization", "Bearer ${texasClient.hentMaskinToken(pdlScope)}")
        }

    private fun håndterPdlErrors(
        errors: List<PdlError>?,
        operasjon: String,
    ) {
        if (errors.isNullOrEmpty()) return

        logger.error("Feil fra PDL ved $operasjon: $errors")

        if (errors.any { it.extensions?.code == "unauthorized" }) {
            throw ManglerTilgang(melding = "Mangler tilgang til opplysningene for denne personen")
        }

        throw PdlException("Feil ved $operasjon: ${errors.first().message}")
    }
}

class PdlException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
