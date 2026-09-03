package no.nav.gjenlevende.bs.sak.iverksett.brev.domene

data class ArkiverDokumentResponse(
    val dokumenter: List<DokumentInfoResponse>,
    val journalpostId: String,
    val journalpostferdigstilt: Boolean,
)

data class DokumentInfoResponse(
    val dokumentInfoId: String,
)
