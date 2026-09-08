package no.nav.grunn.og.hjelpestonad.oppgave

import no.nav.grunn.og.hjelpestonad.texas.TexasClient
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI
import java.util.UUID

@Component
class OppgaveClient(
    private val texasClient: TexasClient,
    @Value("\${OPPGAVE_URL}")
    oppgaveUrl: String,
    @Value("\${OPPGAVE_SCOPE}")
    private val oppgaveScope: URI,
    restClientBuilder: RestClient.Builder,
) {
    private val logger = LoggerFactory.getLogger(OppgaveClient::class.java)
    private val restClient = restClientBuilder.clone().baseUrl(oppgaveUrl).build()

    fun opprettOppgaveM2M(oppgaveRequest: LagOppgaveRequest): OppgaveDto {
        logger.info("Sender opprettOppgave request til Oppgave-service")
        val maskinToken = texasClient.hentMaskinToken(oppgaveScope.toString())

        return hentOppgaveRespons("opprette oppgave") {
            restClient
                .post()
                .uri(API_BASE_URL)
                .headers { headers ->
                    headers.setBearerAuth(maskinToken)
                    headers.set("X-Correlation-ID", correlationId())
                }.body(oppgaveRequest)
                .retrieve()
                .body<OppgaveDto>()
        }.also { response ->
            logger.info("Oppgave opprettet med id: {}", response.id)
        }
    }

    // TODO: Dette må kanskje gjøres med OBO, vi ser på dette siden.
    fun hentOppgaveM2M(oppgaveId: Long): OppgaveDto {
        logger.info("Henter oppgave med id={} fra Oppgave-service", oppgaveId)
        val maskinToken = texasClient.hentMaskinToken(oppgaveScope.toString())

        return hentOppgaveRespons("hente oppgave med id=$oppgaveId") {
            restClient
                .get()
                .uri("$API_BASE_URL/{oppgaveId}", oppgaveId)
                .headers { headers ->
                    headers.setBearerAuth(maskinToken)
                    headers.set("X-Correlation-ID", correlationId())
                }.retrieve()
                .body(OppgaveDto::class.java)
        }.also { response ->
            logger.info("Hentet oppgave med id: {}", response.id)
        }
    }

    fun fordelOppgave(
        oppgaveId: Long,
        saksbehandler: String,
        versjon: Int,
    ): Long {
        logger.info("Fordeler oppgave med id={} til saksbehandler={}", oppgaveId, saksbehandler)
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
        logger.info("Fjerner tilordnetRessurs fra oppgave med id={}", oppgaveId)
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
        logger.info("Ferdigstiller oppgave med id={}", oppgaveId)
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
        logger.info("Oppgave ferdigstilt med id: {}", oppgaveId)
    }

    private fun oppdaterOppgave(
        oppgaveId: Long,
        body: Map<String, Any>,
    ): OppgaveDto {
        val maskinToken = texasClient.hentMaskinToken(oppgaveScope.toString())

        return hentOppgaveRespons("oppdatere oppgave med id=$oppgaveId") {
            restClient
                .patch()
                .uri("$API_BASE_URL/{oppgaveId}", oppgaveId)
                .headers { headers ->
                    headers.setBearerAuth(maskinToken)
                    headers.set("X-Correlation-ID", correlationId())
                }.body(body)
                .retrieve()
                .body<OppgaveDto>()
        }.also { response ->
            logger.info("Oppgave oppdatert med id: {}", response.id)
        }
    }

    private fun hentOppgaveRespons(
        operasjon: String,
        kall: () -> OppgaveDto?,
    ): OppgaveDto =
        try {
            kall() ?: throw NoSuchElementException("Tom respons fra oppgave")
        } catch (e: Exception) {
            logger.error("Feil: klarte ikke {}", operasjon, e)
            throw e
        }

    private fun correlationId(): String = MDC.get("callId") ?: UUID.randomUUID().toString()

    companion object {
        private const val API_BASE_URL = "/api/v1/oppgaver"
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
