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
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

/**
 * Verifiserer at V34 faktisk håndhever invariantene i databasen, ikke bare i servicelaget.
 */
class VilkårDiagnoseRepositoryTest : SpringContextTest() {
    @Autowired
    private lateinit var vilkårVurderingRepository: VilkårVurderingRepository

    @Autowired
    private lateinit var vilkårDiagnoseRepository: VilkårDiagnoseRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var fagsakPersonRepository: FagsakPersonRepository

    @Test
    fun `diagnose kan lagres på en vurdering av varig sykdom`() {
        val vurdering = lagVilkårVurdering(VilkårType.VARIG_SYKDOM_SKADE_ELLER_LYTE)

        vilkårDiagnoseRepository.insert(VilkårDiagnose(vilkårVurderingId = vurdering.id, diagnose = "Diabetes type 1"))
        vilkårDiagnoseRepository.insert(
            VilkårDiagnose(
                vilkårVurderingId = vurdering.id,
                diagnose = "Støyskade",
                yrkesskade = true,
                yrkesskadeDato = LocalDate.of(2020, 3, 1),
            ),
        )

        val diagnoser = vilkårDiagnoseRepository.findByVilkårVurderingId(vurdering.id)
        assertThat(diagnoser).hasSize(2)
        assertThat(diagnoser.single { it.yrkesskade }.yrkesskadeDato).isEqualTo(LocalDate.of(2020, 3, 1))
    }

    @Test
    fun `databasen avviser diagnose på nødvendige ekstrautgifter`() {
        val vurdering = lagVilkårVurdering(VilkårType.NØDVENDIGE_EKSTRAUTGIFTER)

        assertThatThrownBy {
            vilkårDiagnoseRepository.insert(VilkårDiagnose(vilkårVurderingId = vurdering.id, diagnose = "Diabetes type 1"))
        }.hasStackTraceContaining("vilkar_diagnose_vilkar_vurdering_fk")
    }

    @Test
    fun `databasen avviser diagnose på institusjonsvilkåret`() {
        val vurdering = lagVilkårVurdering(VilkårType.IKKE_OPPHOLD_I_INSTITUSJON_ELLER_LOVREGULERT_BOFORM)

        assertThatThrownBy {
            vilkårDiagnoseRepository.insert(VilkårDiagnose(vilkårVurderingId = vurdering.id, diagnose = "Diabetes type 1"))
        }.hasStackTraceContaining("vilkar_diagnose_vilkar_vurdering_fk")
    }

    @Test
    fun `sletting av vilkårsperioden sletter diagnosene`() {
        val vurdering = lagVilkårVurdering(VilkårType.VARIG_SYKDOM_SKADE_ELLER_LYTE)
        vilkårDiagnoseRepository.insert(VilkårDiagnose(vilkårVurderingId = vurdering.id, diagnose = "Diabetes type 1"))

        vilkårVurderingRepository.deleteById(vurdering.id)

        assertThat(vilkårDiagnoseRepository.findByVilkårVurderingId(vurdering.id)).isEmpty()
    }

    @Test
    fun `databasen tillater flere perioder for samme vilkårstype`() {
        val behandlingId = opprettBehandling()

        assertThatCode {
            vilkårVurderingRepository.insert(
                lagVurdering(behandlingId, VilkårType.VARIG_SYKDOM_SKADE_ELLER_LYTE, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 6, 30)),
            )
            vilkårVurderingRepository.insert(
                lagVurdering(behandlingId, VilkårType.VARIG_SYKDOM_SKADE_ELLER_LYTE, LocalDate.of(2025, 7, 1), null),
            )
        }.doesNotThrowAnyException()

        assertThat(
            vilkårVurderingRepository.findByBehandlingIdAndVilkårType(behandlingId, VilkårType.VARIG_SYKDOM_SKADE_ELLER_LYTE),
        ).hasSize(2)
    }

    @Test
    fun `institusjonsvilkårets navn får plass i vilkar_type`() {
        val vurdering = lagVilkårVurdering(VilkårType.IKKE_OPPHOLD_I_INSTITUSJON_ELLER_LOVREGULERT_BOFORM)

        val lagret = vilkårVurderingRepository.findByBehandlingIdAndVilkårType(vurdering.behandlingId, vurdering.vilkårType)

        assertThat(lagret).hasSize(1)
        assertThat(lagret.single().vilkårType).isEqualTo(VilkårType.IKKE_OPPHOLD_I_INSTITUSJON_ELLER_LOVREGULERT_BOFORM)
    }

    private fun lagVilkårVurdering(vilkårType: VilkårType): VilkårVurdering = vilkårVurderingRepository.insert(lagVurdering(opprettBehandling(), vilkårType))

    private fun lagVurdering(
        behandlingId: UUID,
        vilkårType: VilkårType,
        fraOgMedDato: LocalDate? = null,
        tilOgMedDato: LocalDate? = null,
    ) = VilkårVurdering(
        behandlingId = behandlingId,
        vilkårType = vilkårType,
        vurdering = Vurdering.JA,
        begrunnelse = "Test",
        fraOgMedDato = fraOgMedDato,
        tilOgMedDato = tilOgMedDato,
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
