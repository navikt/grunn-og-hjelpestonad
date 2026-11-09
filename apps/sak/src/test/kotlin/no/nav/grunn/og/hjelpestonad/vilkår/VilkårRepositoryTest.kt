package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.grunn.og.hjelpestonad.SpringContextTest
import no.nav.grunn.og.hjelpestonad.behandling.Behandling
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingRepository
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingResultat
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingStatus
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakPersonRepository
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakRepository
import no.nav.grunn.og.hjelpestonad.fagsak.domain.Fagsak
import no.nav.grunn.og.hjelpestonad.fagsak.domain.FagsakPerson
import no.nav.grunn.og.hjelpestonad.fagsak.domain.Personident
import no.nav.grunn.og.hjelpestonad.fagsak.domain.StønadType
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnose
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnoseRepository
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.Regelverk
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskap
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskapRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

class VilkårRepositoryTest(
    private val vilkårMedlemskapRepository: VilkårMedlemskapRepository,
    private val vilkårDiagnoseRepository: VilkårDiagnoseRepository,
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val fagsakPersonRepository: FagsakPersonRepository,
) : SpringContextTest() {
    @Test
    fun `medlemskap lagres og hentes på behandling`() {
        val behandlingId = opprettBehandling()

        vilkårMedlemskapRepository.insert(
            VilkårMedlemskap(
                behandlingId = behandlingId,
                regelverk = Regelverk.EØS_FORORDNINGEN,
                vurdering = Vurdering.JA,
                begrunnelse = "Omfattet av trygdeforordningen",
                fraOgMedDato = LocalDate.of(2025, 1, 1),
            ),
        )

        val lagret = vilkårMedlemskapRepository.findByBehandlingId(behandlingId)
        assertThat(lagret).hasSize(1)
        assertThat(lagret.single().regelverk).isEqualTo(Regelverk.EØS_FORORDNINGEN)
    }

    @Test
    fun `databasen avviser til og med-dato før fra og med-dato`() {
        val behandlingId = opprettBehandling()

        assertThatThrownBy {
            vilkårMedlemskapRepository.insert(
                VilkårMedlemskap(
                    behandlingId = behandlingId,
                    regelverk = Regelverk.NASJONALE_REGLER,
                    vurdering = Vurdering.JA,
                    fraOgMedDato = LocalDate.of(2025, 6, 1),
                    tilOgMedDato = LocalDate.of(2025, 1, 1),
                ),
            )
        }.hasStackTraceContaining("vilkar_medlemskap_datorekkefolge")
    }

    @Test
    fun `databasen tillater flere perioder for samme vilkår`() {
        val behandlingId = opprettBehandling()

        assertThatCode {
            vilkårMedlemskapRepository.insert(
                VilkårMedlemskap(
                    behandlingId = behandlingId,
                    regelverk = Regelverk.NASJONALE_REGLER,
                    vurdering = Vurdering.NEI,
                    fraOgMedDato = LocalDate.of(2025, 1, 1),
                    tilOgMedDato = LocalDate.of(2025, 6, 30),
                ),
            )
            vilkårMedlemskapRepository.insert(
                VilkårMedlemskap(
                    behandlingId = behandlingId,
                    regelverk = Regelverk.NASJONALE_REGLER,
                    vurdering = Vurdering.JA,
                    fraOgMedDato = LocalDate.of(2025, 7, 1),
                ),
            )
        }.doesNotThrowAnyException()

        assertThat(vilkårMedlemskapRepository.findByBehandlingId(behandlingId)).hasSize(2)
    }

    @Test
    fun `flere ulike diagnoser kan løpe samtidig på samme behandling`() {
        val behandlingId = opprettBehandling()

        vilkårDiagnoseRepository.insert(diagnose(behandlingId, "E109", fraOgMedDato = LocalDate.of(2025, 1, 1)))
        vilkårDiagnoseRepository.insert(
            diagnose(behandlingId, "H833", erYrkesskade = true, fraOgMedDato = LocalDate.of(2025, 1, 1)),
        )

        val diagnoser = vilkårDiagnoseRepository.findByBehandlingId(behandlingId)
        assertThat(diagnoser).hasSize(2)
        assertThat(diagnoser.single { it.erYrkesskade }.fraOgMedDato).isEqualTo(LocalDate.of(2025, 1, 1))
    }

    @Test
    fun `deleteByBehandlingId sletter bare diagnosene til den aktuelle behandlingen`() {
        val behandlingId = opprettBehandling()
        val annenBehandlingId = opprettBehandling()
        vilkårDiagnoseRepository.insert(diagnose(behandlingId, "E109"))
        vilkårDiagnoseRepository.insert(diagnose(annenBehandlingId, "H833"))

        vilkårDiagnoseRepository.deleteByBehandlingId(behandlingId)

        assertThat(vilkårDiagnoseRepository.findByBehandlingId(behandlingId)).isEmpty()
        assertThat(vilkårDiagnoseRepository.findByBehandlingId(annenBehandlingId)).hasSize(1)
    }

    @Test
    @Transactional
    fun `kan sette inn periode med samme id etter sletting i samme transaksjon`() {
        val behandlingId = opprettBehandling()
        val lagret = diagnose(behandlingId, "H833")
        vilkårDiagnoseRepository.insert(lagret)

        assertThatCode {
            vilkårDiagnoseRepository.deleteByBehandlingId(behandlingId)
            vilkårDiagnoseRepository.insert(lagret)
        }.doesNotThrowAnyException()

        assertThat(vilkårDiagnoseRepository.findByBehandlingId(behandlingId)).containsExactly(lagret)
    }

    @Test
    fun `lagrer kode og tekst for diagnosen`() {
        val behandlingId = opprettBehandling()

        vilkårDiagnoseRepository.insert(diagnose(behandlingId, "K900"))

        val diagnose = vilkårDiagnoseRepository.findByBehandlingId(behandlingId).single()
        assertThat(diagnose.kode).isEqualTo("K900")
        assertThat(diagnose.tekst).isEqualTo("Tekst fra kodeverket")
    }

    @Test
    fun `databasen avviser diagnose uten kode`() {
        val behandlingId = opprettBehandling()

        assertThatThrownBy { vilkårDiagnoseRepository.insert(diagnose(behandlingId, "   ")) }
            .hasStackTraceContaining("vilkar_diagnose_kode_ikke_tom")
    }

    @Test
    fun `sletting av behandlingen sletter vilkårsperiodene`() {
        val behandlingId = opprettBehandling()
        vilkårMedlemskapRepository.insert(
            VilkårMedlemskap(behandlingId = behandlingId, regelverk = Regelverk.NASJONALE_REGLER, vurdering = Vurdering.JA),
        )
        vilkårDiagnoseRepository.insert(diagnose(behandlingId, "E109"))

        behandlingRepository.deleteById(behandlingId)

        assertThat(vilkårMedlemskapRepository.findByBehandlingId(behandlingId)).isEmpty()
        assertThat(vilkårDiagnoseRepository.findByBehandlingId(behandlingId)).isEmpty()
    }

    private fun diagnose(
        behandlingId: UUID,
        kode: String,
        erYrkesskade: Boolean = false,
        fraOgMedDato: LocalDate? = null,
    ) = VilkårDiagnose(
        behandlingId = behandlingId,
        kode = kode,
        tekst = "Tekst fra kodeverket",
        erYrkesskade = erYrkesskade,
        vurdering = Vurdering.JA,
        begrunnelse = "Test",
        fraOgMedDato = fraOgMedDato,
    )

    private fun opprettBehandling(): UUID {
        val fagsakPerson = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(Personident(tilfeldigIdent()))))
        val fagsak = fagsakRepository.insert(Fagsak(fagsakPersonId = fagsakPerson.id, stønadstype = StønadType.BARNETILSYN))
        return behandlingRepository
            .insert(Behandling(fagsakId = fagsak.id, status = BehandlingStatus.UTREDES, resultat = BehandlingResultat.IKKE_SATT))
            .id
    }

    private fun tilfeldigIdent(): String = (10_000_000_000L..99_999_999_999L).random().toString()
}
