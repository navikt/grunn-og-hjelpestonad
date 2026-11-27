package no.nav.grunn.og.hjelpestonad.oppdrag.utbetalingsgenerator.domain

data class BeregnetUtbetalingsoppdrag(
    val utbetalingsoppdrag: Utbetalingsoppdrag,
    val andeler: List<AndelMedPeriodeId>,
)

data class AndelMedPeriodeId(
    val id: String,
    val periodeId: Long,
    val forrigePeriodeId: Long?,
    val kildeBehandlingId: String,
) {
    constructor(andel: AndelData, nyKildeBehandlingId: String? = null) :
        this(
            id = andel.id,
            periodeId = andel.periodeId ?: error("Mangler offset på andel=${andel.id}"),
            forrigePeriodeId = andel.forrigePeriodeId,
            kildeBehandlingId =
                nyKildeBehandlingId ?: andel.kildeBehandlingId
                    ?: error("Mangler kildebehandlingId på andel=${andel.id}"),
        )
}
