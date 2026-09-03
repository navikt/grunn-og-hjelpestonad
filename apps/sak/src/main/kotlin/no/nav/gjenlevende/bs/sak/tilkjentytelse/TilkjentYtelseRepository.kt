package no.nav.gjenlevende.bs.sak.tilkjentytelse

import no.nav.gjenlevende.bs.sak.felles.InsertUpdateRepository
import no.nav.gjenlevende.bs.sak.felles.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TilkjentYtelseRepository :
    RepositoryInterface<TilkjentYtelse, UUID>,
    InsertUpdateRepository<TilkjentYtelse> {
    fun findByBehandlingId(behandlingId: UUID): TilkjentYtelse?

    fun deleteByBehandlingId(behandlingId: UUID)
}
