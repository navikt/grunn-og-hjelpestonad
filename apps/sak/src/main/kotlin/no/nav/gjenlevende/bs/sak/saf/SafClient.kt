package no.nav.gjenlevende.bs.sak.saf

import no.nav.gjenlevende.bs.sak.config.SafConfig
import no.nav.gjenlevende.bs.sak.texas.TexasClient
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.UUID

@Service
class SafClient(
    val safConfig: SafConfig,
    private val texasClient: TexasClient,
) {
    private val logger = LoggerFactory.getLogger(SafClient::class.java)

    val safWebClient =
        WebClient
            .builder()
            .baseUrl(safConfig.safUri.toString())
            .defaultHeader("Content-Type", "application/json")
            .build()

    private val safRestWebClient =
        WebClient
            .builder()
            .baseUrl(safConfig.safBaseUri.toString())
            .build()

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
                safWebClient
                    .post()
                    .headers { it.addAll(lagSafHeaders()) }
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono<SafJournalpostResponse>()
                    .block() ?: throw SafException("Ingen respons fra SAF for hentSafJournalpostBrukerData")

            val safResponse = response

            håndterSafErrrors(safResponse.errors, "hentSafJournalpostBrukerData")

            safResponse.data ?: throw SafException("Fant ingen person i SAF for brukerId")
        } catch (e: Exception) {
            when (e) {
                is SafException -> {
                    throw e
                }

                else -> {
                    logger.error("Teknisk feil ved SAF-operasjon: hentSafJournalpostBrukerData", e)
                    throw SafException("Teknisk feil ved hentSafJournalpostBrukerData", e)
                }
            }
        }
    }

    fun hentDokument(
        journalpostId: String,
        dokumentInfoId: String,
    ): ByteArray {
        logger.info("Henter dokument fra SAF: journalpostId=$journalpostId, dokumentInfoId=$dokumentInfoId")

        return safRestWebClient
            .get()
            .uri("/rest/hentdokument/$journalpostId/$dokumentInfoId/ARKIV")
            .header("Authorization", "Bearer ${texasClient.hentOboToken(safConfig.safScope)}")
            .header(NAV_CALL_ID, UUID.randomUUID().toString())
            .accept(MediaType.APPLICATION_PDF)
            .retrieve()
            .bodyToMono<ByteArray>()
            .switchIfEmpty(Mono.error(NoSuchElementException("Tomt svar fra SAF for journalpostId=$journalpostId")))
            .timeout(Duration.ofSeconds(TIMEOUT_SEKUNDER))
            .doOnNext { logger.info("Hentet dokument fra SAF: journalpostId=$journalpostId") }
            .doOnError { logger.error("Feil ved henting av dokument fra SAF: journalpostId=$journalpostId: $it") }
            .block() ?: throw RuntimeException("Klarte ikke hente dokument med journalpostId=$journalpostId")
    }

    private fun håndterSafErrrors(
        errors: List<SafError>?,
        operasjon: String,
    ) {
        if (errors != null && errors.isNotEmpty()) {
            logger.error("Feil fra SAF ved $operasjon: $errors")
            val firstError = errors.firstOrNull()
            throw SafException(
                "Feil ved $operasjon: ${firstError?.message ?: "Ukjent feil"}",
            )
        }
    }

    private fun lagSafHeaders(): HttpHeaders =
        HttpHeaders().apply {
            setBearerAuth(texasClient.hentOboToken(safConfig.safScope))
            contentType = MediaType.APPLICATION_JSON
            accept = listOf(MediaType.APPLICATION_JSON)
            add(NAV_CALL_ID, UUID.randomUUID().toString())
        }

    companion object {
        private const val NAV_CALL_ID = "Nav-Callid"
        private const val TIMEOUT_SEKUNDER = 10L
    }
}
