package no.nav.gjenlevende.bs.sak.infotrygd

import no.nav.gjenlevende.bs.sak.infotrygd.dto.PersonPerioderResponse
import no.nav.gjenlevende.bs.sak.infotrygd.dto.PersonidentRequest
import no.nav.gjenlevende.bs.sak.infrastruktur.exception.Feil
import no.nav.gjenlevende.bs.sak.texas.TexasClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.time.Duration

@Configuration
class InfotrygdWebClientConfig {
    @Bean
    fun infotrygdWebClient(
        @Value("\${gjenlevende-bs-infotrygd.url}")
        infotrygdUrl: String,
    ): WebClient =
        WebClient
            .builder()
            .baseUrl(infotrygdUrl)
            .defaultHeader("Content-Type", "application/json")
            .build()
}

@Service
class InfotrygdClient(
    private val infotrygdWebClient: WebClient,
    private val texasClient: TexasClient,
    @Value("\${gjenlevende-bs-infotrygd.audience}")
    private val gjenlevendeBsInfotrygdAudience: String,
) {
    private val logger = LoggerFactory.getLogger(InfotrygdClient::class.java)

    companion object {
        private const val TIMEOUT_SEKUNDER = 10L
        private const val API_BASE_URL = "/api/infotrygd"
    }

    fun hentPerioderForPerson(
        personident: String,
    ): Mono<PersonPerioderResponse> {
        val oboToken =
            texasClient.hentOboToken(
                targetAudience = gjenlevendeBsInfotrygdAudience,
            )

        return infotrygdWebClient
            .post()
            .uri("$API_BASE_URL/perioder")
            .header("Authorization", "Bearer $oboToken")
            .bodyValue(PersonidentRequest(personident = personident))
            .retrieve()
            .bodyToMono<PersonPerioderResponse>()
            .onErrorResume(WebClientResponseException.NotFound::class.java) { _ ->
                logger.info("Person ikke funnet i Infotrygd")
                Mono.empty()
            }.timeout(Duration.ofSeconds(TIMEOUT_SEKUNDER))
            .doOnNext { response ->
                logger.info("Hentet perioder for person: ${response.barnetilsyn.size} barnetilsyn, ${response.skolepenger.size} skolepenger")
            }.doOnError { logger.error("Feilet å hente perioder for person: $it") }
    }

    fun hentPerioderForPersonSync(
        personident: String,
    ): PersonPerioderResponse =
        hentPerioderForPerson(
            personident = personident,
        ).block() ?: throw Feil("Person ikke funnet i Infotrygd", HttpStatus.NOT_FOUND)
}
