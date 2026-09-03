package no.nav.gjenlevende.bs.sak.oppgave

import no.nav.gjenlevende.bs.sak.texas.TexasClient
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.net.URI
import java.time.Duration
import java.util.UUID

@Configuration
class OppgaveWebClientConfig {
    @Bean
    fun oppgaveWebClient(
        @Value("\${OPPGAVE_URL}")
        oppgaveUrl: String,
    ): WebClient =
        WebClient
            .builder()
            .baseUrl(oppgaveUrl)
            .defaultHeader("Content-Type", "application/json")
            .build()
}

@Component
class OppgaveClient(
    private val oppgaveWebClient: WebClient,
    private val texasClient: TexasClient,
    @Value("\${OPPGAVE_SCOPE}")
    private val oppgaveScope: URI,
) {
    private val logger = LoggerFactory.getLogger(OppgaveClient::class.java)

    companion object {
        private const val TIMEOUT_SEKUNDER = 10L
        private const val API_BASE_URL = "/api/v1/oppgaver"
    }

    fun opprettOppgaveM2M(oppgaveRequest: LagOppgaveRequest): OppgaveDto {
        logger.info("Sender opprettOppgave request til Oppgave-service ")
        val maskinToken = texasClient.hentMaskinToken(oppgaveScope.toString())

        return oppgaveWebClient
            .post()
            .uri(API_BASE_URL)
            .header("Authorization", "Bearer $maskinToken")
            .header("X-Correlation-ID", MDC.get("callId") ?: "${UUID.randomUUID()}")
            .bodyValue(oppgaveRequest)
            .retrieve()
            .bodyToMono<OppgaveDto>()
            .switchIfEmpty(Mono.error(NoSuchElementException("Tom respons fra oppgave")))
            .timeout(Duration.ofSeconds(TIMEOUT_SEKUNDER))
            .doOnNext { response ->
                logger.info("Oppgave opprettet med id: ${response.id} ")
            }.doOnError { error ->
                val responseBody = if (error is WebClientResponseException) error.responseBodyAsString else ""
                logger.error("Feil: klarte ikke opprette oppgave med $oppgaveRequest. Response: $responseBody", error)
            }.block() ?: throw RuntimeException("Klarte ikke opprette oppgave")
    }

    // TODO: Dette må kanskje gjøres med OBO, vi ser på dette siden.
    fun hentOppgaveM2M(oppgaveId: Long): OppgaveDto {
        logger.info("Henter oppgave med id=$oppgaveId fra Oppgave-service")
        val maskinToken = texasClient.hentMaskinToken(oppgaveScope.toString())

        return oppgaveWebClient
            .get()
            .uri("$API_BASE_URL/$oppgaveId")
            .header("Authorization", "Bearer $maskinToken")
            .header("X-Correlation-ID", MDC.get("callId") ?: "${UUID.randomUUID()}")
            .retrieve()
            .bodyToMono<OppgaveDto>()
            .switchIfEmpty(Mono.error(NoSuchElementException("Tom respons fra oppgave")))
            .timeout(Duration.ofSeconds(TIMEOUT_SEKUNDER))
            .doOnNext { response ->
                logger.info("Hentet oppgave med id: ${response.id}")
            }.doOnError {
                logger.error("Feil: klarte ikke hente oppgave med id=$oppgaveId")
            }.block() ?: throw RuntimeException("Klarte ikke hente oppgave med id=$oppgaveId")
    }

    fun fordelOppgave(
        oppgaveId: Long,
        saksbehandler: String,
        versjon: Int,
    ): Long {
        logger.info("Fordeler oppgave med id=$oppgaveId til saksbehandler=$saksbehandler")
        val oppdatertOppgave =
            oppdaterOppgave(
                oppgaveId = oppgaveId,
                body =
                    mapOf(
                        "id" to oppgaveId,
                        "tilordnetRessurs" to saksbehandler,
                        "versjon" to versjon,
                    ),
            )
        return oppdatertOppgave.id ?: throw RuntimeException("Oppdatert oppgave mangler id")
    }

    fun fjernTilordnetRessurs(
        oppgaveId: Long,
        versjon: Int,
    ) {
        logger.info("Fjerner tilordnetRessurs fra oppgave med id=$oppgaveId")
        oppdaterOppgave(
            oppgaveId = oppgaveId,
            body =
                mapOf(
                    "id" to oppgaveId,
                    "tilordnetRessurs" to "",
                    "versjon" to versjon,
                ),
        )
    }

    fun ferdigstillOppgave(
        oppgaveId: Long,
        versjon: Int,
    ) {
        logger.info("Ferdigstiller oppgave med id=$oppgaveId")
        oppdaterOppgave(
            oppgaveId = oppgaveId,
            body =
                mapOf(
                    "id" to oppgaveId,
                    "status" to "FERDIGSTILT",
                    "versjon" to versjon,
                    "tilordnetRessurs" to "",
                ),
        )
        logger.info("Oppgave ferdigstilt med id: $oppgaveId")
    }

    private fun oppdaterOppgave(
        oppgaveId: Long,
        body: Map<String, Any>,
    ): OppgaveDto {
        val maskinToken = texasClient.hentMaskinToken(oppgaveScope.toString())

        return oppgaveWebClient
            .patch()
            .uri("$API_BASE_URL/$oppgaveId")
            .header("Authorization", "Bearer $maskinToken")
            .header("X-Correlation-ID", MDC.get("callId") ?: "${UUID.randomUUID()}")
            .bodyValue(body)
            .retrieve()
            .bodyToMono<OppgaveDto>()
            .switchIfEmpty(Mono.error(NoSuchElementException("Tom respons fra oppgave")))
            .timeout(Duration.ofSeconds(TIMEOUT_SEKUNDER))
            .doOnNext { response ->
                logger.info("Oppgave oppdatert med id: ${response.id}")
            }.doOnError {
                logger.error("Feil: klarte ikke oppdatere oppgave med id=$oppgaveId")
            }.block() ?: throw RuntimeException("Klarte ikke oppdatere oppgave med id=$oppgaveId")
    }
}

data class LagOppgaveRequest(
    val personident: String,
    val saksreferanse: String,
    val prioritet: OppgavePrioritet = OppgavePrioritet.NORM,
    val tema: Tema,
    val behandlingstema: String? = null,
    val behandlingstype: String? = null,
    val fristFerdigstillelse: String,
    val aktivDato: String,
    val oppgavetype: OppgavetypeEYO,
    val beskrivelse: String,
    val tilordnetRessurs: String? = null,
    val behandlesAvApplikasjon: String,
    val tildeltEnhetsnr: String,
)
