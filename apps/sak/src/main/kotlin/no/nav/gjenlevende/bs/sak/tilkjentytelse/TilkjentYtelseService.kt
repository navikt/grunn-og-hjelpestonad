package no.nav.gjenlevende.bs.sak.tilkjentytelse

import no.nav.gjenlevende.bs.sak.vedtak.BarnetilsynBeregning
import no.nav.gjenlevende.bs.sak.vedtak.BeløpsperioderDto
import no.nav.gjenlevende.bs.sak.vedtak.BeregningUtils.beregnBarnetilsynperiode
import no.nav.gjenlevende.bs.sak.vedtak.PeriodetypeBarnetilsyn
import no.nav.gjenlevende.bs.sak.vedtak.ResultatType
import no.nav.gjenlevende.bs.sak.vedtak.Vedtak
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TilkjentYtelseService(
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
) {
    fun opprettEllerOppdaterTilkjentYtelse(
        behandlingId: UUID,
        vedtak: Vedtak,
    ) {
        if (vedtak.resultatType == ResultatType.AVSLÅTT || vedtak.resultatType == ResultatType.HENLAGT) return

        val andeler =
            when (vedtak.resultatType) {
                ResultatType.INNVILGET -> beregnAndeler(behandlingId, vedtak)
                else -> emptySet() // OPPHØR
            }

        val tilkjentYtelse =
            TilkjentYtelse(
                behandlingId = behandlingId,
                andelerTilkjentYtelse = andeler,
            )

        val existing = tilkjentYtelseRepository.findByBehandlingId(behandlingId)
        if (existing != null) {
            tilkjentYtelseRepository.update(tilkjentYtelse.copy(id = existing.id))
        } else {
            tilkjentYtelseRepository.insert(tilkjentYtelse)
        }
    }

    fun hentTilkjentYtelse(behandlingId: UUID): TilkjentYtelse? = tilkjentYtelseRepository.findByBehandlingId(behandlingId)

    fun slettTilkjentYtelseHvisFinnes(behandlingId: UUID) {
        if (tilkjentYtelseRepository.findByBehandlingId(behandlingId) != null) {
            tilkjentYtelseRepository.deleteByBehandlingId(behandlingId)
        }
    }

    private fun beregnAndeler(
        behandlingId: UUID,
        vedtak: Vedtak,
    ): Set<AndelTilkjentYtelse> {
        val beregninger =
            vedtak.barnetilsynperioder.map { periode ->
                BarnetilsynBeregning(
                    datoFra = periode.datoFra,
                    datoTil = periode.datoTil,
                    utgifter = periode.utgifter,
                    barn = periode.barn,
                    periodetype = periode.periodetype,
                )
            }
        return beregnBarnetilsynperiode(beregninger)
            .filter { it.periodetype == PeriodetypeBarnetilsyn.ORDINÆR && it.beløp > 0 }
            .map { it.tilAndelTilkjentYtelse(kildeBehandlingId = behandlingId) }
            .toSet()
    }
}

private fun BeløpsperioderDto.tilAndelTilkjentYtelse(kildeBehandlingId: UUID) =
    AndelTilkjentYtelse(
        beløp = this.beløp,
        fom = this.datoFra.atDay(1),
        tom = this.datoTil.atEndOfMonth(),
        kildeBehandlingId = kildeBehandlingId,
    )
