package no.nav.grunn.og.hjelpestonad.oppdrag

import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag.KodeEndring
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode
import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.AndelTilkjentYtelse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

class LagUtbetalingsoppdragTest {
    private val forrigeBehandlingId = UUID.fromString("00000000-0000-0000-0000-00000000000f")
    private val behandlingId = UUID.fromString("00000000-0000-0000-0000-000000000001")

    @Test
    fun `førstegangsbehandling lager ny kjede fra periodeId 0`() {
        val grunnlag = grunnlag(andeler = listOf(andel(1000, "2025-01-01", "2025-03-31"), andel(2000, "2025-04-01", "2025-06-30")))

        val beregnet = lagUtbetalingsoppdrag(grunnlag)
        val oppdrag = beregnet.utbetalingsoppdrag

        assertThat(oppdrag.kodeEndring).isEqualTo(KodeEndring.NY)
        assertThat(oppdrag.fagSystem).isEqualTo("IT28")
        assertThat(oppdrag.saksnummer).isEqualTo(grunnlag.eksternFagsakId.toString())
        assertThat(oppdrag.aktoer).isEqualTo(grunnlag.personIdent)
        assertThat(oppdrag.utbetalingsperiode.map { it.klassifisering }).containsOnly("GHGSORMG")
        assertThat(oppdrag.utbetalingsperiode.map { it.satsType }).containsOnly(Utbetalingsperiode.SatsType.MND)
        assertThat(oppdrag.utbetalingsperiode.map { it.behandlingId }).containsOnly(grunnlag.eksternBehandlingId)
        assertThat(oppdrag.utbetalingsperiode.map { it.periodeId }).containsExactly(0L, 1L)
        assertThat(oppdrag.utbetalingsperiode.map { it.forrigePeriodeId }).containsExactly(null, 0L)

        assertThat(beregnet.harEndringFraForrigeBehandling).isTrue()
        assertThat(beregnet.utbetalingsandeler.utenId()).containsExactly(
            forventet(1000, "2025-01-01", "2025-03-31", periodeId = 0, forrigePeriodeId = null, kilde = behandlingId),
            forventet(2000, "2025-04-01", "2025-06-30", periodeId = 1, forrigePeriodeId = 0, kilde = behandlingId),
        )
    }

    @Test
    fun `filtrerer andeler med nullbeløp`() {
        val beregnet = lagUtbetalingsoppdrag(grunnlag(andeler = listOf(andel(0), andel(1000))))

        assertThat(
            beregnet.utbetalingsoppdrag.utbetalingsperiode
                .single()
                .sats,
        ).isEqualByComparingTo("1000")
        assertThat(beregnet.utbetalingsandeler).hasSize(1)
    }

    @Test
    fun `avrunder andelene til hele kroner`() {
        val beregnet = lagUtbetalingsoppdrag(grunnlag(andeler = listOf(andel("1234.50"))))

        assertThat(
            beregnet.utbetalingsoppdrag.utbetalingsperiode
                .single()
                .sats,
        ).isEqualByComparingTo("1235")
        assertThat(beregnet.utbetalingsandeler.single().beløp).isEqualTo(1235)
    }

    @Test
    fun `ingen andeler og ingen tidligere kjede kaster UgyldigInput`() {
        assertThatThrownBy { lagUtbetalingsoppdrag(grunnlag(andeler = emptyList())) }.isInstanceOf(UgyldigInput::class.java)
        assertThatThrownBy { lagUtbetalingsoppdrag(grunnlag(andeler = listOf(andel(0)))) }.isInstanceOf(UgyldigInput::class.java)
    }

    @Test
    fun `revurdering uten endring sender ingenting og beholder periodeId og kildebehandling`() {
        val forrige =
            listOf(
                utbetalingsandel(1000, "2025-01-01", "2025-03-31", periodeId = 0, forrigePeriodeId = null),
                utbetalingsandel(2000, "2025-04-01", "2025-06-30", periodeId = 1, forrigePeriodeId = 0),
            )

        val beregnet =
            lagUtbetalingsoppdrag(
                grunnlag(
                    andeler = listOf(andel(1000, "2025-01-01", "2025-03-31"), andel(2000, "2025-04-01", "2025-06-30")),
                    forrigeAndeler = forrige,
                    sisteAndelPerKjede = listOf(forrige.last()),
                ),
            )

        assertThat(beregnet.harEndringFraForrigeBehandling).isFalse()
        assertThat(beregnet.utbetalingsoppdrag.kodeEndring).isEqualTo(KodeEndring.ENDR)
        assertThat(beregnet.utbetalingsandeler.utenId()).containsExactly(
            forventet(1000, "2025-01-01", "2025-03-31", periodeId = 0, forrigePeriodeId = null, kilde = forrigeBehandlingId),
            forventet(2000, "2025-04-01", "2025-06-30", periodeId = 1, forrigePeriodeId = 0, kilde = forrigeBehandlingId),
        )
    }

    @Test
    fun `revurdering med nytt beløp fra en måned sender ny periode kjedet til siste andel`() {
        val forrige =
            listOf(
                utbetalingsandel(1000, "2025-01-01", "2025-03-31", periodeId = 0, forrigePeriodeId = null),
                utbetalingsandel(2000, "2025-04-01", "2025-06-30", periodeId = 1, forrigePeriodeId = 0),
            )

        val beregnet =
            lagUtbetalingsoppdrag(
                grunnlag(
                    andeler = listOf(andel(1000, "2025-01-01", "2025-03-31"), andel(2500, "2025-04-01", "2025-06-30")),
                    forrigeAndeler = forrige,
                    sisteAndelPerKjede = listOf(forrige.last()),
                ),
            )

        val periode = beregnet.utbetalingsoppdrag.utbetalingsperiode.single()
        assertThat(periode.opphør).isNull()
        assertThat(periode.periodeId).isEqualTo(2L)
        assertThat(periode.forrigePeriodeId).isEqualTo(1L)
        assertThat(periode.sats).isEqualByComparingTo("2500")
        assertThat(periode.vedtakdatoFom).isEqualTo(LocalDate.parse("2025-04-01"))
        assertThat(beregnet.utbetalingsandeler.utenId()).containsExactly(
            forventet(1000, "2025-01-01", "2025-03-31", periodeId = 0, forrigePeriodeId = null, kilde = forrigeBehandlingId),
            forventet(2500, "2025-04-01", "2025-06-30", periodeId = 2, forrigePeriodeId = 1, kilde = behandlingId),
        )
    }

    @Test
    fun `revurdering som forlenger siste andel sender den forlengede perioden kjedet til den gamle`() {
        val forrige = listOf(utbetalingsandel(1000, "2025-01-01", "2025-03-31", periodeId = 0, forrigePeriodeId = null))

        val beregnet =
            lagUtbetalingsoppdrag(
                grunnlag(
                    andeler = listOf(andel(1000, "2025-01-01", "2025-06-30")),
                    forrigeAndeler = forrige,
                    sisteAndelPerKjede = forrige,
                ),
            )

        val periode = beregnet.utbetalingsoppdrag.utbetalingsperiode.single()
        assertThat(periode.opphør).isNull()
        assertThat(periode.periodeId).isEqualTo(1L)
        assertThat(periode.forrigePeriodeId).isEqualTo(0L)
        assertThat(periode.vedtakdatoFom).isEqualTo(LocalDate.parse("2025-01-01"))
        assertThat(periode.vedtakdatoTom).isEqualTo(LocalDate.parse("2025-06-30"))
        assertThat(beregnet.utbetalingsandeler.utenId()).containsExactly(
            forventet(1000, "2025-01-01", "2025-06-30", periodeId = 1, forrigePeriodeId = 0, kilde = behandlingId),
        )
    }

    @Test
    fun `revurdering som forkorter siste andel opphører kjeden fra måneden etter`() {
        val forrige = listOf(utbetalingsandel(1000, "2025-01-01", "2025-06-30", periodeId = 0, forrigePeriodeId = null))

        val beregnet =
            lagUtbetalingsoppdrag(
                grunnlag(
                    andeler = listOf(andel(1000, "2025-01-01", "2025-03-31")),
                    forrigeAndeler = forrige,
                    sisteAndelPerKjede = forrige,
                ),
            )

        val periode = beregnet.utbetalingsoppdrag.utbetalingsperiode.single()
        assertThat(periode.erEndringPåEksisterendePeriode).isTrue()
        assertThat(periode.opphør?.opphørDatoFom).isEqualTo(LocalDate.parse("2025-04-01"))
        assertThat(periode.periodeId).isEqualTo(0L)
        assertThat(periode.vedtakdatoTom).isEqualTo(LocalDate.parse("2025-06-30"))
        // Den avkortede andelen beholder periodeId, men har nå sitt opphav i denne behandlingen.
        assertThat(beregnet.utbetalingsandeler.utenId()).containsExactly(
            forventet(1000, "2025-01-01", "2025-03-31", periodeId = 0, forrigePeriodeId = null, kilde = behandlingId),
        )
    }

    @Test
    fun `opphør av alle andeler opphører siste andel fra første måned og lagrer ingen andeler`() {
        val forrige =
            listOf(
                utbetalingsandel(1000, "2025-01-01", "2025-03-31", periodeId = 0, forrigePeriodeId = null),
                utbetalingsandel(2000, "2025-04-01", "2025-06-30", periodeId = 1, forrigePeriodeId = 0),
            )

        val beregnet =
            lagUtbetalingsoppdrag(
                grunnlag(andeler = emptyList(), forrigeAndeler = forrige, sisteAndelPerKjede = listOf(forrige.last())),
            )

        val periode = beregnet.utbetalingsoppdrag.utbetalingsperiode.single()
        assertThat(periode.periodeId).isEqualTo(1L)
        assertThat(periode.forrigePeriodeId).isEqualTo(0L)
        assertThat(periode.opphør?.opphørDatoFom).isEqualTo(LocalDate.parse("2025-01-01"))
        assertThat(beregnet.harEndringFraForrigeBehandling).isTrue()
        assertThat(beregnet.utbetalingsandeler).isEmpty()
    }

    @Test
    fun `ny andel etter fullt opphør kjedes til siste andel selv om forrige behandling ikke har andeler`() {
        val sisteAndel = utbetalingsandel(2000, "2025-04-01", "2025-06-30", periodeId = 1, forrigePeriodeId = 0)

        val beregnet =
            lagUtbetalingsoppdrag(
                grunnlag(
                    andeler = listOf(andel(1500, "2025-07-01", "2025-12-31")),
                    forrigeAndeler = emptyList(),
                    sisteAndelPerKjede = listOf(sisteAndel),
                ),
            )

        val periode = beregnet.utbetalingsoppdrag.utbetalingsperiode.single()
        assertThat(beregnet.utbetalingsoppdrag.kodeEndring).isEqualTo(KodeEndring.ENDR)
        assertThat(periode.opphør).isNull()
        assertThat(periode.periodeId).isEqualTo(2L)
        assertThat(periode.forrigePeriodeId).isEqualTo(1L)
        assertThat(beregnet.utbetalingsandeler.utenId()).containsExactly(
            forventet(1500, "2025-07-01", "2025-12-31", periodeId = 2, forrigePeriodeId = 1, kilde = behandlingId),
        )
    }

    private fun grunnlag(
        andeler: List<AndelTilkjentYtelse>,
        forrigeAndeler: List<Utbetalingsandel> = emptyList(),
        sisteAndelPerKjede: List<Utbetalingsandel> = emptyList(),
    ) = Oppdragsgrunnlag(
        behandlingId = behandlingId,
        eksternBehandlingId = 123456L,
        eksternFagsakId = 654321L,
        personIdent = "12345678910",
        saksbehandlerId = "Z123456",
        vedtaksdato = LocalDate.parse("2025-07-01"),
        andelerTilkjentYtelse = andeler,
        utbetalingsandelerForrigeBehandling = forrigeAndeler,
        sisteAndelIHverKjedeForFagsak = sisteAndelPerKjede,
    )

    private fun andel(
        beløp: Int,
        fom: String = "2025-01-01",
        tom: String = "2025-12-31",
    ) = andel(beløp.toString(), fom, tom)

    private fun andel(
        beløp: String,
        fom: String = "2025-01-01",
        tom: String = "2025-12-31",
    ) = AndelTilkjentYtelse(
        beløp = beløp.toBigDecimal(),
        fom = LocalDate.parse(fom),
        tom = LocalDate.parse(tom),
        kildeBehandlingId = behandlingId,
    )

    /** En andel lagret på forrige behandling, med egen id slik generatoren krever. */
    private fun utbetalingsandel(
        beløp: Int,
        fom: String,
        tom: String,
        periodeId: Long,
        forrigePeriodeId: Long?,
    ) = Utbetalingsandel(
        fom = LocalDate.parse(fom),
        tom = LocalDate.parse(tom),
        beløp = beløp,
        ytelsestype = GrunnOgHjelpYtelsestype.GRUNNSTØNAD,
        periodeId = periodeId,
        forrigePeriodeId = forrigePeriodeId,
        kildeBehandlingId = forrigeBehandlingId,
    )

    /** En andel slik den skal lagres på denne behandlingen. Id-en er tilfeldig og sammenlignes ikke. */
    private fun forventet(
        beløp: Int,
        fom: String,
        tom: String,
        periodeId: Long,
        forrigePeriodeId: Long?,
        kilde: UUID,
    ) = Utbetalingsandel(
        id = FAST_ID,
        fom = LocalDate.parse(fom),
        tom = LocalDate.parse(tom),
        beløp = beløp,
        ytelsestype = GrunnOgHjelpYtelsestype.GRUNNSTØNAD,
        periodeId = periodeId,
        forrigePeriodeId = forrigePeriodeId,
        kildeBehandlingId = kilde,
    )

    private fun List<Utbetalingsandel>.utenId() = map { it.copy(id = FAST_ID) }

    companion object {
        private val FAST_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa")
    }
}
