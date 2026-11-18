package no.nav.grunn.og.hjelpestonad.steg.satsOgTrygdetid

import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingSteg
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringType
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.BeregnTilkjentYtelseService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SatsOgTrygdetidService(
    private val behandlingService: BehandlingService,
    private val beregnTilkjentYtelseService: BeregnTilkjentYtelseService,
    private val ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
    private val endringshistorikkService: EndringshistorikkService,
) {
    @Transactional
    fun fullførSteg(behandlingId: UUID) {
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
        behandlingService.validerKanEndreSteg(behandlingId, BehandlingSteg.SATS_OG_TRYGDETID)

        beregnTilkjentYtelseService.beregnOgLagreTilkjentYtelse(behandlingId)
        behandlingService.settNåværendeSteg(behandlingId, BehandlingSteg.VEDTAK_OG_BREV)
        endringshistorikkService.registrerEndring(behandlingId, EndringType.SATS_OG_TRYGDETID_FULLFØRT)
    }
}
