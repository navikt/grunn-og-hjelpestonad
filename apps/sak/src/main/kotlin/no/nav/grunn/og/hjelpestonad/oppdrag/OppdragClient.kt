package no.nav.grunn.og.hjelpestonad.oppdrag

import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.infrastruktur.http.Ressurs
import no.nav.grunn.og.hjelpestonad.infrastruktur.http.VellykketSvar
import no.nav.grunn.og.hjelpestonad.infrastruktur.http.kallEksternTjenesteMedRessurs
import no.nav.grunn.og.hjelpestonad.texas.TexasClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.body

/**
 * Klient mot `familie-oppdrag-backend` (teamfamilie), som sender videre til Oppdragsystemet (OS).
 *
 * Simulering bruker saksbehandlerens OBO-token. Iverksetting og status bruker apptoken (client credentials), fordi de
 * kjøres fra tasker uten innlogget bruker. Tokenet sier derfor ikke hvem som behandlet saken. Det står i
 * `saksbehandlerId` i oppdraget, som `familie-oppdrag-backend` sender til OS både som saksbehandler og som attestant.
 */
@Service
class OppdragClient(
    private val texasClient: TexasClient,
    restClientBuilder: RestClient.Builder,
    @Value("\${integrasjoner.oppdrag.url}")
    url: String,
    @Value("\${integrasjoner.oppdrag.scope}")
    private val scope: String,
) {
    private val restClient = restClientBuilder.clone().baseUrl(url).build()

    fun simuler(utbetalingsoppdrag: Utbetalingsoppdrag): DetaljertSimuleringResultat =
        post<DetaljertSimuleringResultat, DetaljertSimuleringResultat>(
            path = SIMULERING_PATH,
            body = utbetalingsoppdrag,
            handling = "Simulering mot oppdrag",
            token = Token.OBO,
        ) { it.data }

    /**
     * Sender oppdraget til OS. Et vellykket svar betyr bare at `familie-oppdrag-backend` har lagt oppdraget på køen.
     * Kvitteringen fra OS kommer asynkront, og må leses med [hentStatus].
     *
     * 409 betyr at `familie-oppdrag-backend` har et oppdrag med samme ID fra før, ikke at innholdet er likt. Det gir
     * [IverksettUtfall.ALLEREDE_SENDT]. Vi sender bare oppdrag vi har lagret, så et nytt forsøk sender samme innhold.
     */
    fun iverksett(utbetalingsoppdrag: Utbetalingsoppdrag): IverksettUtfall =
        post<String, IverksettUtfall>(
            path = IVERKSETT_PATH,
            body = utbetalingsoppdrag,
            handling = "Iverksetting mot oppdrag",
            token = Token.APP,
            håndterHttpStatus = mapOf(HttpStatus.CONFLICT to { IverksettUtfall.ALLEREDE_SENDT }),
        ) { IverksettUtfall.SENDT }

    fun hentStatus(oppdragId: OppdragId): OppdragStatusSvar =
        post<String, OppdragStatusSvar>(
            path = STATUS_PATH,
            body = oppdragId,
            handling = "Henting av oppdragsstatus",
            token = Token.APP,
            håndterHttpStatus = mapOf(HttpStatus.NOT_FOUND to { e -> throw Feil("Fant ikke oppdrag for behandlingen", HttpStatus.NOT_FOUND, e) }),
        ) { svar ->
            OppdragStatusSvar(status = svar.data, melding = svar.melding?.takeIf { it.isNotBlank() })
        }

    private inline fun <reified T : Any, R> post(
        path: String,
        body: Any,
        handling: String,
        token: Token,
        håndterHttpStatus: Map<HttpStatus, (RestClientResponseException) -> R> = emptyMap(),
        noinline vedSuksess: (VellykketSvar<T>) -> R,
    ): R {
        val bearerToken =
            when (token) {
                Token.OBO -> texasClient.hentOboToken(targetAudience = scope)
                Token.APP -> texasClient.hentMaskinToken(targetAudience = scope)
            }
        val forespørsel =
            restClient
                .post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .headers { it.setBearerAuth(bearerToken) }
                .body(body)

        return kallEksternTjenesteMedRessurs(
            tjeneste = TJENESTENAVN,
            formål = handling,
            håndterHttpStatus = håndterHttpStatus,
            hentRessurs = { forespørsel.retrieve().body<Ressurs<T>>() },
            vedSuksess = vedSuksess,
        )
    }

    companion object {
        private const val SIMULERING_PATH = "/api/simulering/v1"
        private const val IVERKSETT_PATH = "/api/oppdrag"
        private const val STATUS_PATH = "/api/status"
        private const val TJENESTENAVN = "familie-oppdrag-backend"
    }
}

enum class IverksettUtfall {
    SENDT,
    ALLEREDE_SENDT,
}

private enum class Token {
    OBO,
    APP,
}
