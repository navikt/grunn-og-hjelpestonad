package no.nav.grunn.og.hjelpestonad.tilkjentytelse

import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringType
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.trygdetid.TrygdetidService
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnoseService
import no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter.VilkårEkstrautgifterService
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskapService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.YearMonth
import java.util.UUID

@Service
class BeregnTilkjentYtelseService(
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val vilkårMedlemskapService: VilkårMedlemskapService,
    private val vilkårDiagnoseService: VilkårDiagnoseService,
    private val vilkårEkstrautgifterService: VilkårEkstrautgifterService,
    private val trygdetidService: TrygdetidService,
    private val ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
    private val endringshistorikkService: EndringshistorikkService,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    @Transactional
    fun fullførSteg(behandlingId: UUID) {
        behandlingService.validerBehandlingErRedigerbar(behandlingId)
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)

        val vilkårMedlemskapPerioder = vilkårMedlemskapService.hentPerioder(behandlingId)
        val vilkårDiagnosePerioder = vilkårDiagnoseService.hentPerioder(behandlingId)
        val vilkårEkstrautgifterPerioder = vilkårEkstrautgifterService.hentPerioder(behandlingId)
        val utlededeAndeler =
            TilkjentYtelseUtleder.utled(
                vilkårMedlemskapPerioder,
                vilkårDiagnosePerioder,
                vilkårEkstrautgifterPerioder,
                trygdetidService.hentPerioder(behandlingId),
            )
        val andelerTilkjentYtelse = fyllInnManglendeSluttdato(utlededeAndeler, YearMonth.now(clock), behandlingId)
        lagreTilkjentYtelse(behandlingId, andelerTilkjentYtelse)
        endringshistorikkService.registrerEndring(
            behandlingId,
            EndringType.SATS_OG_TRYGDETID_FULLFØRT,
            "${andelerTilkjentYtelse.size} andeler" +
                (andelerTilkjentYtelse.minOfOrNull { it.fom }?.let { ", fra $it til ${andelerTilkjentYtelse.maxOf { andel -> andel.tom }}" } ?: ""),
        )
    }

    /** Brukes også når steg 1 hopper over sats og trygdetid fordi inngangsvilkårene ikke er oppfylt. */
    @Transactional
    fun lagreTilkjentYtelseUtenAndeler(behandlingId: UUID) = lagreTilkjentYtelse(behandlingId, emptySet())

    private fun lagreTilkjentYtelse(
        behandlingId: UUID,
        andelerTilkjentYtelse: Set<AndelTilkjentYtelse>,
    ) {
        val behandling = behandlingService.hentBehandlingEllerKast(behandlingId)
        val forrigeStartmåned = behandling.forrigeBehandlingId?.let { tilkjentYtelseService.hentTilkjentYtelse(it)?.startmåned }
        val startmåned =
            if (andelerTilkjentYtelse.isEmpty() && forrigeStartmåned == null) {
                // Ingen utbetaling i en førstegangsbehandling. Startmåneden brukes ikke til et oppdrag.
                YearMonth.now(clock)
            } else {
                utledStartmåned(andelerTilkjentYtelse, forrigeStartmåned, opphørsmåned = null)
            }
        tilkjentYtelseService.slettTilkjentYtelseHvisFinnes(behandlingId)
        tilkjentYtelseService.lagre(TilkjentYtelse(behandlingId = behandlingId, startmåned = startmåned, andelerTilkjentYtelse = andelerTilkjentYtelse))
    }
}
