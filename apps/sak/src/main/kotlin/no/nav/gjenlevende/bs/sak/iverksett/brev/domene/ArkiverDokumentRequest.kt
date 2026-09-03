package no.nav.gjenlevende.bs.sak.iverksett.brev.domene

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.NotEmpty
import no.nav.gjenlevende.bs.sak.saf.BrukerIdType

@JsonInclude(JsonInclude.Include.NON_NULL)
data class JournalpostRequest(
    val journalpostType: JournalpostType? = null,
    val avsenderMottaker: AvsenderMottaker? = null,
    val bruker: DokarkivBruker? = null,
    val tema: String? = null,
    val behandlingstema: String? = null,
    val tittel: String? = null,
    val kanal: String? = null,
    val journalfoerendeEnhet: String? = null,
    val eksternReferanseId: String? = null,
    val sak: Sak? = null,
    val dokumenter: List<ArkivDokument> = ArrayList(),
)

class ArkivDokument(
    val tittel: String? = null,
    val brevkode: String? = null,
    val dokumentKategori: Dokumentkategori? = null,
    val dokumentvarianter: List<Dokumentvariant> = ArrayList(),
)

class Dokumentvariant(
    val filtype: String,
    val variantformat: String,
    val fysiskDokument: ByteArray,
    val filnavn: String?,
)

enum class Dokumentkategori(
    private val beskrivelse: String,
) {
    B("Brev"),
    VB("Vedtaksbrev"),
    IB("Infobrev"),
    ES("Elektronisk skjema"),
    TS("Tolkbart skjema"),
    IS("Ikke tolkbart skjema"),
    KS("Konverterte data fra system"),
    KD("Konvertert fra elektronisk arkiv"),
    SED("SED"),
    PUBL_BLANKETT_EOS("Pb EØS"),
    ELEKTRONISK_DIALOG("Elektronisk dialog"),
    REFERAT("Referat"),
    FORVALTNINGSNOTAT("Forvaltningsnotat"), // DENNE BLIR SYNLIG TIL SLUTTBRUKER!
    SOK("Søknad"),
    KA("Klage eller anke"),
}

enum class JournalpostType {
    INNGAAENDE,
    UTGAAENDE,
    NOTAT,
}

data class DokarkivBruker(
    val idType: BrukerIdType,
    val id: String,
)

data class Sak(
    val arkivsaksnummer: String? = null,
    val arkivsaksystem: String? = null,
    val fagsakId: String? = null,
    val fagsaksystem: Fagsystem? = null,
    val sakstype: String? = null,
)

enum class Fagsystem(
    val navn: String,
    val tema: String,
) {
    EY("Gjenlevende", "EYO"),
}

class Dokument(
    @field:NotEmpty val dokument: ByteArray,
    @field:NotEmpty val filtype: Filtype,
    val filnavn: String? = null,
    val tittel: String? = null,
    @field:NotEmpty val dokumenttype: Dokumenttype,
)

enum class Filtype {
    PDFA,
    JSON,
}

data class AvsenderMottaker(
    val id: String?,
    val idType: AvsenderMottakerIdType?,
    val navn: String,
)

enum class AvsenderMottakerIdType {
    FNR,
    HPRNR,
    NULL,
    ORGNR,
    UKJENT,
    UTL_ORG,
}
