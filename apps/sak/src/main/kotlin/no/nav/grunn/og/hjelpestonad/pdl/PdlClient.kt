package no.nav.grunn.og.hjelpestonad.pdl

import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.ManglerTilgangIPdlException
import no.nav.grunn.og.hjelpestonad.texas.TexasClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class PdlClient(
    private val texasClient: TexasClient,
    @Value("\${integrasjoner.pdl.url}")
    pdlUrl: String,
    @Value("\${integrasjoner.pdl.scope}")
    private val pdlScope: String,
    restClientBuilder: RestClient.Builder,
) {
    private val logger = LoggerFactory.getLogger(PdlClient::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")
    private val pdlRestClient = restClientBuilder.clone().baseUrl(pdlUrl).build()

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
                is PdlException, is ManglerTilgangIPdlException -> {
                    throw e
                }

                else -> {
                    logger.error("Feil ved kall til PDL")
                    secureLogger.error("Feil ved kall til PDL", e)
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
                is PdlException, is ManglerTilgangIPdlException -> {
                    throw e
                }

                else -> {
                    logger.error("Feil ved kall til PDL")
                    secureLogger.error("Feil ved kall til PDL", e)
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
                is PdlException, is ManglerTilgangIPdlException -> {
                    throw e
                }

                else -> {
                    logger.error("Feil ved kall til PDL")
                    secureLogger.error("Feil ved kall til PDL", e)
                    throw PdlException("Teknisk feil ved henting av navn fra PDL", e)
                }
            }
        }
    }

    fun hentFaktagrunnlag(request: PdlRequest): PersonFaktagrunnlag? {
        logger.info("Utfører PDL-operasjon: hentFaktagrunnlag")
        try {
            val pdlResponse =
                pdlRestClient
                    .post()
                    .uri("/graphql")
                    .headers { it.addAll(lagPdlOnBehalfOfHeaders()) }
                    .body(request)
                    .retrieve()
                    .body(PdlResponseFaktagrunnlag::class.java)

            håndterPdlErrors(pdlResponse?.errors, "hentFaktagrunnlag")
            return pdlResponse?.data?.hentPerson
        } catch (e: Exception) {
            when (e) {
                is PdlException, is ManglerTilgangIPdlException -> {
                    throw e
                }

                else -> {
                    logger.error("Feil ved kall til PDL")
                    secureLogger.error("Feil ved kall til PDL", e)
                    throw PdlException("Teknisk feil ved henting av faktagrunnlag fra PDL", e)
                }
            }
        }
    }

    private fun lagPdlOnBehalfOfHeaders(): HttpHeaders =
        pdlHeaders().apply {
            setBearerAuth(texasClient.hentOboToken(pdlScope))

        }

    private fun lagPdlMaskinTilMaskinToken(): HttpHeaders =
        pdlHeaders().apply {
            setBearerAuth(texasClient.hentMaskinToken(pdlScope))
        }

    private fun pdlHeaders(): HttpHeaders =
        HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("Tema", "EYO")
            set("behandlingsnummer", "B373")
            set("Permissive-Mode", "true") // Dropper validering av behandlingskatalog. Fjernes når app skal ut i prod
        }

    private fun håndterPdlErrors(
        errors: List<PdlError>?,
        operasjon: String,
    ) {
        if (errors.isNullOrEmpty()) return

        val feil =
            if (errors.any { it.extensions?.code == "unauthorized" }) {
                ManglerTilgangIPdlException(melding = "Mangler tilgang til opplysningene for denne personen")
            } else {
                PdlException("Feil ved $operasjon: ${errors.first().message}")
            }
        logger.warn("PDL returnerte {} feil ved {}", errors.size, operasjon)
        secureLogger.warn("PDL returnerte feil ved {}: {}", operasjon, errors, feil)
        throw feil
    }
}

class PdlException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
