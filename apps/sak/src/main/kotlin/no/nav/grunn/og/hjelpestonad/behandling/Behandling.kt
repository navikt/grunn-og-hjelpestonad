package no.nav.grunn.og.hjelpestonad.behandling

import no.nav.grunn.og.hjelpestonad.felles.sporbar.Sporbar
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import org.springframework.http.HttpStatus
import java.util.UUID

@Table("behandling")
data class Behandling(
    @Id
    val id: UUID = UUID.randomUUID(),
    val eksternId: Long = 0,
    val fagsakId: UUID,
    val forrigeBehandlingId: UUID? = null,
    val status: BehandlingStatus,
    val resultat: BehandlingResultat,
    /** Steget saksbehandler skal fullføre nå. Alle steg før det er fullført. */
    @Column("navarende_steg")
    val nåværendeSteg: BehandlingSteg = BehandlingSteg.ÅRSAK,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)

fun Behandling.erRedigerbar(): Boolean = status in setOf(BehandlingStatus.OPPRETTET, BehandlingStatus.UTREDES)

fun Behandling.validerErRedigerbar() {
    if (!erRedigerbar()) {
        throw Feil(
            melding = "Behandlingen er ikke redigerbar. Status: $status",
            httpStatus = HttpStatus.BAD_REQUEST,
        )
    }
}

/** Kaster hvis [steg] kommer etter nåværende steg, altså ikke er nådd ennå. */
fun Behandling.validerHarNådd(steg: BehandlingSteg) {
    if (steg.rekkefølge > nåværendeSteg.rekkefølge) {
        throw Feil(
            melding = "Fullfør «${nåværendeSteg.visningsnavn}» før du går videre.",
            httpStatus = HttpStatus.BAD_REQUEST,
        )
    }
}

/** Begge sjekkene før en skriving som hører til [steg]: behandlingen er redigerbar, og steget er nådd. */
fun Behandling.validerKanEndre(steg: BehandlingSteg) {
    validerErRedigerbar()
    validerHarNådd(steg)
}

enum class BehandlingStatus {
    OPPRETTET,
    UTREDES,
    FATTER_VEDTAK,
    IVERKSETTER_VEDTAK,
    FERDIGSTILT,
}

enum class BehandlingResultat {
    INNVILGET,
    OPPHØRT,
    AVSLÅTT,
    IKKE_SATT,
    HENLAGT,
}
