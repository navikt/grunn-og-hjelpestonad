package no.nav.grunn.og.hjelpestonad.vilkår

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnose
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnoseRepository
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnoseRequest
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnoseService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.springframework.http.HttpStatus
import java.util.Optional
import java.util.UUID
import kotlin.test.Test

/**
 * Tilgangssjekken i controllerne gjelder behandlingId. Disse testene sikrer at en vilkårsperiode
 * bare kan endres via behandlingen den tilhører, slik at en saksbehandler ikke kan nå perioder på
 * andre personers behandlinger med en behandlingId hen har tilgang til. Se ADR-0009.
 */
class VilkårPeriodeBindingTest {
    private val repository = mockk<VilkårDiagnoseRepository>(relaxed = true)
    private val service =
        VilkårDiagnoseService(
            repository,
            mockk<BehandlingService>(relaxed = true),
            mockk<EndringshistorikkService>(relaxed = true),
            mockk<AnsvarligSaksbehandlerService>(relaxed = true),
        )

    private val egenBehandlingId = UUID.randomUUID()
    private val fremmedBehandlingId = UUID.randomUUID()
    private val fremmedPeriode =
        VilkårDiagnose(
            behandlingId = fremmedBehandlingId,
            kode = "E109",
            tekst = "Tekst fra kodeverket",
            vurdering = Vurdering.JA,
            begrunnelse = "Fremmed periode",
            fraOgMedDato = null,
            tilOgMedDato = null,
        )

    init {
        every { repository.findById(fremmedPeriode.id) } returns Optional.of(fremmedPeriode)
        every { repository.findByBehandlingId(any()) } returns emptyList()
    }

    @Test
    fun `kan ikke slette en periode via en annen behandling`() {
        assertThatThrownBy { service.slettPeriode(egenBehandlingId, fremmedPeriode.id) }
            .isInstanceOfSatisfying(Feil::class.java) { assertThat(it.httpStatus).isEqualTo(HttpStatus.NOT_FOUND) }

        verify(exactly = 0) { repository.deleteById(any()) }
    }

    @Test
    fun `kan ikke oppdatere en periode via en annen behandling`() {
        val request =
            VilkårDiagnoseRequest(
                id = fremmedPeriode.id,
                kode = "E109",
                erYrkesskade = false,
                vurdering = Vurdering.NEI,
                begrunnelse = "Forsøk på endring",
                fraOgMedDato = null,
                tilOgMedDato = null,
            )

        assertThatThrownBy { service.lagrePeriode(egenBehandlingId, request) }
            .isInstanceOfSatisfying(Feil::class.java) { assertThat(it.httpStatus).isEqualTo(HttpStatus.NOT_FOUND) }

        verify(exactly = 0) { repository.update(any()) }
    }
}
