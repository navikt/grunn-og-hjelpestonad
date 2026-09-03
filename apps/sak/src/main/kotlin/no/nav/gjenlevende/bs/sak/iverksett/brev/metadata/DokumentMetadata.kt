package no.nav.gjenlevende.bs.sak.iverksett.brev.metadata

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import no.nav.gjenlevende.bs.sak.iverksett.brev.domene.Dokumentkategori
import no.nav.gjenlevende.bs.sak.iverksett.brev.domene.Dokumenttype
import no.nav.gjenlevende.bs.sak.iverksett.brev.domene.Fagsystem
import no.nav.gjenlevende.bs.sak.iverksett.brev.domene.JournalpostType

sealed interface Dokumentmetadata {
    val journalpostType: JournalpostType
    val fagsakSystem: Fagsystem?
    val tema: String
    val behandlingstema: Behandlingstema?
    val kanal: String?
    val dokumenttype: Dokumenttype
    val tittel: String?
    val brevkode: String? // NB: Maks lengde som er støttet i joark er 50 tegn
    val dokumentKategori: Dokumentkategori
}

enum class Behandlingstema(
    @JsonValue val value: String,
) {
    Barnetilsyn("ab0028"),
    Skolepenger("ab0177"),
    ;

    companion object {
        private val behandlingstemaMap = entries.associateBy(Behandlingstema::value) + entries.associateBy { it.name }

        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): Behandlingstema = behandlingstemaMap[value] ?: throw error("Fant ikke Behandlingstema for value=$value")
    }
}

fun Dokumenttype.tilMetadata(): Dokumentmetadata =
    when (this) {
        Dokumenttype.VEDTAKSBREV_BARNETILSYN -> BarnetilsynVedtaksbrevMetadata
        Dokumenttype.VEDTAKSBREV_SKOLEPENGER -> SkolepengerVedtaksbrevMetadata
    }
