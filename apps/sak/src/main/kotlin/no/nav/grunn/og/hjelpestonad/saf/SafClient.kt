package no.nav.grunn.og.hjelpestonad.saf

import no.nav.grunn.og.hjelpestonad.config.SafConfig
import no.nav.grunn.og.hjelpestonad.texas.TexasClient
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.util.UUID

@Service
class SafClient(
    val safConfig: SafConfig,
    private val texasClient: TexasClient,
    restClientBuilder: RestClient.Builder,
) {
    private val logger = LoggerFactory.getLogger(SafClient::class.java)
    private val safGraphqlClient = restClientBuilder.clone().baseUrl(safConfig.safUri.toString()).build()
    private val safDocumentClient = restClientBuilder.clone().baseUrl(safConfig.safBaseUri.toString()).build()

    fun hentSafJournalpostBrukerData(
        variables: JournalposterForBrukerRequest,
    ): SafJournalpostBrukerData {
        val request =
            SafJournalpostRequest(
                query = SafConfig.hentJournalposterBrukerQuery,
                variables = variables.tilSafRequestForBruker(),
            )

        logger.info("Utfører SAF-operasjon: hentSafJournalpostBrukerData")

        return try {
            val response =
                safGraphqlClient
                    .post()
                    .uri("")
                    .headers { it.addAll(lagSafHeaders(MediaType.APPLICATION_JSON)) }
                    .body(request)
                    .retrieve()
                    .body(SafJournalpostResponse::class.java)
                    ?: throw SafException("Ingen respons fra SAF for hentSafJournalpostBrukerData")

            håndterSafErrrors(response.errors, "hentSafJournalpostBrukerData")

            response.data ?: throw SafException("Fant ingen person i SAF for brukerId")
        } catch (e: SafException) {
            throw e
        } catch (e: Exception) {
            logger.error("Teknisk feil ved SAF-operasjon: hentSafJournalpostBrukerData", e)
            throw SafException("Teknisk feil ved hentSafJournalpostBrukerData", e)
        }
    }

    fun hentDokument(
        journalpostId: String,
        dokumentInfoId: String,
    ): ByteArray {
        logger.info("Henter dokument fra SAF")

        return try {
            safDocumentClient
                .get()
                .uri("/rest/hentdokument/{journalpostId}/{dokumentInfoId}/ARKIV", journalpostId, dokumentInfoId)
                .headers { it.addAll(lagSafHeaders(MediaType.APPLICATION_PDF)) }
                .retrieve()
                .body(ByteArray::class.java)
                ?.also { logger.info("Hentet dokument fra SAF") }
                ?: throw NoSuchElementException("Tomt svar fra SAF")
        } catch (e: Exception) {
            logger.error("Feil ved henting av dokument fra SAF", e)
            throw e
        }
    }

    private fun håndterSafErrrors(
        errors: List<SafError>?,
        operasjon: String,
    ) {
        if (!errors.isNullOrEmpty()) {
            logger.error("SAF returnerte {} feil ved {}", errors.size, operasjon)
            throw SafException(
                "Feil ved $operasjon: ${errors.first().message ?: "Ukjent feil"}",
            )
        }
    }

    private fun lagSafHeaders(accept: MediaType): HttpHeaders =
        HttpHeaders().apply {
            setBearerAuth(texasClient.hentOboToken(safConfig.safScope))
            contentType = MediaType.APPLICATION_JSON
            this.accept = listOf(accept)
            add(NAV_CALL_ID, UUID.randomUUID().toString())
        }

    companion object {
        private const val NAV_CALL_ID = "Nav-Callid"
    }
}
