package no.nav.grunn.og.hjelpestonad.iverksett.brev.domene

data class ArkiverDokumentResponse(
    val dokumenter: List<DokumentInfoResponse>,
    val journalpostId: String,
    val journalpostferdigstilt: Boolean,
)

data class DokumentInfoResponse(
    val dokumentInfoId: String,
)
