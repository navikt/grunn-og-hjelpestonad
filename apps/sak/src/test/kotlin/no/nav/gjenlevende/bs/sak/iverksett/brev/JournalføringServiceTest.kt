package no.nav.gjenlevende.bs.sak.iverksett.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.gjenlevende.bs.sak.behandling.Behandling
import no.nav.gjenlevende.bs.sak.behandling.BehandlingResultat
import no.nav.gjenlevende.bs.sak.behandling.BehandlingService
import no.nav.gjenlevende.bs.sak.behandling.BehandlingStatus
import no.nav.gjenlevende.bs.sak.brev.Brev
import no.nav.gjenlevende.bs.sak.brev.BrevService
import no.nav.gjenlevende.bs.sak.brev.BrevmottakerService
import no.nav.gjenlevende.bs.sak.brev.domain.BrevRequest
import no.nav.gjenlevende.bs.sak.brev.domain.BrevmalDto
import no.nav.gjenlevende.bs.sak.brev.domain.Brevmottaker
import no.nav.gjenlevende.bs.sak.brev.domain.BrevmottakerRolle
import no.nav.gjenlevende.bs.sak.brev.domain.InformasjonOmBrukerDto
import no.nav.gjenlevende.bs.sak.brev.domain.MottakerType
import no.nav.gjenlevende.bs.sak.fagsak.FagsakPersonService
import no.nav.gjenlevende.bs.sak.fagsak.FagsakRepository
import no.nav.gjenlevende.bs.sak.fagsak.domain.Fagsak
import no.nav.gjenlevende.bs.sak.fagsak.domain.StønadType
import no.nav.gjenlevende.bs.sak.iverksett.brev.domene.AvsenderMottakerIdType
import no.nav.gjenlevende.bs.sak.iverksett.brev.domene.Dokumenttype
import no.nav.gjenlevende.bs.sak.iverksett.brev.domene.JournalpostType
import no.nav.gjenlevende.bs.sak.pdl.Navn
import no.nav.gjenlevende.bs.sak.pdl.PdlService
import no.nav.gjenlevende.bs.sak.pdl.Person
import no.nav.gjenlevende.bs.sak.saf.BrukerIdType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.time.LocalDate
import java.util.Optional
import java.util.UUID
import kotlin.test.Test

class JournalføringServiceTest {
    private val behandlingService = mockk<BehandlingService>()
    private val fagsakRepository = mockk<FagsakRepository>()
    private val fagsakPersonService = mockk<FagsakPersonService>()
    private val brevService = mockk<BrevService>()
    private val brevmottakerService = mockk<BrevmottakerService>()
    private val pdlService = mockk<PdlService>()
    private val journalføringService =
        JournalføringService(
            behandlingService = behandlingService,
            fagsakRepository = fagsakRepository,
            fagsakPersonService = fagsakPersonService,
            brevService = brevService,
            brevmottakerService = brevmottakerService,
            pdlService = pdlService,
        )

    @Test
    fun `lagJournalføringRequester skal lage korrekt request for én mottaker`() {
        val behandlingId = UUID.randomUUID()
        val fagsakId = UUID.randomUUID()
        val fagsakPersonId = UUID.randomUUID()
        val personident = "12345678901"
        val behandling = lagBehandling(behandlingId, fagsakId, BehandlingResultat.INNVILGET)
        val fagsak = lagFagsak(fagsakId, fagsakPersonId, StønadType.BARNETILSYN)
        val brev = lagBrev(behandlingId)
        val mottaker = lagBrevmottaker(behandlingId, personident)

        every { behandlingService.hentBehandling(behandlingId) } returns behandling
        every { fagsakRepository.findById(fagsakId) } returns Optional.of(fagsak)
        every { fagsakPersonService.hentAktivIdent(fagsakPersonId) } returns personident
        every { brevService.hentBrev(behandlingId) } returns brev
        every { brevmottakerService.hentBrevmottakere(behandlingId) } returns listOf(mottaker)
        every { pdlService.hentPersonMedPersonIdent(personident) } returns Person(Navn("Ola", null, "Nordmann"), LocalDate.of(1990, 1, 15))
        val result = journalføringService.lagJournalføringRequester(behandlingId)

        assertThat(result).hasSize(1)
        assertThat(result[0].journalpostType).isEqualTo(JournalpostType.UTGAAENDE)
        assertThat(result[0].bruker?.id).isEqualTo(personident)
        assertThat(result[0].bruker?.idType).isEqualTo(BrukerIdType.FNR)
        assertThat(result[0].tittel).contains("barnetilsyn")
        assertThat(result[0].avsenderMottaker?.id).isEqualTo(personident)
        assertThat(result[0].avsenderMottaker?.idType).isEqualTo(AvsenderMottakerIdType.FNR)
        assertThat(result[0].avsenderMottaker?.navn).isEqualTo("Ola Nordmann")
        assertThat(result[0].eksternReferanseId).isEqualTo("$behandlingId-vedtaksbrev-mottaker-0")
    }

    @Test
    fun `lagJournalføringRequester skal lage request for flere mottakere`() {
        val behandlingId = UUID.randomUUID()
        val fagsakId = UUID.randomUUID()
        val fagsakPersonId = UUID.randomUUID()
        val personident = "12345678901"
        val vergeIdent = "98765432109"
        val behandling = lagBehandling(behandlingId, fagsakId, BehandlingResultat.INNVILGET)
        val fagsak = lagFagsak(fagsakId, fagsakPersonId, StønadType.SKOLEPENGER)
        val brev = lagBrev(behandlingId)
        val mottakere =
            listOf(
                lagBrevmottaker(behandlingId, personident, BrevmottakerRolle.BRUKER),
                lagBrevmottaker(behandlingId, vergeIdent, BrevmottakerRolle.VERGE),
            )

        every { behandlingService.hentBehandling(behandlingId) } returns behandling
        every { fagsakRepository.findById(fagsakId) } returns Optional.of(fagsak)
        every { fagsakPersonService.hentAktivIdent(fagsakPersonId) } returns personident
        every { brevService.hentBrev(behandlingId) } returns brev
        every { brevmottakerService.hentBrevmottakere(behandlingId) } returns mottakere
        every { pdlService.hentPersonMedPersonIdent(personident) } returns Person(Navn("Ola", null, "Nordmann"), LocalDate.of(1990, 1, 15))
        every { pdlService.hentPersonMedPersonIdent(vergeIdent) } returns Person(Navn("Kari", "Marie", "Hansen"), LocalDate.of(1990, 1, 15))
        val result = journalføringService.lagJournalføringRequester(behandlingId)

        assertThat(result).hasSize(2)
        assertThat(result[0].eksternReferanseId).isEqualTo("$behandlingId-vedtaksbrev-mottaker-0")
        assertThat(result[1].eksternReferanseId).isEqualTo("$behandlingId-vedtaksbrev-mottaker-1")
        assertThat(result[0].avsenderMottaker?.navn).isEqualTo("Ola Nordmann")
        assertThat(result[1].avsenderMottaker?.navn).isEqualTo("Kari Marie Hansen")
    }

    @Test
    fun `lagJournalføringRequester skal håndtere organisasjonsmottaker`() {
        val behandlingId = UUID.randomUUID()
        val fagsakId = UUID.randomUUID()
        val fagsakPersonId = UUID.randomUUID()
        val personident = "12345678901"
        val orgnr = "123456789"
        val behandling = lagBehandling(behandlingId, fagsakId, BehandlingResultat.AVSLÅTT)
        val fagsak = lagFagsak(fagsakId, fagsakPersonId, StønadType.BARNETILSYN)
        val brev = lagBrev(behandlingId)
        val mottaker =
            Brevmottaker(
                behandlingId = behandlingId,
                personRolle = BrevmottakerRolle.FULLMEKTIG,
                mottakerType = MottakerType.ORGANISASJON,
                orgnr = orgnr,
                navnHosOrganisasjon = "Advokatfirmaet AS",
            )

        every { behandlingService.hentBehandling(behandlingId) } returns behandling
        every { fagsakRepository.findById(fagsakId) } returns Optional.of(fagsak)
        every { fagsakPersonService.hentAktivIdent(fagsakPersonId) } returns personident
        every { brevService.hentBrev(behandlingId) } returns brev
        every { brevmottakerService.hentBrevmottakere(behandlingId) } returns listOf(mottaker)
        val result = journalføringService.lagJournalføringRequester(behandlingId)

        assertThat(result).hasSize(1)
        assertThat(result[0].avsenderMottaker?.id).isEqualTo(orgnr)
        assertThat(result[0].avsenderMottaker?.idType).isEqualTo(AvsenderMottakerIdType.ORGNR)
        assertThat(result[0].avsenderMottaker?.navn).isEqualTo("Advokatfirmaet AS")
    }

    @Test
    fun `lagJournalføringRequester skal feile når behandling ikke finnes`() {
        val behandlingId = UUID.randomUUID()

        every { behandlingService.hentBehandling(behandlingId) } returns null

        assertThatThrownBy { journalføringService.lagJournalføringRequester(behandlingId) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Fant ikke behandling")
    }

    @Test
    fun `lagJournalføringRequester skal feile når fagsak ikke finnes`() {
        val behandlingId = UUID.randomUUID()
        val fagsakId = UUID.randomUUID()
        val behandling = lagBehandling(behandlingId, fagsakId, BehandlingResultat.INNVILGET)

        every { behandlingService.hentBehandling(behandlingId) } returns behandling
        every { fagsakRepository.findById(fagsakId) } returns Optional.empty()

        assertThatThrownBy { journalføringService.lagJournalføringRequester(behandlingId) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Fant ikke fagsak")
    }

    @Test
    fun `lagJournalføringRequester skal feile når brev ikke finnes`() {
        val behandlingId = UUID.randomUUID()
        val fagsakId = UUID.randomUUID()
        val fagsakPersonId = UUID.randomUUID()
        val personident = "12345678901"
        val behandling = lagBehandling(behandlingId, fagsakId, BehandlingResultat.INNVILGET)
        val fagsak = lagFagsak(fagsakId, fagsakPersonId, StønadType.BARNETILSYN)

        every { behandlingService.hentBehandling(behandlingId) } returns behandling
        every { fagsakRepository.findById(fagsakId) } returns Optional.of(fagsak)
        every { fagsakPersonService.hentAktivIdent(fagsakPersonId) } returns personident
        every { brevService.hentBrev(behandlingId) } returns null

        assertThatThrownBy { journalføringService.lagJournalføringRequester(behandlingId) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Fant ikke brev")
    }

    @Test
    fun `lagJournalføringRequester skal feile når brev mangler PDF`() {
        val behandlingId = UUID.randomUUID()
        val fagsakId = UUID.randomUUID()
        val fagsakPersonId = UUID.randomUUID()
        val personident = "12345678901"
        val behandling = lagBehandling(behandlingId, fagsakId, BehandlingResultat.INNVILGET)
        val fagsak = lagFagsak(fagsakId, fagsakPersonId, StønadType.BARNETILSYN)
        val brevUtenPdf =
            Brev(
                behandlingId = behandlingId,
                brevJson = lagBrevRequest(),
                brevPdf = null,
            )

        every { behandlingService.hentBehandling(behandlingId) } returns behandling
        every { fagsakRepository.findById(fagsakId) } returns Optional.of(fagsak)
        every { fagsakPersonService.hentAktivIdent(fagsakPersonId) } returns personident
        every { brevService.hentBrev(behandlingId) } returns brevUtenPdf

        assertThatThrownBy { journalføringService.lagJournalføringRequester(behandlingId) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("mangler PDF")
    }

    @Test
    fun `lagJournalføringRequester skal feile når ingen brevmottakere finnes`() {
        val behandlingId = UUID.randomUUID()
        val fagsakId = UUID.randomUUID()
        val fagsakPersonId = UUID.randomUUID()
        val personident = "12345678901"
        val behandling = lagBehandling(behandlingId, fagsakId, BehandlingResultat.INNVILGET)
        val fagsak = lagFagsak(fagsakId, fagsakPersonId, StønadType.BARNETILSYN)
        val brev = lagBrev(behandlingId)

        every { behandlingService.hentBehandling(behandlingId) } returns behandling
        every { fagsakRepository.findById(fagsakId) } returns Optional.of(fagsak)
        every { fagsakPersonService.hentAktivIdent(fagsakPersonId) } returns personident
        every { brevService.hentBrev(behandlingId) } returns brev
        every { brevmottakerService.hentBrevmottakere(behandlingId) } returns emptyList()

        assertThatThrownBy { journalføringService.lagJournalføringRequester(behandlingId) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Ingen brevmottakere")
    }

    @Test
    fun `vedtaksbrevForStønadType skal returnere korrekt dokumenttype`() {
        assertThat(journalføringService.vedtaksbrevForStønadType(StønadType.BARNETILSYN))
            .isEqualTo(Dokumenttype.VEDTAKSBREV_BARNETILSYN)
        assertThat(journalføringService.vedtaksbrevForStønadType(StønadType.SKOLEPENGER))
            .isEqualTo(Dokumenttype.VEDTAKSBREV_SKOLEPENGER)
    }

    @Test
    fun `lagStønadtypeTekst skal returnere korrekt tekst`() {
        assertThat(journalføringService.lagStønadtypeTekst(StønadType.BARNETILSYN))
            .isEqualTo("stønad til barnetilsyn")
        assertThat(journalføringService.lagStønadtypeTekst(StønadType.SKOLEPENGER))
            .isEqualTo("stønad til skolepenger")
    }

    private fun lagBehandling(
        behandlingId: UUID,
        fagsakId: UUID,
        resultat: BehandlingResultat,
    ) = Behandling(
        id = behandlingId,
        fagsakId = fagsakId,
        status = BehandlingStatus.IVERKSETTER_VEDTAK,
        resultat = resultat,
    )

    private fun lagFagsak(
        fagsakId: UUID,
        fagsakPersonId: UUID,
        stønadType: StønadType,
    ) = Fagsak(
        id = fagsakId,
        fagsakPersonId = fagsakPersonId,
        eksternId = 12345L,
        stønadstype = stønadType,
    )

    private fun lagBrev(behandlingId: UUID) =
        Brev(
            behandlingId = behandlingId,
            brevJson = lagBrevRequest(),
            brevPdf = "PDF-innhold".toByteArray(),
            beslutterEnhetnavn = "IT avdelingen",
            beslutterEnhetnummer = "1234",
        )

    private fun lagBrevRequest() =
        BrevRequest(
            brevmal =
                BrevmalDto(
                    tittel = "Vedtaksbrev",
                    informasjonOmBruker =
                        InformasjonOmBrukerDto(
                            navn = "Test Person",
                            fnr = "12345678901",
                        ),
                    fastTekstAvslutning = emptyList(),
                ),
            fritekstbolker = emptyList(),
        )

    private fun lagBrevmottaker(
        behandlingId: UUID,
        personident: String,
        rolle: BrevmottakerRolle = BrevmottakerRolle.BRUKER,
    ) = Brevmottaker(
        behandlingId = behandlingId,
        personRolle = rolle,
        mottakerType = MottakerType.PERSON,
        personident = personident,
    )
}
