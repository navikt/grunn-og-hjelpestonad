package no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering

import java.util.UUID

enum class InngangsvilkårFeilkode {
    FALSK_IDENTITET,
}

data class InngangsvilkårBlokkertResponse(
    val feilkode: InngangsvilkårFeilkode,
    val melding: String,
    val status: Int,
)

/**
 * Behandlingen kan ikke fortsette forbi inngangsvilkårssteget på grunn av en blokkerende tilstand
 * (for eksempel falsk identitet i Folkeregisteret). Håndteres av lokal `@ExceptionHandler` i
 * `InngangsvilkårStegController` med HTTP 422.
 */
class InngangsvilkårBlokkertFeil(
    val behandlingId: UUID,
    val feilkode: InngangsvilkårFeilkode,
    val melding: String,
) : RuntimeException(melding)
