package no.nav.grunn.og.hjelpestonad.iverksett.brev

import efterlatte.prosessering.Status
import efterlatte.prosessering.Task
import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskProdusent
import efterlatte.prosessering.TaskType
import efterlatte.prosessering.Transaksjon
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.ArkiverDokumentResponse
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.AvsenderMottaker
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.AvsenderMottakerIdType
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.DokumentInfoResponse
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.JournalpostRequest
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.JournalpostType
import org.assertj.core.api.Assertions.assertThat
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Instant
import java.util.UUID
import kotlin.test.Test

class JournalførVedtaksbrevTaskTest {
    private val dokarkivClient = mockk<DokarkivClient>()
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private val journalføringService = mockk<JournalføringService>()
    private val journalpostForBehandlingService = mockk<JournalpostForBehandlingService>()
    private val journalførVedtaksbrevTask =
        JournalførVedtaksbrevTask(
            dokarkivClient = dokarkivClient,
            objectMapper = objectMapper,
            journalføringService = journalføringService,
            journalpostForBehandlingService = journalpostForBehandlingService,
        )

    @Test
    fun `doTask skal journalføre vedtaksbrev for én mottaker`() {
        val behandlingId = UUID.randomUUID()
        val taskData = JournalførVedtaksbrevTask.JournalførVedtaksbrevTaskData(behandlingId)
        val payload = objectMapper.writeValueAsString(taskData)
        val journalpostRequest = lagJournalpostRequest(behandlingId, 0)

        every { journalføringService.lagJournalføringRequester(behandlingId) } returns listOf(journalpostRequest)
        every { dokarkivClient.arkiverDokument(journalpostRequest, true) } returns lagArkiverDokumentResponse("123456")
        every { journalpostForBehandlingService.lagreJournalpostId(behandlingId, "123456") } returns
            JournalpostForBehandling(behandlingId = behandlingId, journalpostId = "123456")

        journalførVedtaksbrevTask.utfor(kontekst(payload, JournalførVedtaksbrevTask.TYPE))

        verify(exactly = 1) { journalføringService.lagJournalføringRequester(behandlingId) }
        verify(exactly = 1) { dokarkivClient.arkiverDokument(journalpostRequest, true) }
        verify(exactly = 1) { journalpostForBehandlingService.lagreJournalpostId(behandlingId, "123456") }
    }

    @Test
    fun `doTask skal journalføre vedtaksbrev for flere mottakere`() {
        val behandlingId = UUID.randomUUID()
        val taskData = JournalførVedtaksbrevTask.JournalførVedtaksbrevTaskData(behandlingId)
        val payload = objectMapper.writeValueAsString(taskData)
        val request1 = lagJournalpostRequest(behandlingId, 0)
        val request2 = lagJournalpostRequest(behandlingId, 1)

        every { journalføringService.lagJournalføringRequester(behandlingId) } returns listOf(request1, request2)
        every { dokarkivClient.arkiverDokument(request1, true) } returns lagArkiverDokumentResponse("123456")
        every { dokarkivClient.arkiverDokument(request2, true) } returns lagArkiverDokumentResponse("123457")
        every { journalpostForBehandlingService.lagreJournalpostId(behandlingId, "123456") } returns
            JournalpostForBehandling(behandlingId = behandlingId, journalpostId = "123456")
        every { journalpostForBehandlingService.lagreJournalpostId(behandlingId, "123457") } returns
            JournalpostForBehandling(behandlingId = behandlingId, journalpostId = "123457")

        journalførVedtaksbrevTask.utfor(kontekst(payload, JournalførVedtaksbrevTask.TYPE))

        verify(exactly = 1) { journalføringService.lagJournalføringRequester(behandlingId) }
        verify(exactly = 1) { dokarkivClient.arkiverDokument(request1, true) }
        verify(exactly = 1) { dokarkivClient.arkiverDokument(request2, true) }
        verify(exactly = 1) { journalpostForBehandlingService.lagreJournalpostId(behandlingId, "123456") }
        verify(exactly = 1) { journalpostForBehandlingService.lagreJournalpostId(behandlingId, "123457") }
    }

    @Test
    fun `opprettIverksettVedtaksbrevTask skal opprette riktig payload`() {
        val payload = """{"behandlingId": "123e4567-e89b-12d3-a456-426614174000"}"""
        val service = IverksettVedtaksbrevService(objectMapper)

        val faktisk = service.opprettIverksettVedtaksbrevTask(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))

        assertThat(objectMapper.readTree(faktisk)).isEqualTo(objectMapper.readTree(payload))
    }

    @Test
    fun `TYPE konstant skal være korrekt`() {
        assertThat(JournalførVedtaksbrevTask.TYPE.navn).isEqualTo("journalførVedtaksbrev")
    }

    private fun kontekst(
        payload: String,
        type: TaskType<String>,
    ) = TaskKontekst(
        task =
            Task(
                id = 1,
                type = type.navn,
                status = Status.KJØRER,
                payload = payload,
                triggerTid = Instant.now(),
                opprettetTid = Instant.now(),
                plukketTid = null,
                antallFeil = 0,
                stoppaarsak = null,
                versjon = 0,
            ),
        payload = payload,
        transaksjon = mockk<Transaksjon>(),
        produsent = mockk<TaskProdusent>(),
    )

    private fun lagJournalpostRequest(
        behandlingId: UUID,
        mottakerIndeks: Int,
    ) = JournalpostRequest(
        journalpostType = JournalpostType.UTGAAENDE,
        avsenderMottaker =
            AvsenderMottaker(
                id = "12345678901",
                idType = AvsenderMottakerIdType.FNR,
                navn = "Test Person",
            ),
        tema = "EYO",
        tittel = "Vedtak om innvilget stønad til barnetilsyn",
        eksternReferanseId = "$behandlingId-vedtaksbrev-mottaker$mottakerIndeks",
    )

    private fun lagArkiverDokumentResponse(journalpostId: String) =
        ArkiverDokumentResponse(
            dokumenter = listOf(DokumentInfoResponse(dokumentInfoId = "123")),
            journalpostId = journalpostId,
            journalpostferdigstilt = true,
        )
}
