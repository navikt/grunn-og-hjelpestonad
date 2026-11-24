package no.nav.grunn.og.hjelpestonad.iverksett.brev.metadata

import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.Dokumentkategori
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.Dokumenttype
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.Fagsystem
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.JournalpostType
import no.nav.grunn.og.hjelpestonad.oppgave.Tema
import org.springframework.stereotype.Component

@Component
object GrunnstønadVedtaksbrevMetadata : Dokumentmetadata {
    override val journalpostType: JournalpostType = JournalpostType.UTGAAENDE
    override val fagsakSystem: Fagsystem = Fagsystem.GRUNN_OG_HJELP
    override val tema: String = Tema.GRU.name
    override val behandlingstema: Behandlingstema = Behandlingstema.Grunnstønad
    override val kanal: String? = null
    override val dokumenttype: Dokumenttype = Dokumenttype.VEDTAKSBREV_GRUNNSTØNAD // kodeverk her, eller bare et navn vi legger på?
    override val tittel: String? = null
    override val brevkode: String = "GRU_BREV_GRUNNSTØNAD_VEDTAK" // usikker TDO sjekk denne
    override val dokumentKategori: Dokumentkategori = Dokumentkategori.VB
}
