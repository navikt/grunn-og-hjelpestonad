package no.nav.grunn.og.hjelpestonad.tilkjentytelse

import no.nav.grunn.og.hjelpestonad.oppdrag.simulering.SimuleringRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TilkjentYtelseService(
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val simuleringRepository: SimuleringRepository,
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

    /**
     * Sletter også simuleringen, slik at ingen ser en simulering av en tilkjent ytelse som ikke finnes lenger (ADR-0018).
     */
    fun slettTilkjentYtelseHvisFinnes(behandlingId: UUID) {
        if (tilkjentYtelseRepository.findByBehandlingId(behandlingId) != null) {
            tilkjentYtelseRepository.deleteByBehandlingId(behandlingId)
        }
        simuleringRepository.slett(behandlingId)
    }
}
