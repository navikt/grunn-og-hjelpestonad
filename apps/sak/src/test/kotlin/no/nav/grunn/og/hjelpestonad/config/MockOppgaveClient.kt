package no.nav.grunn.og.hjelpestonad.config

import no.nav.grunn.og.hjelpestonad.oppgave.LagOppgaveRequest
import no.nav.grunn.og.hjelpestonad.oppgave.OppgaveClient
import no.nav.grunn.og.hjelpestonad.oppgave.OppgaveDto
import no.nav.grunn.og.hjelpestonad.oppgave.OppgavetypeGRU
import no.nav.grunn.og.hjelpestonad.oppgave.StatusEnum
import no.nav.grunn.og.hjelpestonad.oppgave.Tema
import no.nav.grunn.og.hjelpestonad.texas.TexasClient
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

// Tilstandsfull in-memory oppgave-tjeneste for profilen local-mock. Oppgavene lever i minnet,
// så fordeling, fjerning av saksbehandler og ferdigstilling virker lokalt uten kall mot dev-fss.
@Service
@Primary
@Profile("local-mock")
class MockOppgaveClient(
    texasClient: TexasClient,
    restClientBuilder: RestClient.Builder,
) : OppgaveClient(
        texasClient = texasClient,
        oppgaveUrl = "http://localhost:8090",
        oppgaveScope = URI("mock-scope"),
        restClientBuilder = restClientBuilder,
    ) {
    private val logger = LoggerFactory.getLogger(MockOppgaveClient::class.java)
    private val lagredeOppgaver = ConcurrentHashMap<Long, OppgaveDto>()

    // Starter på tidsstempel, så nye ID-er ikke kolliderer med rader fra tidligere kjøringer.
    private val oppgaveIdTeller = AtomicLong(System.currentTimeMillis())

    override fun opprettOppgaveM2M(oppgaveRequest: LagOppgaveRequest): OppgaveDto {
        val nyOppgaveId = oppgaveIdTeller.incrementAndGet()
        val opprettetOppgave =
            OppgaveDto(
                id = nyOppgaveId,
                versjon = 1,
                status = StatusEnum.OPPRETTET,
                tema = oppgaveRequest.tema,
                oppgavetype = oppgaveRequest.oppgavetype.name,
                behandlingstema = oppgaveRequest.behandlingstema,
                behandlingstype = oppgaveRequest.behandlingstype,
                personident = oppgaveRequest.personident,
                saksreferanse = oppgaveRequest.saksreferanse,
                prioritet = oppgaveRequest.prioritet,
                fristFerdigstillelse = oppgaveRequest.fristFerdigstillelse,
                aktivDato = oppgaveRequest.aktivDato,
                beskrivelse = oppgaveRequest.beskrivelse,
                tilordnetRessurs = oppgaveRequest.tilordnetRessurs,
                behandlesAvApplikasjon = oppgaveRequest.behandlesAvApplikasjon,
                tildeltEnhetsnr = oppgaveRequest.tildeltEnhetsnr,
            )
        lagredeOppgaver[nyOppgaveId] = opprettetOppgave
        logger.info(
            "MockOppgaveClient: Opprettet oppgave med id={} og oppgavetype={}",
            nyOppgaveId,
            oppgaveRequest.oppgavetype.name,
        )
        return opprettetOppgave
    }

    override fun hentOppgaveM2M(oppgaveId: Long): OppgaveDto {
        logger.info("MockOppgaveClient: Henter oppgave med id={}", oppgaveId)
        return hentEllerOpprettFallback(oppgaveId)
    }

    override fun fordelOppgave(
        oppgaveId: Long,
        saksbehandler: String,
        versjon: Int,
    ): Long {
        logger.info("MockOppgaveClient: Fordeler oppgave med id={}", oppgaveId)
        val eksisterendeOppgave = hentEllerOpprettFallback(oppgaveId)
        val oppdatertOppgave =
            eksisterendeOppgave.copy(
                tilordnetRessurs = saksbehandler,
                versjon = versjon + 1,
            )
        lagredeOppgaver[oppgaveId] = oppdatertOppgave
        return oppgaveId
    }

    override fun fjernTilordnetRessurs(
        oppgaveId: Long,
        versjon: Int,
    ) {
        logger.info("MockOppgaveClient: Fjerner tilordnetRessurs fra oppgave med id={}", oppgaveId)
        val eksisterendeOppgave = hentEllerOpprettFallback(oppgaveId)
        val oppdatertOppgave =
            eksisterendeOppgave.copy(
                tilordnetRessurs = null,
                versjon = versjon + 1,
            )
        lagredeOppgaver[oppgaveId] = oppdatertOppgave
    }

    override fun ferdigstillOppgave(
        oppgaveId: Long,
        versjon: Int,
    ) {
        logger.info("MockOppgaveClient: Ferdigstiller oppgave med id={}", oppgaveId)
        val eksisterendeOppgave = hentEllerOpprettFallback(oppgaveId)
        val oppdatertOppgave =
            eksisterendeOppgave.copy(
                status = StatusEnum.FERDIGSTILT,
                tilordnetRessurs = null,
                versjon = versjon + 1,
            )
        lagredeOppgaver[oppgaveId] = oppdatertOppgave
    }

    // Oppgaver fra Postgres kan finnes uten at mocken har sett dem etter omstart.
    // Da lagres en fallback-oppgave, så videre kall som fordeling og ferdigstilling virker.
    private fun hentEllerOpprettFallback(oppgaveId: Long): OppgaveDto =
        lagredeOppgaver.getOrPut(oppgaveId) {
            logger.info("MockOppgaveClient: Oppgave med id={} finnes ikke i minnet, lager fallback", oppgaveId)
            OppgaveDto(
                id = oppgaveId,
                versjon = 1,
                tema = Tema.GRU,
                oppgavetype = OppgavetypeGRU.BEH_SAK.name,
                tildeltEnhetsnr = "4415",
                tilordnetRessurs = null,
                status = StatusEnum.OPPRETTET,
            )
        }
}
