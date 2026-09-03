package no.nav.gjenlevende.bs.sak.brev

import no.nav.familie.prosessering.domene.Task
import no.nav.gjenlevende.bs.sak.behandling.BehandlingService
import no.nav.gjenlevende.bs.sak.brev.domain.BrevRequest
import no.nav.gjenlevende.bs.sak.endringshistorikk.EndringType
import no.nav.gjenlevende.bs.sak.endringshistorikk.EndringshistorikkService
import no.nav.gjenlevende.bs.sak.felles.sikkerhet.SikkerhetContext
import no.nav.gjenlevende.bs.sak.oppgave.AnsvarligSaksbehandlerService
import no.nav.gjenlevende.bs.sak.saksbehandler.EntraProxyClient
import no.nav.gjenlevende.bs.sak.task.BrevTask
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@Service
class BrevService(
    private val brevRepository: BrevRepository,
    private val behandlingService: BehandlingService,
    private val objectMapper: ObjectMapper,
    private val entraProxyClient: EntraProxyClient,
    private val endringshistorikkService: EndringshistorikkService,
    private val ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
) {
    fun lagBrevPdfTask(behandlingId: UUID): Task =
        BrevTask.opprettTask(
            objectMapper.writeValueAsString(
                BrevTask.LagBrevPdfTaskData(behandlingId),
            ),
        )

    @Transactional
    fun mellomlagreBrev(
        behandlingId: UUID,
        brevRequest: BrevRequest,
    ) {
        behandlingService.validerBehandlingErRedigerbar(behandlingId)
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
        val brev =
            Brev(
                behandlingId = behandlingId,
                brevJson = brevRequest,
            )

        if (brevRepository.existsById(behandlingId)) {
            brevRepository.update(brev)
        } else {
            brevRepository.insert(brev)
        }
    }

    fun hentBrev(behandlingId: UUID): Brev? = brevRepository.findByIdOrNull(behandlingId)

    @Transactional
    fun oppdatereBrevPdf(
        behandlingId: UUID,
        pdf: ByteArray,
    ) {
        val eksisterendeBrev =
            brevRepository.findByIdOrNull(behandlingId)
                ?: error("Fant ikke brev for behandlingId=$behandlingId ved lagring av PDF")
        val oppdatertBrevPdf = eksisterendeBrev.copy(brevPdf = pdf)
        brevRepository.update(oppdatertBrevPdf)
    }

    @Transactional
    fun oppdaterSaksbehandlerForBrev(
        behandlingId: UUID,
    ) {
        val saksbehandler = SikkerhetContext.hentSaksbehandlerEllerSystembruker()
        val saksbehandlerInfo = entraProxyClient.hentSaksbehandlerInfo(saksbehandler)
        val saksbehandlerNavn = "${saksbehandlerInfo.fornavn} ${saksbehandlerInfo.etternavn}"
        val saksbehandlerEnhet = saksbehandlerInfo.enhet.navn
        val eksisterendeBrev =
            brevRepository.findByIdOrNull(behandlingId)
                ?: error("Fant ikke brev for behandlingId=$behandlingId ved oppdatering av saksbehandler")
        val oppdatert = eksisterendeBrev.copy(saksbehandler = saksbehandlerNavn, saksbehandlerEnhet = saksbehandlerEnhet)
        brevRepository.update(oppdatert)
    }

    @Transactional
    fun oppdaterBeslutterForBrev(
        behandlingId: UUID,
    ) {
        val beslutter = SikkerhetContext.hentSaksbehandlerEllerSystembruker()
        val beslutterInfo = entraProxyClient.hentSaksbehandlerInfo(beslutter)
        val beslutterNavn = "${beslutterInfo.fornavn} ${beslutterInfo.etternavn}"
        val beslutterEnhetnavn = beslutterInfo.enhet.navn
        val beslutterEnhetnummer = beslutterInfo.enhet.enhetnummer
        val eksisterendeBrev =
            brevRepository.findByIdOrNull(behandlingId)
                ?: error("Fant ikke brev for behandlingId=$behandlingId ved oppdatering av beslutter")
        val oppdatertBrev = eksisterendeBrev.copy(beslutter = beslutterNavn, beslutterEnhetnavn = beslutterEnhetnavn, beslutterEnhetnummer = beslutterEnhetnummer)
        brevRepository.update(oppdatertBrev)
        endringshistorikkService.registrerEndring(
            behandlingId = behandlingId,
            endringType = EndringType.BESLUTTER_GODKJENT,
        )
    }

    fun lagHtml(brev: Brev): String {
        val brevInnhold = brev.brevJson
        val brukerNavn = brevInnhold.brevmal.informasjonOmBruker.navn
        val brukerPersonident = brevInnhold.brevmal.informasjonOmBruker.fnr
        val dagensDato = LocalDate.now().format(DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.forLanguageTag("no")))
        val tittel = brevInnhold.brevmal.tittel
        val logoSvg = hentLogoSvg()
        val fritekst = lagHtmlTekstbolker(brevInnhold.fritekstbolker)
        val avslutning = lagHtmlTekstbolker(brevInnhold.brevmal.fastTekstAvslutning)
        val saksbehandlerNavn = brev.saksbehandler ?: ""
        val beslutterNavn = brev.beslutter
        val saksbehandlerEnhet = "Nav familie- og pensjonsytelser ${brev.saksbehandlerEnhet ?: ""}"

        return """
            <!DOCTYPE html>
            <html lang="no">
            <head>
                <meta charset="UTF-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <title>$tittel</title>
                <style type="text/css">
                    body { font-family: 'Source Sans Pro', sans-serif; font-size: 11pt; line-height: 12pt; margin-left: 48pt; margin-right: 48pt; }
                    header { margin-bottom: 12pt; }
                    h1 { font-size: 16pt; line-height: 20pt; font-weight: 700; margin-bottom: 26pt; }
                    h2 { font-size: 13pt; line-height: 16pt; font-weight: 700; margin-bottom: 6pt;}
                    h3 { font-size: 12pt; line-height: 16pt; font-weight: 700; margin-bottom: 6pt;}
                    section { margin-bottom: 26pt; }
                    .header {
                        padding-top: 32pt;
                        margin-bottom: 48pt
                    }
                    .logo {
                        display: block;
                        margin-bottom: 32pt;
                        width: 48pt;
                    }
                    .logo svg {
                        width: 100%;
                        height: auto;
                    }
                    .bruker-info {
                    display: flex;
                    flex-direction: row;
                    justify-content: space-between;
                    align-items: flex-start;
                    }
                    .bruker-info .venstre { display: table; }
                    .bruker-info .venstre .row { display: table-row; }
                    .bruker-info .label {
                        display: table-cell;
                        white-space: nowrap;
                        padding-right: 12pt;
                    }
                    .bruker-info .value {
                        display: table-cell;
                    }
                    .bruker-info .høyre {
                        text-align: right;
                        position: relative;
                        top: -12pt;
                    }
                    footer {
                        display: block;
                        page-break-inside: avoid;
                        break-inside: avoid;
                    }
                    .signatur {
                        display: table;
                        margin-top: 24pt;
                    }

                    .signatur .row {
                        display: table-row;
                    }

                    .signatur .cell {
                        display: table-cell;
                        padding-right: 100pt;
                        white-space: nowrap;
                    }
                </style>
            </head>
            <body>
                <header class="header">
                    <div class="logo">$logoSvg</div>
                    <div class="bruker-info">
                        <div class="venstre">
                            <div class="row"><span class="label">Navn:</span><span class="value">$brukerNavn</span></div>
                            <div class="row"><span class="label">Fødselsnummer:</span><span class="value">$brukerPersonident</span></div>
                        </div>
                        <div class="høyre">
                            <span>$dagensDato</span>
                        </div>
                    </div>
                </header>
                <main>
                    <h1>$tittel</h1>
                    $fritekst
                    $avslutning
                </main>
                <footer>
                    <p> Med vennlig hilsen,</p>
                    <div class="signatur">
                        <div class="row">
                            <span class="cell">$beslutterNavn</span>
                            <span class="cell">$saksbehandlerNavn</span>
                        </div>
                        <br />
                        <div class="row">
                            <span class="cell">$saksbehandlerEnhet</span>
                        </div>
                    </div>
                </footer>
            </body>
            </html>
            """.trimIndent()
    }
}
