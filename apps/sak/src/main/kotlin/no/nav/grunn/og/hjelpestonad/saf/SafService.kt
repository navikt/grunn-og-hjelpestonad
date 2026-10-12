package no.nav.grunn.og.hjelpestonad.saf

import no.nav.grunn.og.hjelpestonad.fagsak.FagsakPersonService
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.tilgangskontroll.TilgangService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class SafService(
    private val safClient: SafClient,
    private val fagsakPersonService: FagsakPersonService,
) {
    fun hentJournalposterForIdent(ident: String): List<Journalpost>? {
        val data: SafJournalpostBrukerData =
            safClient.hentSafJournalpostBrukerData(
                variables = JournalposterForBrukerRequest(Bruker(ident, BrukerIdType.FNR), emptyList(), emptyList(), 200),
            )

        return data.dokumentoversiktBruker.journalposter
    }

    fun finnDokumenterForPerson(fagsakPersonId: UUID): List<DokumentinfoResponse> {
        val ident = fagsakPersonService.hentAktivIdent(fagsakPersonId)
        val journalposter = hentJournalposterForIdent(ident)

        return journalposter
            ?.flatMap { journalpost -> journalpost.dokumenter?.map { tilDokumentInfoResponse(it, journalpost) } ?: emptyList() } ?: emptyList()
    }

    fun hentDokumentForPerson(
        fagsakPersonId: UUID,
        journalpostId: String,
        dokumentInfoId: String,
    ): ByteArray {
        validerAtDokumentTilhørerPerson(fagsakPersonId, journalpostId, dokumentInfoId)
        return safClient.hentDokument(journalpostId, dokumentInfoId)
    }

    private fun validerAtDokumentTilhørerPerson(
        fagsakPersonId: UUID,
        journalpostId: String,
        dokumentInfoId: String,
    ) {
        hentJournalposterForIdent(fagsakPersonService.hentAktivIdent(fagsakPersonId))
            ?.find { it.journalpostId == journalpostId }
            ?.dokumenter
            ?.find { it.dokumentInfoId == dokumentInfoId }
            ?: throw Feil(DOKUMENT_TILHØRER_IKKE_PERSON_FEIL, httpStatus = HttpStatus.FORBIDDEN)
    }

    private fun tilDokumentInfoResponse(
        dokumentInfo: DokumentInfo,
        journalpost: Journalpost,
    ): DokumentinfoResponse =
        DokumentinfoResponse(
            dokumentinfoId = dokumentInfo.dokumentInfoId,
            filnavn = dokumentInfo.dokumentvarianter?.find { it.variantformat == Dokumentvariantformat.ARKIV }?.filnavn,
            tittel = dokumentInfo.tittel ?: "Tittel mangler",
            journalpostId = journalpost.journalpostId,
            dato = mestRelevanteDato(journalpost),
            journalstatus = journalpost.journalstatus,
            journalposttype = journalpost.journalposttype,
            logiskeVedlegg = dokumentInfo.logiskeVedlegg?.map { LogiskVedleggDto(tittel = it.tittel) } ?: emptyList(),
            avsenderMottaker = journalpost.avsenderMottaker,
            tema = journalpost.tema,
            harSaksbehandlerTilgang = dokumentInfo.dokumentvarianter?.find { it.variantformat == Dokumentvariantformat.ARKIV }?.saksbehandlerHarTilgang ?: false,
        )

    fun mestRelevanteDato(journalpost: Journalpost): LocalDateTime? = journalpost.datoMottatt ?: journalpost.relevanteDatoer?.maxByOrNull { datoTyperSortert(it.datotype) }?.dato

    private fun datoTyperSortert(datoType: String) =
        when (datoType) {
            "DATO_JOURNALFOERT" -> 4
            "DATO_REGISTRERT" -> 3
            "DATO_DOKUMENT" -> 2
            "DATO_OPPRETTET" -> 1
            else -> 0
        }
}

const val DOKUMENT_TILHØRER_IKKE_PERSON_FEIL = "Dokumentet tilhører ikke personen"
