package no.nav.grunn.og.hjelpestonad.iverksett.brev

import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.brev.BrevService
import no.nav.grunn.og.hjelpestonad.brev.BrevmottakerService
import no.nav.grunn.og.hjelpestonad.brev.domain.Brevmottaker
import no.nav.grunn.og.hjelpestonad.brev.domain.MottakerType
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakPersonService
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakRepository
import no.nav.grunn.og.hjelpestonad.fagsak.domain.StønadType
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.ArkivDokument
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.AvsenderMottaker
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.AvsenderMottakerIdType
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.DokarkivBruker
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.Dokument
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.Dokumenttype
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.Dokumentvariant
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.Fagsystem
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.Filtype
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.JournalpostRequest
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.Sak
import no.nav.grunn.og.hjelpestonad.iverksett.brev.metadata.tilMetadata
import no.nav.grunn.og.hjelpestonad.pdl.Navn
import no.nav.grunn.og.hjelpestonad.pdl.PdlService
import no.nav.grunn.og.hjelpestonad.saf.BrukerIdType
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class JournalføringService(
    private val behandlingService: BehandlingService,
    private val fagsakRepository: FagsakRepository,
    private val fagsakPersonService: FagsakPersonService,
    private val brevService: BrevService,
    private val brevmottakerService: BrevmottakerService,
    private val pdlService: PdlService,
) {
    fun lagJournalføringRequester(behandlingId: UUID): List<JournalpostRequest> {
        val behandling =
            behandlingService.hentBehandling(behandlingId)
                ?: error("Fant ikke behandling med id=$behandlingId")
        val fagsak =
            fagsakRepository.findById(behandling.fagsakId).orElseThrow {
                error("Fant ikke fagsak med id=${behandling.fagsakId}")
            }
        val personident = fagsakPersonService.hentAktivIdent(fagsak.fagsakPersonId)
        val vedtaksbrev =
            brevService.hentBrev(behandlingId)
                ?: error("Fant ikke brev for behandlingId=$behandlingId")
        val brevPdf = vedtaksbrev.brevPdf ?: error("Vedtaksbrev mangler PDF for behandlingId=$behandlingId")
        val dokument =
            Dokument(
                dokument = brevPdf,
                filtype = Filtype.PDFA,
                dokumenttype = vedtaksbrevForStønadType(fagsak.stønadstype),
                tittel = "Vedtak om " + lagStønadtypeTekst(fagsak.stønadstype),
            )
        val metadata = dokument.dokumenttype.tilMetadata()
        val mottakere = brevmottakerService.hentBrevmottakere(behandlingId)
        val dokarkivBruker = DokarkivBruker(BrukerIdType.FNR, personident)
        val sak =
            Sak(fagsakId = fagsak.eksternId.toString(), sakstype = "FAGSAK", fagsaksystem = Fagsystem.EY)
        val dokumenter = listOf(mapTilArkivdokument(dokument))
        val journalpostRequester = mutableListOf<JournalpostRequest>()
        require(mottakere.isNotEmpty()) { "Ingen brevmottakere funnet for behandlingId=$behandlingId" }
        mottakere.forEachIndexed { indeks, mottaker ->
            journalpostRequester.add(
                JournalpostRequest(
                    journalpostType = metadata.journalpostType,
                    behandlingstema = metadata.behandlingstema?.value,
                    avsenderMottaker = mottaker.tilAvsenderMottaker(),
                    bruker = dokarkivBruker,
                    tema = metadata.tema,
                    tittel = dokument.tittel ?: metadata.tittel,
                    journalfoerendeEnhet = vedtaksbrev.beslutterEnhetnummer,
                    eksternReferanseId = "$behandlingId-vedtaksbrev-mottaker-$indeks",
                    sak = sak,
                    dokumenter = dokumenter,
                ),
            )
        }
        return journalpostRequester
    }

    fun vedtaksbrevForStønadType(stønadType: StønadType): Dokumenttype =
        when (stønadType) {
            StønadType.BARNETILSYN -> Dokumenttype.VEDTAKSBREV_BARNETILSYN
            StønadType.SKOLEPENGER -> Dokumenttype.VEDTAKSBREV_SKOLEPENGER
        }

    fun hentVariantformat(dokument: Dokument): String =
        if (dokument.filtype == Filtype.PDFA) {
            "ARKIV" // ustrukturert dokumentDto
        } else {
            "ORIGINAL" // strukturert dokumentDto
        }

    fun lagStønadtypeTekst(stønadstype: StønadType): String =
        when (stønadstype) {
            StønadType.BARNETILSYN -> "stønad til barnetilsyn"
            StønadType.SKOLEPENGER -> "stønad til skolepenger"
        }

    private fun Brevmottaker.tilAvsenderMottaker(): AvsenderMottaker =
        AvsenderMottaker(
            id =
                when (mottakerType) {
                    MottakerType.PERSON -> personident
                    MottakerType.ORGANISASJON -> orgnr
                },
            idType =
                when (mottakerType) {
                    MottakerType.PERSON -> AvsenderMottakerIdType.FNR
                    MottakerType.ORGANISASJON -> AvsenderMottakerIdType.ORGNR
                },
            navn =
                when (mottakerType) {
                    MottakerType.PERSON -> pdlService.hentPersonMedPersonIdent(personident)?.navn?.tilFulltNavn() ?: ""
                    MottakerType.ORGANISASJON -> navnHosOrganisasjon ?: ""
                },
        )

    fun Navn.tilFulltNavn(): String = listOfNotNull(fornavn, mellomnavn, etternavn).joinToString(" ")

    private fun mapTilArkivdokument(dokument: Dokument): ArkivDokument {
        val metadata = dokument.dokumenttype.tilMetadata()
        val variantFormat: String = hentVariantformat(dokument)
        return ArkivDokument(
            brevkode = metadata.brevkode,
            dokumentKategori = metadata.dokumentKategori,
            tittel = metadata.tittel ?: dokument.tittel,
            dokumentvarianter =
                listOf(
                    Dokumentvariant(
                        filtype = dokument.filtype.name,
                        variantformat = variantFormat,
                        fysiskDokument = dokument.dokument,
                        filnavn = dokument.filnavn,
                    ),
                ),
        )
    }
}
