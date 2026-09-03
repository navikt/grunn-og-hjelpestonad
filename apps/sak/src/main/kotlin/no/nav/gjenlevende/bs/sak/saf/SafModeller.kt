package no.nav.gjenlevende.bs.sak.saf

import java.time.LocalDateTime
import java.util.UUID

data class HentDokumenterRequest(
    val fagsakPersonId: UUID,
)

class SafException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class SafJournalpostBrukerData(
    val dokumentoversiktBruker: Journalpostliste,
)

data class Journalpostliste(
    val journalposter: List<Journalpost>?,
)

data class SafJournalpostResponse(
    val data: SafJournalpostBrukerData? = null,
    val errors: List<SafError>? = null,
)

data class SafError(
    val message: String? = null,
    val extensions: SafExtension? = null,
)

data class SafExtension(
    val code: SafErrorCode,
    val classification: String,
)

enum class SafErrorCode {
    FORBIDDEN,
    NOT_FOUND,
    BAD_REQUEST,
    SERVER_ERROR,
}

data class JournalposterForBrukerRequest(
    val brukerId: Bruker,
    val tema: List<Arkivtema>?,
    val journalposttype: List<Journalposttype>?,
    val antall: Int,
)

data class Bruker(
    val id: String,
    val type: BrukerIdType,
)

enum class BrukerIdType {
    AKTOERID,
    FNR,
    ORGNR,
}

fun JournalposterForBrukerRequest.tilSafRequestForBruker(): SafRequestForBruker =
    SafRequestForBruker(
        brukerId = brukerId,
        tema = tema,
        journalposttype = journalposttype,
        antall = antall,
    )

data class SafRequestForBruker(
    val brukerId: Bruker,
    val tema: List<Arkivtema>?,
    val journalposttype: List<Journalposttype>?,
    val journalstatus: List<String>? = emptyList(),
    val antall: Int,
)

data class SafJournalpostRequest(
    val variables: Any,
    val query: String,
)

data class Journalpost(
    val journalpostId: String,
    val journalposttype: Journalposttype,
    val journalstatus: Journalstatus,
    val tema: String? = null,
    val behandlingstema: String? = null,
    val tittel: String? = null,
    val bruker: Bruker? = null,
    val avsenderMottaker: AvsenderMottaker? = null,
    val journalforendeEnhet: String? = null,
    val kanal: String? = null,
    val dokumenter: List<DokumentInfo>? = null,
    val relevanteDatoer: List<RelevantDato>? = null,
    val eksternReferanseId: String? = null,
) {
    val datoMottatt = relevanteDatoer?.firstOrNull { it.datotype == "DATO_REGISTRERT" }?.dato
}

data class RelevantDato(
    val dato: LocalDateTime,
    val datotype: String,
)

enum class Arkivtema(
    val navn: String,
) {
    AAP("Arbeidsavklaringspenger"),
    AAR("Aa-registeret"),
    AGR("Ajourhold - Grunnopplysninger"),
    ARP("Arbeidsrådgivning – psykologtester"),
    ARS("Arbeidsrådgivning – skjermet"),
    BAR("Barnetrygd"),
    BID("Bidrag"),
    BIL("Bil"),
    DAG("Dagpenger"),
    ENF("Enslig forsørger"),
    ERS("Erstatning"),
    EYB("Barnepensjon"),
    EYO("Omstillingsstønad"),
    FAR("Farskap"),
    FIP("Fiskerpensjon"),
    FOS("Forsikring"),
    FUL("Fullmakt"),
    GEN("Generell"),
    GRA("Gravferdsstønad"),
    GRU("Grunn- og hjelpestønad"),
    HEL("Helsetjenester og ortopediske hjelpemidler"),
    HJE("Hjelpemidler"),
    IAR("Inkluderende arbeidsliv"),
    IND("Tiltakspenger"),
    KLL("Klage – lønnsgaranti"),
    KON("Kontantstøtte"),
    KTA("Kontroll – anmeldelse"),
    KTR("Kontroll"),
    MED("Medlemskap"),
    MOB("Mobilitetsfremmende stønad"),
    FOR("Foreldre- og svangerskapspenger"),
    FEI("Feilutbetaling"),
    FRI("Kompensasjon for selvstendig næringsdrivende/frilansere"),
    OKO("Økonomi"),
    OMS("Omsorgspenger pleiepenger og opplæringspenger"),
    OPA("Oppfølging - Arbeidsgiver"),
    OPP("Oppfølging"),
    PEN("Pensjon"),
    PER("Permittering og masseoppsigelser"),
    REH("Rehabilitering"),
    REK("Rekruttering og stilling"),
    RPO("Retting av personopplysninger"),
    RVE("Rettferdsvederlag"),
    SAA("Sanksjon - Arbeidsgiver"),
    SAK("Saksomkostninger"),
    SAP("Sanksjon - Person"),
    SER("Serviceklager"),
    SIK("Sikkerhetstiltak"),
    STO("Regnskap/utbetaling"),
    SUP("Supplerende stønad"),
    SYK("Sykepenger"),
    SYM("Sykmeldinger"),
    TIL("Tiltak"),
    TRK("Trekkhåndtering"),
    TRY("Trygdeavgift"),
    TSO("Tilleggsstønad"),
    TSR("Tilleggsstønad arbeidssøkere"),
    UFM("Unntak fra medlemskap"),
    UFO("Uføretrygd"),
    UKJ("Ukjent"),
    VEN("Ventelønn"),
    YRA("Yrkesrettet attføring"),
    YRK("Yrkesskade"),
}

enum class Journalposttype {
    I,
    U,
    N,
}

data class DokumentInfo(
    val dokumentInfoId: String,
    val tittel: String? = null,
    val brevkode: String? = null,
    val dokumentstatus: Dokumentstatus? = null,
    val dokumentvarianter: List<Dokumentvariant>? = null,
    val logiskeVedlegg: List<LogiskVedlegg>? = null,
)

enum class Dokumentstatus {
    FERDIGSTILT,
    AVBRUTT,
    UNDER_REDIGERING,
    KASSERT,
}

data class Dokumentvariant(
    val variantformat: Dokumentvariantformat,
    val filnavn: String? = null,
    val saksbehandlerHarTilgang: Boolean,
)

enum class Dokumentvariantformat {
    ORIGINAL,
    ARKIV,
    FULLVERSJON,
    PRODUKSJON,
    PRODUKSJON_DLF,
    SLADDET,
}

data class LogiskVedlegg(
    val logiskVedleggId: String,
    val tittel: String,
)

data class DokumentinfoDto(
    val dokumentinfoId: String,
    val filnavn: String?,
    val tittel: String,
    val journalpostId: String,
    val dato: LocalDateTime?,
    val tema: String?,
    val journalstatus: Journalstatus,
    val journalposttype: Journalposttype,
    val harSaksbehandlerTilgang: Boolean,
    val logiskeVedlegg: List<LogiskVedleggDto>,
    val avsenderMottaker: AvsenderMottaker?,
)

enum class Journalstatus {
    MOTTATT,
    JOURNALFOERT,
    FERDIGSTILT,
    EKSPEDERT,
    UNDER_ARBEID,
    FEILREGISTRERT,
    UTGAAR,
    AVBRUTT,
    UKJENT_BRUKER,
    RESERVERT,
    OPPLASTING_DOKUMENT,
    UKJENT,
}

data class AvsenderMottaker(
    val id: String?,
    val type: AvsenderMottakerIdType?,
    val navn: String?,
    val land: String?,
    val erLikBruker: Boolean,
)

enum class AvsenderMottakerIdType {
    FNR,
    HPRNR,
    NULL,
    ORGNR,
    UKJENT,
    UTL_ORG,
}

data class LogiskVedleggDto(
    val tittel: String,
)
