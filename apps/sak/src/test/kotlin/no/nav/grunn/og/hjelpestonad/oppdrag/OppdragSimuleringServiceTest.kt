package no.nav.grunn.og.hjelpestonad.oppdrag

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingSteg
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingStegService
import no.nav.grunn.og.hjelpestonad.felles.sikkerhet.SikkerhetContext
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.IkkeAnsvarligSaksbehandlerFeil
import no.nav.grunn.og.hjelpestonad.oppdrag.simulering.LagretSimulering
import no.nav.grunn.og.hjelpestonad.oppdrag.simulering.SimuleringRepository
import no.nav.grunn.og.hjelpestonad.oppdrag.simulering.SimuleringResponse
import no.nav.grunn.og.hjelpestonad.oppdrag.simulering.Simuleringsstatus
import no.nav.grunn.og.hjelpestonad.oppdrag.simulering.SimuleringsstatusResponse
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.TilkjentYtelse
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.TilkjentYtelseService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

class OppdragSimuleringServiceTest {
    private val ansvarligSaksbehandlerService = mockk<AnsvarligSaksbehandlerService>()
    private val behandlingStegService = mockk<BehandlingStegService>()
    private val utbetalingsoppdragService = mockk<UtbetalingsoppdragService>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()
    private val oppdragClient = mockk<OppdragClient>()
    private val simuleringRepository = mockk<SimuleringRepository>()
    private val meterRegistry = SimpleMeterRegistry()
    private val clock = Clock.fixed(Instant.parse("2026-11-19T11:15:30.123456Z"), ZoneOffset.UTC)
    private val service =
        OppdragSimuleringService(
            behandlingStegService = behandlingStegService,
            ansvarligSaksbehandlerService = ansvarligSaksbehandlerService,
            utbetalingsoppdragService = utbetalingsoppdragService,
            tilkjentYtelseService = tilkjentYtelseService,
            oppdragClient = oppdragClient,
            simuleringRepository = simuleringRepository,
            objectMapper = jacksonObjectMapper(),
            meterRegistry = meterRegistry,
            clock = clock,
        )

    private val behandlingId = UUID.randomUUID()
    private val innloggetSaksbehandlerIdent = "Z999999"
    private val tidspunkt = LocalDateTime.parse("2026-11-19T11:15:30.123456")
    private val utbetalingsoppdrag = mockk<Utbetalingsoppdrag>()
    private val tilkjentYtelse = mockk<TilkjentYtelse>()

    // Syntetisk fødselsnummer. Svaret fra Oppdrag inneholder det, så det er bevisst med her.
    private val simuleringsrespons: JsonNode =
        jacksonObjectMapper().readTree(
            """
            {
              "simuleringMottaker": [
                {
                  "mottakerNummer": "01010199997",
                  "mottakerType": "BRUKER",
                  "simulertPostering": [
                    {
                      "fagOmrådeKode": "GRUNNSTØNAD_INFOTRYGD",
                      "fom": "2026-12-01",
                      "tom": "2026-12-31",
                      "forfallsdato": "2026-12-01",
                      "posteringType": "YTELSE",
                      "beløp": 752
                    }
                  ]
                }
              ]
            }
            """.trimIndent(),
        )

    // Samme postering som over, men uten beløp. Svaret kan ikke oppsummeres.
    private val responsUtenBeløp: JsonNode =
        jacksonObjectMapper().readTree(
            """
            {
              "simuleringMottaker": [
                {
                  "mottakerNummer": "01010199997",
                  "simulertPostering": [
                    {
                      "fagOmrådeKode": "GRUNNSTØNAD_INFOTRYGD",
                      "fom": "2026-12-01",
                      "tom": "2026-12-31",
                      "forfallsdato": "2026-12-01",
                      "posteringType": "YTELSE"
                    }
                  ]
                }
              ]
            }
            """.trimIndent(),
        )

    // Samme postering som over, men med en posteringType som ikke finnes i koden. Svaret kan ikke oppsummeres.
    private val responsMedUkjentPosteringType: JsonNode =
        jacksonObjectMapper().readTree(
            """
            {
              "simuleringMottaker": [
                {
                  "mottakerNummer": "01010199997",
                  "mottakerType": "BRUKER",
                  "simulertPostering": [
                    {
                      "fagOmrådeKode": "GRUNNSTØNAD_INFOTRYGD",
                      "fom": "2026-12-01",
                      "tom": "2026-12-31",
                      "forfallsdato": "2026-12-01",
                      "posteringType": "NY_TYPE",
                      "beløp": 752
                    }
                  ]
                }
              ]
            }
            """.trimIndent(),
        )

    @BeforeEach
    fun setup() {
        mockkObject(SikkerhetContext)
        every { SikkerhetContext.hentSaksbehandler() } returns innloggetSaksbehandlerIdent
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SikkerhetContext)
    }

    @Test
    fun `simulerPåNytt validerer ansvarlig saksbehandler først, og lager og simulerer ingenting når den feiler`() {
        every { ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId) } throws
            IkkeAnsvarligSaksbehandlerFeil("Innlogget saksbehandler er ikke ansvarlig saksbehandler")

        assertThatThrownBy { service.simulerPåNytt(behandlingId) }.isInstanceOf(IkkeAnsvarligSaksbehandlerFeil::class.java)

        verify(exactly = 0) { behandlingStegService.validerKanEndreSteg(any(), any()) }
        verify(exactly = 0) { utbetalingsoppdragService.lagUtbetalingsoppdrag(any(), any()) }
        verify(exactly = 0) { oppdragClient.simuler(any()) }
        verify(exactly = 0) { simuleringRepository.lagre(any(), any(), any(), any()) }
    }

    @Test
    fun `simulerPåNytt sjekker steget etter ansvarlig saksbehandler, og feil fra steget stopper alt`() {
        every { ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId) } just runs
        val feil = Feil(melding = "Fullfør «Sats og trygdetid» før du går videre.", httpStatus = HttpStatus.BAD_REQUEST)
        every { behandlingStegService.validerKanEndreSteg(behandlingId, BehandlingSteg.VEDTAK_OG_BREV) } throws feil

        assertThatThrownBy { service.simulerPåNytt(behandlingId) }.isSameAs(feil)

        verify(exactly = 0) { utbetalingsoppdragService.lagUtbetalingsoppdrag(any(), any()) }
        verify(exactly = 0) { oppdragClient.simuler(any()) }
        verify(exactly = 0) { simuleringRepository.lagre(any(), any(), any(), any()) }
    }

    @Test
    fun `svar fra Oppdrag lagres uendret med SIMULERT og klokka som tidspunkt, og DTO har oppsummering`() {
        stubGyldigAnsvarligOgSteg()
        every { utbetalingsoppdragService.lagUtbetalingsoppdrag(behandlingId, innloggetSaksbehandlerIdent) } returns
            UtbetalingsoppdragForBehandling(utbetalingsoppdrag = utbetalingsoppdrag)
        every { oppdragClient.simuler(utbetalingsoppdrag) } returns simuleringsrespons
        val lagretRespons = slot<JsonNode>()
        every { simuleringRepository.lagre(behandlingId, Simuleringsstatus.SIMULERT, capture(lagretRespons), tidspunkt) } just runs
        every { simuleringRepository.finn(behandlingId) } returns
            LagretSimulering(behandlingId = behandlingId, status = Simuleringsstatus.SIMULERT, respons = simuleringsrespons, opprettetTid = tidspunkt)

        val simuleringResponse = service.simulerPåNytt(behandlingId)

        assertThat(lagretRespons.captured).isEqualTo(simuleringsrespons)
        assertThat(simuleringResponse.status).isEqualTo(SimuleringsstatusResponse.SIMULERT)
        assertThat(simuleringResponse.simulertTidspunkt).isEqualTo(tidspunkt)
        assertThat(simuleringResponse.oppsummering?.nesteUtbetaling?.beløp).isEqualByComparingTo(BigDecimal("752"))
        assertThat(simuleringResponse.oppsummering?.ukjenteFagområdekoder).isEmpty()
        assertThat(tellerTags()).isEqualTo(mapOf("resultat" to "ok", "utloser" to "knapp"))
        verifyOrder {
            ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
            behandlingStegService.validerKanEndreSteg(behandlingId, BehandlingSteg.VEDTAK_OG_BREV)
            utbetalingsoppdragService.lagUtbetalingsoppdrag(behandlingId, innloggetSaksbehandlerIdent)
            oppdragClient.simuler(utbetalingsoppdrag)
            simuleringRepository.lagre(behandlingId, Simuleringsstatus.SIMULERT, simuleringsrespons, tidspunkt)
        }
    }

    @Test
    fun `null fra utbetalingsoppdrag lagres som INGEN_ENDRING uten respons, og Oppdrag kalles ikke`() {
        stubGyldigAnsvarligOgSteg()
        every { utbetalingsoppdragService.lagUtbetalingsoppdrag(behandlingId, innloggetSaksbehandlerIdent) } returns null
        every { simuleringRepository.lagre(behandlingId, Simuleringsstatus.INGEN_ENDRING, null, tidspunkt) } just runs
        every { simuleringRepository.finn(behandlingId) } returns
            LagretSimulering(behandlingId = behandlingId, status = Simuleringsstatus.INGEN_ENDRING, respons = null, opprettetTid = tidspunkt)

        val simuleringResponse = service.simulerPåNytt(behandlingId)

        assertThat(simuleringResponse).isEqualTo(SimuleringResponse(SimuleringsstatusResponse.INGEN_ENDRING, tidspunkt, null))
        verify(exactly = 0) { oppdragClient.simuler(any()) }
        assertThat(tellerTags()).isEqualTo(mapOf("resultat" to "ingen_endring", "utloser" to "knapp"))
    }

    @Test
    fun `unntak fra Oppdrag lagres som FEILET uten respons, kastes ikke, og telles som feilet`() {
        stubGyldigAnsvarligOgSteg()
        every { utbetalingsoppdragService.lagUtbetalingsoppdrag(behandlingId, innloggetSaksbehandlerIdent) } returns
            UtbetalingsoppdragForBehandling(utbetalingsoppdrag = utbetalingsoppdrag)
        every { oppdragClient.simuler(utbetalingsoppdrag) } throws Feil(melding = "Simulering mot oppdrag feilet", httpStatus = HttpStatus.BAD_GATEWAY)
        every { simuleringRepository.lagre(behandlingId, Simuleringsstatus.FEILET, null, tidspunkt) } just runs
        every { simuleringRepository.finn(behandlingId) } returns
            LagretSimulering(behandlingId = behandlingId, status = Simuleringsstatus.FEILET, respons = null, opprettetTid = tidspunkt)

        val simuleringResponse = service.simulerPåNytt(behandlingId)

        assertThat(simuleringResponse).isEqualTo(SimuleringResponse(SimuleringsstatusResponse.FEILET, tidspunkt, null))
        assertThat(tellerTags()).isEqualTo(mapOf("resultat" to "feilet", "utloser" to "knapp"))
    }

    @Test
    fun `svar som ikke kan oppsummeres, for eksempel uten beløp, lagres uendret som SIMULERT`() {
        stubGyldigAnsvarligOgSteg()
        every { utbetalingsoppdragService.lagUtbetalingsoppdrag(behandlingId, innloggetSaksbehandlerIdent) } returns
            UtbetalingsoppdragForBehandling(utbetalingsoppdrag = utbetalingsoppdrag)
        every { oppdragClient.simuler(utbetalingsoppdrag) } returns responsUtenBeløp
        every { simuleringRepository.lagre(behandlingId, Simuleringsstatus.SIMULERT, responsUtenBeløp, tidspunkt) } just runs
        every { simuleringRepository.finn(behandlingId) } returns
            LagretSimulering(behandlingId = behandlingId, status = Simuleringsstatus.SIMULERT, respons = responsUtenBeløp, opprettetTid = tidspunkt)

        val simuleringResponse = service.simulerPåNytt(behandlingId)

        // Innhenting og lagring gikk bra, så metrikken er ok. Oppsummeringen feiler først ved lesing, og da gir API FEILET.
        verify(exactly = 1) { simuleringRepository.lagre(behandlingId, Simuleringsstatus.SIMULERT, responsUtenBeløp, tidspunkt) }
        assertThat(simuleringResponse).isEqualTo(SimuleringResponse(SimuleringsstatusResponse.FEILET, tidspunkt, null))
        assertThat(tellerTags()).isEqualTo(mapOf("resultat" to "ok", "utloser" to "knapp"))
    }

    @Test
    fun `simulerEtterNyTilkjentYtelse gjør ingenting når behandlingen ikke har tilkjent ytelse`() {
        every { tilkjentYtelseService.hentTilkjentYtelse(behandlingId) } returns null

        service.simulerEtterNyTilkjentYtelse(behandlingId)

        verify(exactly = 0) { utbetalingsoppdragService.lagUtbetalingsoppdrag(any(), any()) }
        verify(exactly = 0) { oppdragClient.simuler(any()) }
        verify(exactly = 0) { simuleringRepository.lagre(any(), any(), any(), any()) }
        assertThat(meterRegistry.find("oppdrag_simulering").counters()).isEmpty()
    }

    @Test
    fun `simulerEtterNyTilkjentYtelse simulerer uten ny steg-sjekk og teller med utløser tilkjent_ytelse`() {
        every { tilkjentYtelseService.hentTilkjentYtelse(behandlingId) } returns tilkjentYtelse
        every { utbetalingsoppdragService.lagUtbetalingsoppdrag(behandlingId, innloggetSaksbehandlerIdent) } returns
            UtbetalingsoppdragForBehandling(utbetalingsoppdrag = utbetalingsoppdrag)
        every { oppdragClient.simuler(utbetalingsoppdrag) } returns simuleringsrespons
        every { simuleringRepository.lagre(behandlingId, Simuleringsstatus.SIMULERT, simuleringsrespons, tidspunkt) } just runs

        service.simulerEtterNyTilkjentYtelse(behandlingId)

        verify(exactly = 0) { behandlingStegService.validerKanEndreSteg(any(), any()) }
        verify(exactly = 0) { ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(any()) }
        assertThat(tellerTags()).isEqualTo(mapOf("resultat" to "ok", "utloser" to "tilkjent_ytelse"))
    }

    @Test
    fun `hentSimulering returnerer IKKE_SIMULERT uten rad, og kaller ikke Oppdrag`() {
        every { simuleringRepository.finn(behandlingId) } returns null

        val simuleringResponse = service.hentSimulering(behandlingId)

        assertThat(simuleringResponse).isEqualTo(SimuleringResponse(SimuleringsstatusResponse.IKKE_SIMULERT, null, null))
        verify(exactly = 0) { oppdragClient.simuler(any()) }
    }

    @Test
    fun `hentSimulering oppsummerer lagret SIMULERT-respons`() {
        every { simuleringRepository.finn(behandlingId) } returns
            LagretSimulering(behandlingId = behandlingId, status = Simuleringsstatus.SIMULERT, respons = simuleringsrespons, opprettetTid = tidspunkt)

        val simuleringResponse = service.hentSimulering(behandlingId)

        assertThat(simuleringResponse.status).isEqualTo(SimuleringsstatusResponse.SIMULERT)
        assertThat(simuleringResponse.simulertTidspunkt).isEqualTo(tidspunkt)
        assertThat(simuleringResponse.oppsummering?.nesteUtbetaling?.beløp).isEqualByComparingTo(BigDecimal("752"))
        verify(exactly = 0) { oppdragClient.simuler(any()) }
    }

    @Test
    fun `hentSimulering gir FEILET uten oppsummering`() {
        every { simuleringRepository.finn(behandlingId) } returns
            LagretSimulering(behandlingId = behandlingId, status = Simuleringsstatus.FEILET, respons = null, opprettetTid = tidspunkt)

        val simuleringResponse = service.hentSimulering(behandlingId)

        assertThat(simuleringResponse).isEqualTo(SimuleringResponse(SimuleringsstatusResponse.FEILET, tidspunkt, null))
    }

    @Test
    fun `hentSimulering gir FEILET når lagret SIMULERT-respons ikke kan oppsummeres`() {
        every { simuleringRepository.finn(behandlingId) } returns
            LagretSimulering(behandlingId = behandlingId, status = Simuleringsstatus.SIMULERT, respons = responsMedUkjentPosteringType, opprettetTid = tidspunkt)

        val simuleringResponse = service.hentSimulering(behandlingId)

        assertThat(simuleringResponse).isEqualTo(SimuleringResponse(SimuleringsstatusResponse.FEILET, tidspunkt, null))
        verify(exactly = 0) { simuleringRepository.lagre(any(), any(), any(), any()) }
        verify(exactly = 0) { oppdragClient.simuler(any()) }
    }

    @Test
    fun `hentSimulering gir FEILET når lagret SIMULERT-respons mangler beløp, og endrer ikke raden`() {
        every { simuleringRepository.finn(behandlingId) } returns
            LagretSimulering(behandlingId = behandlingId, status = Simuleringsstatus.SIMULERT, respons = responsUtenBeløp, opprettetTid = tidspunkt)

        val simuleringResponse = service.hentSimulering(behandlingId)

        assertThat(simuleringResponse).isEqualTo(SimuleringResponse(SimuleringsstatusResponse.FEILET, tidspunkt, null))
        verify(exactly = 0) { simuleringRepository.lagre(any(), any(), any(), any()) }
        verify(exactly = 0) { oppdragClient.simuler(any()) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["BARNETRYGD", "NY_YTELSE", "grunnstønad_infotrygd"])
    fun `lagret SIMULERT-respons med ukjent fagOmrådeKode gir SIMULERT med koden i ukjenteFagområdekoder`(fagområdekode: String) {
        every { simuleringRepository.finn(behandlingId) } returns
            LagretSimulering(
                behandlingId = behandlingId,
                status = Simuleringsstatus.SIMULERT,
                respons = responsMedFagområdekode(fagområdekode),
                opprettetTid = tidspunkt,
            )

        val simuleringResponse = service.hentSimulering(behandlingId)

        assertThat(simuleringResponse.status).isEqualTo(SimuleringsstatusResponse.SIMULERT)
        assertThat(simuleringResponse.oppsummering?.ukjenteFagområdekoder).containsExactly(fagområdekode)
        verify(exactly = 0) { simuleringRepository.lagre(any(), any(), any(), any()) }
        verify(exactly = 0) { oppdragClient.simuler(any()) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   "])
    fun `lagret SIMULERT-respons med tom fagOmrådeKode gir FEILET uten oppsummering`(fagområdekode: String) {
        every { simuleringRepository.finn(behandlingId) } returns
            LagretSimulering(
                behandlingId = behandlingId,
                status = Simuleringsstatus.SIMULERT,
                respons = responsMedFagområdekode(fagområdekode),
                opprettetTid = tidspunkt,
            )

        val simuleringResponse = service.hentSimulering(behandlingId)

        assertThat(simuleringResponse).isEqualTo(SimuleringResponse(SimuleringsstatusResponse.FEILET, tidspunkt, null))
        verify(exactly = 0) { simuleringRepository.lagre(any(), any(), any(), any()) }
        verify(exactly = 0) { oppdragClient.simuler(any()) }
    }

    @Test
    fun `lagret SIMULERT-respons der fagOmrådeKode mangler helt gir FEILET uten oppsummering`() {
        val responsUtenFagområdekode =
            responsMedFagområdekode("NY_YTELSE").also { respons ->
                val postering =
                    respons
                        .get("simuleringMottaker")
                        .get(0)
                        .get("simulertPostering")
                        .get(0) as ObjectNode
                postering.remove("fagOmrådeKode")
            }
        every { simuleringRepository.finn(behandlingId) } returns
            LagretSimulering(
                behandlingId = behandlingId,
                status = Simuleringsstatus.SIMULERT,
                respons = responsUtenFagområdekode,
                opprettetTid = tidspunkt,
            )

        val simuleringResponse = service.hentSimulering(behandlingId)

        assertThat(simuleringResponse).isEqualTo(SimuleringResponse(SimuleringsstatusResponse.FEILET, tidspunkt, null))
        verify(exactly = 0) { simuleringRepository.lagre(any(), any(), any(), any()) }
        verify(exactly = 0) { oppdragClient.simuler(any()) }
    }

    @Test
    fun `historisk FEILET-rad med respons gir FEILET uten oppsummering, og endrer ikke raden`() {
        every { simuleringRepository.finn(behandlingId) } returns
            LagretSimulering(behandlingId = behandlingId, status = Simuleringsstatus.FEILET, respons = responsUtenBeløp, opprettetTid = tidspunkt)

        val simuleringResponse = service.hentSimulering(behandlingId)

        assertThat(simuleringResponse).isEqualTo(SimuleringResponse(SimuleringsstatusResponse.FEILET, tidspunkt, null))
        verify(exactly = 0) { simuleringRepository.lagre(any(), any(), any(), any()) }
        verify(exactly = 0) { oppdragClient.simuler(any()) }
    }

    @Test
    fun `gjentatt lesing av samme rad gir samme svar, uten nytt kall til Oppdrag eller ny lagring`() {
        every { simuleringRepository.finn(behandlingId) } returns
            LagretSimulering(behandlingId = behandlingId, status = Simuleringsstatus.SIMULERT, respons = simuleringsrespons, opprettetTid = tidspunkt)

        val førsteLesing = service.hentSimulering(behandlingId)
        val andreLesing = service.hentSimulering(behandlingId)

        assertThat(andreLesing).isEqualTo(førsteLesing)
        assertThat(andreLesing.oppsummering?.nesteUtbetaling?.beløp).isEqualByComparingTo(BigDecimal("752"))
        verify(exactly = 2) { simuleringRepository.finn(behandlingId) }
        verify(exactly = 0) { simuleringRepository.lagre(any(), any(), any(), any()) }
        verify(exactly = 0) { oppdragClient.simuler(any()) }
    }

    /** Samme svar som [simuleringsrespons], men med [fagområdekode] på posteringen. */
    private fun responsMedFagområdekode(fagområdekode: String): JsonNode {
        val respons = simuleringsrespons.deepCopy() as ObjectNode
        val postering =
            respons
                .get("simuleringMottaker")
                .get(0)
                .get("simulertPostering")
                .get(0) as ObjectNode
        postering.put("fagOmrådeKode", fagområdekode)
        return respons
    }

    private fun stubGyldigAnsvarligOgSteg() {
        every { ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId) } just runs
        every { behandlingStegService.validerKanEndreSteg(behandlingId, BehandlingSteg.VEDTAK_OG_BREV) } just runs
    }

    /** Tagene på den ene telleren `oppdrag_simulering`. Feiler hvis det finnes flere. */
    private fun tellerTags(): Map<String, String> =
        meterRegistry
            .get("oppdrag_simulering")
            .counter()
            .id.tags
            .associate { it.key to it.value }
}
