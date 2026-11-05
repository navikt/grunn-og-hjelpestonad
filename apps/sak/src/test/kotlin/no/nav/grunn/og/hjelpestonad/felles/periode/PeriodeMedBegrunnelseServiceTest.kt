package no.nav.grunn.og.hjelpestonad.felles.periode

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringType
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.felles.sporbar.Sporbar
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate
import java.util.Optional
import java.util.UUID
import kotlin.test.Test

/**
 * Detaljene i endringshistorikken bygges av fellesdelen, så en periode uten vurdering, slik
 * trygdetid blir i ticket 08, får bare type og periode. Vilkårene dekkes av testene for
 * medlemskap og diagnose. Kopiering av perioder uten vurdering testes også her, fordi de
 * kopieres alltid.
 */
class PeriodeMedBegrunnelseServiceTest {
    private val repository = mockk<PeriodeMedBegrunnelseRepository<TestPeriode>>(relaxed = true)
    private val endringshistorikkService = mockk<EndringshistorikkService>(relaxed = true)
    private val service =
        TestPeriodeService(
            repository,
            mockk<BehandlingService>(relaxed = true),
            endringshistorikkService,
            mockk<AnsvarligSaksbehandlerService>(relaxed = true),
        )

    private val behandlingId = UUID.randomUUID()

    init {
        every { repository.findByBehandlingId(any()) } returns emptyList()
        every { repository.insertAll(any()) } answers { firstArg() }
    }

    @Test
    fun `detaljene for en periode uten vurdering har bare type og periode`() {
        val detaljer = slot<String>()
        every { endringshistorikkService.registrerEndring(any(), any(), capture(detaljer)) } returns Unit

        service.lagrePeriode(
            behandlingId,
            TestPeriodeRequest(fraOgMedDato = LocalDate.of(2025, 1, 1), tilOgMedDato = LocalDate.of(2025, 6, 30)),
        )

        assertThat(detaljer.captured).isEqualTo("TESTPERIODE, Periode: 01.01.2025 – 30.06.2025")
        assertThat(detaljer.captured).doesNotContain(HEMMELIG_BEGRUNNELSE)
    }

    @Test
    fun `servicen skriver endringstypene den er konfigurert med`() {
        val lagret = service.lagrePeriode(behandlingId, TestPeriodeRequest())
        every { repository.findById(lagret.id) } returns Optional.of(lagret)

        service.lagrePeriode(behandlingId, TestPeriodeRequest(id = lagret.id))
        service.slettPeriode(behandlingId, lagret.id)

        verify { endringshistorikkService.registrerEndring(behandlingId, OPPRETTET, any()) }
        verify { endringshistorikkService.registrerEndring(behandlingId, OPPDATERT, any()) }
        verify { endringshistorikkService.registrerEndring(behandlingId, SLETTET, any()) }
    }

    @Test
    fun `periode uten vurdering kopieres alltid`() {
        val nyBehandlingId = UUID.fromString("00000000-0000-0000-0000-000000000002")
        every { repository.findByBehandlingId(behandlingId) } returns
            listOf(
                TestPeriode(
                    behandlingId = behandlingId,
                    begrunnelse = "Trygdetid 2020",
                    fraOgMedDato = LocalDate.of(2020, 1, 1),
                    tilOgMedDato = LocalDate.of(2020, 12, 31),
                ),
                TestPeriode(
                    behandlingId = behandlingId,
                    begrunnelse = "Trygdetid 2021",
                    fraOgMedDato = LocalDate.of(2021, 1, 1),
                    tilOgMedDato = LocalDate.of(2021, 12, 31),
                ),
            )

        val kopier = service.kopierPerioder(fraBehandlingId = behandlingId, tilBehandlingId = nyBehandlingId)

        assertThat(kopier).hasSize(2)
        assertThat(kopier.map { it.behandlingId }).containsOnly(nyBehandlingId)
    }

    private enum class TestType { TESTPERIODE }

    data class TestPeriode(
        override val id: UUID = UUID.randomUUID(),
        override val behandlingId: UUID,
        override val begrunnelse: String,
        override val fraOgMedDato: LocalDate?,
        override val tilOgMedDato: LocalDate?,
        override val sporbar: Sporbar = Sporbar(),
    ) : PeriodeMedBegrunnelse<TestPeriode> {
        override fun kopierTilBehandling(behandlingId: UUID) = copy(id = UUID.randomUUID(), behandlingId = behandlingId, sporbar = Sporbar())

        override fun kopierMedTidsrom(
            id: UUID,
            fraOgMedDato: LocalDate?,
            tilOgMedDato: LocalDate?,
        ) = copy(id = id, fraOgMedDato = fraOgMedDato, tilOgMedDato = tilOgMedDato)
    }

    data class TestPeriodeRequest(
        override val id: UUID? = null,
        override val begrunnelse: String = HEMMELIG_BEGRUNNELSE,
        override val fraOgMedDato: LocalDate? = null,
        override val tilOgMedDato: LocalDate? = null,
    ) : PeriodeMedBegrunnelseRequest

    private class TestPeriodeService(
        repository: PeriodeMedBegrunnelseRepository<TestPeriode>,
        behandlingService: BehandlingService,
        endringshistorikkService: EndringshistorikkService,
        ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
    ) : PeriodeMedBegrunnelseService<TestPeriode, TestPeriodeRequest>(
            repository,
            behandlingService,
            endringshistorikkService,
            ansvarligSaksbehandlerService,
            Endringstyper(opprettet = OPPRETTET, oppdatert = OPPDATERT, slettet = SLETTET),
        ) {
        override val periodebeskrivelse = "testperiode"
        override val type: Enum<*> = TestType.TESTPERIODE

        override fun nyPeriode(
            behandlingId: UUID,
            request: TestPeriodeRequest,
        ) = TestPeriode(
            behandlingId = behandlingId,
            begrunnelse = request.begrunnelse,
            fraOgMedDato = request.fraOgMedDato,
            tilOgMedDato = request.tilOgMedDato,
        )

        override fun TestPeriode.oppdaterMed(request: TestPeriodeRequest) = copy(begrunnelse = request.begrunnelse, fraOgMedDato = request.fraOgMedDato, tilOgMedDato = request.tilOgMedDato)
    }

    private companion object {
        const val HEMMELIG_BEGRUNNELSE = "Diabetes type 1"

        // Andre typer enn VILKÅR_VURDERING_*, slik at testen viser at servicen ikke har dem hardkodet.
        val OPPRETTET = EndringType.SENDT_TIL_BESLUTTER
        val OPPDATERT = EndringType.ANGRET_SEND_TIL_BESLUTTER
        val SLETTET = EndringType.VILKÅR_VURDERING_FULLFØRT
    }
}
