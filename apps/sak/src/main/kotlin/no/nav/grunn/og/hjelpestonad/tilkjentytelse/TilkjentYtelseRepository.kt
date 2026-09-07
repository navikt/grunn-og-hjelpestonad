package no.nav.grunn.og.hjelpestonad.tilkjentytelse

import no.nav.grunn.og.hjelpestonad.felles.InsertUpdateRepository
import no.nav.grunn.og.hjelpestonad.felles.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TilkjentYtelseRepository :
    RepositoryInterface<TilkjentYtelse, UUID>,
    InsertUpdateRepository<TilkjentYtelse> {
    fun findByBehandlingId(behandlingId: UUID): TilkjentYtelse?

    fun deleteByBehandlingId(behandlingId: UUID)
}
