package no.nav.grunn.og.hjelpestonad.oppdrag

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.AndelTilkjentYtelse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

class LagUtbetalingsoppdragTest {
    @Test
    fun `lager oppdrag med to andeler for førstegangsbehandling`() {
        val grunnlag = grunnlag(andeler = listOf(andel(1000, "2025-01-01", "2025-03-31"), andel(2000, "2025-04-01", "2025-06-30")))

        val oppdrag = lagUtbetalingsoppdrag(grunnlag)

        assertThat(oppdrag.kodeEndring).isEqualTo(no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag.KodeEndring.NY)
        assertThat(oppdrag.fagSystem).isEqualTo("IT28")
        assertThat(oppdrag.saksnummer).isEqualTo(grunnlag.eksternFagsakId.toString())
        assertThat(oppdrag.aktoer).isEqualTo(grunnlag.personIdent)
        assertThat(oppdrag.utbetalingsperiode).hasSize(2)
        assertThat(oppdrag.utbetalingsperiode.map { it.klassifisering }).containsExactly("GHGSORMG", "GHGSORMG")
        assertThat(oppdrag.utbetalingsperiode.map { it.satsType }).containsExactlyElementsOf(
            List(2) { no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode.SatsType.MND },
        )
        assertThat(oppdrag.utbetalingsperiode.map { it.behandlingId }).containsExactly(grunnlag.eksternBehandlingId, grunnlag.eksternBehandlingId)
        assertThat(oppdrag.utbetalingsperiode.map { it.periodeId }).containsExactly(0L, 1L)
        assertThat(oppdrag.utbetalingsperiode.map { it.forrigePeriodeId }).containsExactly(null, 0L)
    }

    @Test
    fun `filtrerer andeler med nullbeløp`() {
        val grunnlag = grunnlag(andeler = listOf(andel(0), andel(1000)))

        val oppdrag = lagUtbetalingsoppdrag(grunnlag)

        assertThat(oppdrag.utbetalingsperiode).hasSize(1)
        assertThat(oppdrag.utbetalingsperiode.single().sats).isEqualByComparingTo("1000")
    }

    @Test
    fun `ingen andeler kaster UgyldigInput`() {
        assertThatThrownBy { lagUtbetalingsoppdrag(grunnlag(andeler = emptyList())) }
            .isInstanceOf(UgyldigInput::class.java)
    }

    @Test
    fun `bare nullbeløpsandeler kaster UgyldigInput`() {
        assertThatThrownBy { lagUtbetalingsoppdrag(grunnlag(andeler = listOf(andel(0)))) }
            .isInstanceOf(UgyldigInput::class.java)
    }

    private fun grunnlag(andeler: List<AndelTilkjentYtelse>) =
        Oppdragsgrunnlag(
            behandlingId = UUID.fromString("00000000-0000-0000-0000-000000000001"),
            eksternBehandlingId = 123456L,
            eksternFagsakId = 654321L,
            personIdent = "12345678910",
            saksbehandlerId = "Z123456",
            vedtaksdato = LocalDate.parse("2025-07-01"),
            andeler = andeler,
        )

    private fun andel(
        beløp: Int,
        fom: String = "2025-01-01",
        tom: String = "2025-12-31",
    ) = AndelTilkjentYtelse(
        id = UUID.randomUUID(),
        beløp = beløp.toBigDecimal(),
        fom = LocalDate.parse(fom),
        tom = LocalDate.parse(tom),
        kildeBehandlingId = UUID.fromString("00000000-0000-0000-0000-000000000002"),
    )
}
