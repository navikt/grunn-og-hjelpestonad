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
import no.nav.grunn.og.hjelpestonad.vilkår.institusjon.Oppholdstype
import no.nav.grunn.og.hjelpestonad.vilkår.institusjon.Unntakshjemmel
import no.nav.grunn.og.hjelpestonad.vilkår.institusjon.VilkårInstitusjon
import no.nav.grunn.og.hjelpestonad.vilkår.institusjon.VilkårInstitusjonRepository
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.Regelverk
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskap
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskapRepository
import no.nav.grunn.og.hjelpestonad.vilkår.rett.PeriodeMedRett
import no.nav.grunn.og.hjelpestonad.vilkår.rett.PeriodeMedRettRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class VilkårRepositoryTest(
    private val vilkårMedlemskapRepository: VilkårMedlemskapRepository,
    private val vilkårDiagnoseRepository: VilkårDiagnoseRepository,
    private val vilkårInstitusjonRepository: VilkårInstitusjonRepository,
    private val periodeMedRettRepository: PeriodeMedRettRepository,
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
    fun `flere ulike diagnoser kan løpe samtidig på samme behandling`() {
        val behandlingId = opprettBehandling()

        vilkårDiagnoseRepository.insert(diagnose(behandlingId, "Diabetes type 1", fraOgMedDato = LocalDate.of(2025, 1, 1)))
        vilkårDiagnoseRepository.insert(
            diagnose(behandlingId, "Støyskade", erYrkesskade = true, fraOgMedDato = LocalDate.of(2025, 1, 1)),
        )

        val diagnoser = vilkårDiagnoseRepository.findByBehandlingId(behandlingId)
        assertThat(diagnoser).hasSize(2)
        assertThat(diagnoser.single { it.erYrkesskade }.fraOgMedDato).isEqualTo(LocalDate.of(2025, 1, 1))
    }

    @Test
    fun `databasen avviser diagnose uten innhold`() {
        val behandlingId = opprettBehandling()

        assertThatThrownBy { vilkårDiagnoseRepository.insert(diagnose(behandlingId, "   ")) }
            .hasStackTraceContaining("vilkar_diagnose_ikke_tom")
    }

    @Test
    fun `databasen avviser unntakshjemmel uten registrert opphold`() {
        val behandlingId = opprettBehandling()

        assertThatThrownBy {
            vilkårInstitusjonRepository.insert(
                VilkårInstitusjon(
                    behandlingId = behandlingId,
                    unntakshjemmel = Unntakshjemmel.KORTTIDSOPPHOLD,
                    vurdering = Vurdering.JA,
                ),
            )
        }.hasStackTraceContaining("vilkar_institusjon_unntak_krever_opphold")
    }

    @Test
    fun `databasen avviser til og med-dato før fra og med-dato`() {
        val behandlingId = opprettBehandling()

        assertThatThrownBy {
            vilkårInstitusjonRepository.insert(
                VilkårInstitusjon(
                    behandlingId = behandlingId,
                    oppholdstype = Oppholdstype.HELSE_OG_OMSORGSINSTITUSJON,
                    vurdering = Vurdering.NEI,
                    fraOgMedDato = LocalDate.of(2025, 6, 1),
                    tilOgMedDato = LocalDate.of(2025, 1, 1),
                ),
            )
        }.hasStackTraceContaining("vilkar_institusjon_datorekkefolge")
    }

    @Test
    fun `databasen tillater flere perioder for samme vilkår`() {
        val behandlingId = opprettBehandling()

        assertThatCode {
            vilkårInstitusjonRepository.insert(
                VilkårInstitusjon(
                    behandlingId = behandlingId,
                    oppholdstype = Oppholdstype.SPESIALISTHELSETJENESTEN,
                    vurdering = Vurdering.NEI,
                    fraOgMedDato = LocalDate.of(2025, 1, 1),
                    tilOgMedDato = LocalDate.of(2025, 6, 30),
                ),
            )
            vilkårInstitusjonRepository.insert(
                VilkårInstitusjon(behandlingId = behandlingId, vurdering = Vurdering.JA, fraOgMedDato = LocalDate.of(2025, 7, 1)),
            )
        }.doesNotThrowAnyException()

        assertThat(vilkårInstitusjonRepository.findByBehandlingId(behandlingId)).hasSize(2)
    }

    @Test
    fun `perioder med rett lagres og hentes på behandling`() {
        val behandlingId = opprettBehandling()

        periodeMedRettRepository.insertAll(
            listOf(
                PeriodeMedRett(behandlingId = behandlingId, fraOgMedDato = LocalDate.of(2025, 1, 1), tilOgMedDato = LocalDate.of(2025, 3, 31)),
                PeriodeMedRett(behandlingId = behandlingId, fraOgMedDato = LocalDate.of(2025, 6, 1)),
            ),
        )

        val lagrede = periodeMedRettRepository.findByBehandlingId(behandlingId)
        assertThat(lagrede).hasSize(2)
        assertThat(lagrede.map { it.tilOgMedDato }).containsExactlyInAnyOrder(LocalDate.of(2025, 3, 31), null)
    }

    @Test
    fun `deleteByBehandlingId sletter bare periodene til den aktuelle behandlingen`() {
        val behandlingId = opprettBehandling()
        val annenBehandlingId = opprettBehandling()
        periodeMedRettRepository.insert(PeriodeMedRett(behandlingId = behandlingId))
        periodeMedRettRepository.insert(PeriodeMedRett(behandlingId = annenBehandlingId))

        periodeMedRettRepository.deleteByBehandlingId(behandlingId)

        assertThat(periodeMedRettRepository.findByBehandlingId(behandlingId)).isEmpty()
        assertThat(periodeMedRettRepository.findByBehandlingId(annenBehandlingId)).hasSize(1)
    }

    @Test
    fun `databasen avviser periode med rett der til og med-dato er før fra og med-dato`() {
        val behandlingId = opprettBehandling()

        assertThatThrownBy {
            periodeMedRettRepository.insert(
                PeriodeMedRett(
                    behandlingId = behandlingId,
                    fraOgMedDato = LocalDate.of(2025, 6, 1),
                    tilOgMedDato = LocalDate.of(2025, 1, 1),
                ),
            )
        }.hasStackTraceContaining("periode_med_rett_datorekkefolge")
    }

    @Test
    fun `sletting av behandlingen sletter vilkårsperiodene`() {
        val behandlingId = opprettBehandling()
        vilkårMedlemskapRepository.insert(
            VilkårMedlemskap(behandlingId = behandlingId, regelverk = Regelverk.NASJONALE_REGLER, vurdering = Vurdering.JA),
        )
        vilkårDiagnoseRepository.insert(diagnose(behandlingId, "Diabetes type 1"))
        vilkårInstitusjonRepository.insert(VilkårInstitusjon(behandlingId = behandlingId, vurdering = Vurdering.JA))
        periodeMedRettRepository.insert(PeriodeMedRett(behandlingId = behandlingId))

        behandlingRepository.deleteById(behandlingId)

        assertThat(vilkårMedlemskapRepository.findByBehandlingId(behandlingId)).isEmpty()
        assertThat(vilkårDiagnoseRepository.findByBehandlingId(behandlingId)).isEmpty()
        assertThat(vilkårInstitusjonRepository.findByBehandlingId(behandlingId)).isEmpty()
        assertThat(periodeMedRettRepository.findByBehandlingId(behandlingId)).isEmpty()
    }

    private fun diagnose(
        behandlingId: UUID,
        diagnose: String,
        erYrkesskade: Boolean = false,
        fraOgMedDato: LocalDate? = null,
    ) = VilkårDiagnose(
        behandlingId = behandlingId,
        diagnose = diagnose,
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
