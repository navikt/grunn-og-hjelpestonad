package no.nav.grunn.og.hjelpestonad.tilkjentytelse

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TilkjentYtelseService(
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
) {
    fun hentTilkjentYtelse(behandlingId: UUID): TilkjentYtelse? = tilkjentYtelseRepository.findByBehandlingId(behandlingId)

    fun lagre(tilkjentYtelse: TilkjentYtelse) {
        val existing = tilkjentYtelseRepository.findByBehandlingId(tilkjentYtelse.behandlingId)
        if (existing != null) {
            tilkjentYtelseRepository.update(tilkjentYtelse.copy(id = existing.id))
        } else {
            tilkjentYtelseRepository.insert(tilkjentYtelse)
        }
    }

    fun slettTilkjentYtelseHvisFinnes(behandlingId: UUID) {
        if (tilkjentYtelseRepository.findByBehandlingId(behandlingId) != null) {
            tilkjentYtelseRepository.deleteByBehandlingId(behandlingId)
        }
    }
}
