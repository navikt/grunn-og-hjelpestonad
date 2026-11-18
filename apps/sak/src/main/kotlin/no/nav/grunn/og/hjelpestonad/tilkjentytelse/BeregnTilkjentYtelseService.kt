package no.nav.grunn.og.hjelpestonad.tilkjentytelse

import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
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
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    @Transactional
    fun beregnOgLagreTilkjentYtelse(behandlingId: UUID) {
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
    }

    /** Brukes når inngangsvilkårene ikke er oppfylt og sats og trygdetid hoppes over. */
    @Transactional
    fun lagreTilkjentYtelseUtenAndeler(behandlingId: UUID) = lagreTilkjentYtelse(behandlingId, emptySet())

    private fun lagreTilkjentYtelse(
        behandlingId: UUID,
        andelerTilkjentYtelse: Set<AndelTilkjentYtelse>,
    ) {
        val behandling = behandlingService.hentBehandlingEllerKast(behandlingId)
        val forrigeStartmåned = behandling.forrigeBehandlingId?.let { tilkjentYtelseService.hentTilkjentYtelse(it)?.startmåned }
        if (andelerTilkjentYtelse.isEmpty() && forrigeStartmåned == null) {
            tilkjentYtelseService.slettTilkjentYtelseHvisFinnes(behandlingId)
            return
        }
        val startmåned = utledStartmåned(andelerTilkjentYtelse, forrigeStartmåned, opphørsmåned = null)
        tilkjentYtelseService.slettTilkjentYtelseHvisFinnes(behandlingId)
        tilkjentYtelseService.lagre(TilkjentYtelse(behandlingId = behandlingId, startmåned = startmåned, andelerTilkjentYtelse = andelerTilkjentYtelse))
    }
}
