package no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter

import no.nav.grunn.og.hjelpestonad.felles.sporbar.Sporbar
import no.nav.grunn.og.hjelpestonad.vilkår.VilkårPeriode
import no.nav.grunn.og.hjelpestonad.vilkår.Vurdering
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.util.UUID

@Table("vilkar_ekstrautgifter")
data class VilkårEkstrautgifter(
    @Id
    override val id: UUID = UUID.randomUUID(),
    override val behandlingId: UUID,
    override val vurdering: Vurdering,
    val sats: Sats? = null,
    override val begrunnelse: String = "",
    override val fraOgMedDato: LocalDate? = null,
    override val tilOgMedDato: LocalDate? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    override val sporbar: Sporbar = Sporbar(),
    /** Ekstrautgiftene lagret som én rad per hjemmelbokstav og diagnose. Bruk [ekstrautgifter] for å lese dem. */
    @MappedCollection(idColumn = "vilkar_ekstrautgifter_id")
    private val ekstrautgiftDiagnoser: Set<EkstrautgiftDiagnose> = emptySet(),
) : VilkårPeriode<VilkårEkstrautgifter> {
    /** Én ekstrautgift per hjemmelbokstav, sortert etter bokstav. */
    val ekstrautgifter: List<Ekstrautgift>
        get() =
            ekstrautgiftDiagnoser
                .groupBy({ it.hjemmelbokstav }, { it.diagnosekode })
                .toSortedMap()
                .map { (bokstav, koder) -> Ekstrautgift(bokstav, koder.toSortedSet()) }

    val bokstaver: Set<Hjemmelbokstav> get() = ekstrautgiftDiagnoser.mapTo(mutableSetOf()) { it.hjemmelbokstav }

    fun medEkstrautgifter(ekstrautgifter: List<Ekstrautgift>) = copy(ekstrautgiftDiagnoser = ekstrautgifter.tilRader())

    override fun kopierTilBehandling(behandlingId: UUID) = copy(id = UUID.randomUUID(), behandlingId = behandlingId, sporbar = Sporbar())

    override fun kopierMedTidsrom(
        id: UUID,
        fraOgMedDato: LocalDate?,
        tilOgMedDato: LocalDate?,
    ) = copy(id = id, fraOgMedDato = fraOgMedDato, tilOgMedDato = tilOgMedDato)
}

/** En nødvendig ekstrautgift: hva den går til ([hjemmelbokstav]) og hvilke diagnoser som gir den. */
data class Ekstrautgift(
    val hjemmelbokstav: Hjemmelbokstav,
    val diagnosekoder: Set<String>,
)

/** Lagringsformen for [Ekstrautgift]: én rad per diagnose. */
@Table("vilkar_ekstrautgift_diagnose")
data class EkstrautgiftDiagnose(
    val hjemmelbokstav: Hjemmelbokstav,
    val diagnosekode: String,
)

private fun List<Ekstrautgift>.tilRader() = flatMapTo(mutableSetOf()) { utgift -> utgift.diagnosekoder.map { EkstrautgiftDiagnose(utgift.hjemmelbokstav, it) } }

enum class Sats(
    val nummer: Int,
) {
    SATS_1(1),
    SATS_2(2),
    SATS_3(3),
    SATS_4(4),
    SATS_5(5),
    SATS_6(6),
}

enum class Hjemmelbokstav(
    /** Bokstav d og e gir ikke grunnstønad i nye tilfeller fra 1. januar 2020, men løpende saker kan fortsatt ha dem. */
    val gjelderNyeTilfeller: Boolean = true,
) {
    A,
    B,
    C,
    D(gjelderNyeTilfeller = false),
    E(gjelderNyeTilfeller = false),
    F,
    G,
    H,
}
