package no.nav.grunn.og.hjelpestonad.infotrygd.tilgangskontroll

import no.nav.grunn.og.hjelpestonad.infotrygd.texas.TexasClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
open class TilgangsmaskinClient(
    @Value("\${integrasjoner.tilgangsmaskin.url}") tilgangsmaskinUrl: URI,
    @Value("\${integrasjoner.tilgangsmaskin.scope}") private val tilgangsmaskinScope: String,
    private val texasClient: TexasClient,
    restClientBuilder: RestClient.Builder,
) {
    private val restClient = restClientBuilder.clone().build()
    private val bulkUri: URI =
        UriComponentsBuilder
            .fromUri(tilgangsmaskinUrl)
            .pathSegment("api", "v1", "bulk", "obo")
            .build()
            .toUri()

    open fun sjekkTilgang(
        personidenter: Set<String>,
        regeltype: Regeltype,
    ): List<Tilgangsvurdering> {
        val identer = personidenter.filter { it.isNotBlank() }.toSet()
        if (identer.isEmpty()) return emptyList()

        val respons =
            try {
                restClient
                    .post()
                    .uri(bulkUri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers { it.setBearerAuth(texasClient.hentOboToken(target = tilgangsmaskinScope)) }
                    .body(identer.map { BrukerIdOgRegeltypeDto(brukerId = it, type = regeltype) })
                    .retrieve()
                    .body<BulkResponsDto>()
            } catch (e: Exception) {
                throw tilTilgangsmaskinException(e)
            } ?: throw TilgangsmaskinException("Tilgangsmaskinen svarte uten body")

        return tolkSvar(respons)
    }

    private fun tilTilgangsmaskinException(e: Exception): TilgangsmaskinException =
        when (e) {
            is RestClientResponseException -> {
                TilgangsmaskinException(
                    message = "Feil ved kall mot tilgangsmaskinen: HTTP ${e.statusCode.value()}",
                    httpStatus = e.statusCode.value(),
                    cause = e,
                )
            }

            else -> {
                TilgangsmaskinException("Feil ved kall mot tilgangsmaskinen: ${e.javaClass.simpleName}", cause = e)
            }
        }

    internal fun tolkSvar(
        respons: BulkResponsDto?,
    ): List<Tilgangsvurdering> {
        if (respons == null) throw IllegalStateException("Tilgangsmaskinen returnerte ikke et gyldig svar")

        return respons.resultater.map { resultatDto ->
            Tilgangsvurdering(
                personident = resultatDto.brukerId,
                harTilgang = resultatDto.status == 204,
                avvisningskode =
                    resultatDto.detaljer?.title?.let { title ->
                        Avvisningskode.entries.find { it.name == title }
                    },
                traceId = resultatDto.detaljer?.traceId,
            )
        }
    }
}

class TilgangsmaskinException(
    message: String,
    val httpStatus: Int? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
