package no.nav.grunn.og.hjelpestonad.vilkår

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnose
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnoseService
import no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter.Ekstrautgift
import no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter.EkstrautgifterRegler
import no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter.Hjemmelbokstav
import no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter.Sats
import no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter.VilkårEkstrautgifter
import no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter.VilkårEkstrautgifterRepository
import no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter.VilkårEkstrautgifterRequest
import no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter.VilkårEkstrautgifterService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.time.LocalDate
import java.util.Optional
import java.util.UUID
import kotlin.test.Test

private const val MS = "G35"

class VilkårEkstrautgifterServiceTest {
    private val repository = mockk<VilkårEkstrautgifterRepository>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val endringshistorikkService = mockk<EndringshistorikkService>(relaxed = true)
    private val ansvarligSaksbehandlerService = mockk<AnsvarligSaksbehandlerService>(relaxed = true)
    private val vilkårDiagnoseService = mockk<VilkårDiagnoseService>()
    private val service =
        VilkårEkstrautgifterService(
            repository,
            behandlingService,
            endringshistorikkService,
            ansvarligSaksbehandlerService,
            vilkårDiagnoseService,
        )

    private val behandlingId = UUID.randomUUID()
    private val annenBehandlingId = UUID.randomUUID()

    init {
        every { repository.insertAll(any()) } answers { firstArg() }
        every { repository.findByBehandlingId(any()) } returns emptyList()
        every { vilkårDiagnoseService.hentPerioder(behandlingId) } returns listOf(diagnose(behandlingId))
        every { vilkårDiagnoseService.hentPerioder(annenBehandlingId) } returns listOf(diagnose(annenBehandlingId, kode = "K900"))
    }

    private fun diagnose(
        behandlingId: UUID,
        kode: String = MS,
    ) = VilkårDiagnose(behandlingId = behandlingId, kode = kode, tekst = "Tekst", vurdering = Vurdering.JA)

    private fun request(
        id: UUID? = null,
        vurdering: Vurdering = Vurdering.JA,
        sats: Sats? = Sats.SATS_2,
        bokstaver: Set<Hjemmelbokstav> = setOf(Hjemmelbokstav.B, Hjemmelbokstav.F),
        diagnosekoder: Set<String> = setOf(MS),
        fraOgMedDato: LocalDate? = LocalDate.of(2025, 11, 1),
        tilOgMedDato: LocalDate? = null,
    ) = VilkårEkstrautgifterRequest(
        id = id,
        vurdering = vurdering,
        sats = sats,
        ekstrautgifter = bokstaver.map { Ekstrautgift(it, diagnosekoder) },
        begrunnelse = "Økte utgifter til bil",
        fraOgMedDato = fraOgMedDato,
        tilOgMedDato = tilOgMedDato,
    )

    private fun lagret(
        bokstaver: Set<Hjemmelbokstav>,
        vurdering: Vurdering = Vurdering.JA,
        fraOgMedDato: LocalDate = LocalDate.of(2025, 11, 1),
        tilOgMedDato: LocalDate? = null,
    ) = VilkårEkstrautgifter(
        behandlingId = behandlingId,
        vurdering = vurdering,
        sats = if (vurdering == Vurdering.JA) Sats.SATS_1 else null,
        fraOgMedDato = fraOgMedDato,
        tilOgMedDato = tilOgMedDato,
    ).medEkstrautgifter(bokstaver.map { Ekstrautgift(it, setOf(MS)) })

    @Test
    fun `lagrer sats og ekstrautgifter med egne diagnoser per hjemmelbokstav`() {
        every { vilkårDiagnoseService.hentPerioder(behandlingId) } returns listOf(diagnose(behandlingId), diagnose(behandlingId, "K900"))
        val ekstrautgifter = listOf(Ekstrautgift(Hjemmelbokstav.B, setOf(MS)), Ekstrautgift(Hjemmelbokstav.F, setOf("K900", MS)))

        val resultat = service.lagrePeriode(behandlingId, request().copy(ekstrautgifter = ekstrautgifter))

        assertThat(resultat.behandlingId).isEqualTo(behandlingId)
        assertThat(resultat.sats).isEqualTo(Sats.SATS_2)
        assertThat(resultat.ekstrautgifter).isEqualTo(ekstrautgifter)
        verify(exactly = 1) { repository.insertAll(any()) }
    }

    @Test
    fun `avviser en ekstrautgift uten diagnose før noe skrives`() {
        assertThatThrownBy {
            service.lagrePeriode(behandlingId, request().copy(ekstrautgifter = listOf(Ekstrautgift(Hjemmelbokstav.B, emptySet()))))
        }.isInstanceOf(UgyldigInput::class.java)
            .hasMessage(EkstrautgifterRegler.DIAGNOSE_MANGLER)

        verify(exactly = 0) { repository.insertAll(any()) }
    }

    @Test
    fun `lagrePeriode avviser begrunnelse over 2000 tegn før noe skrives`() {
        assertThatThrownBy { service.lagrePeriode(behandlingId, request().copy(begrunnelse = "a".repeat(2001))) }
            .isInstanceOf(UgyldigInput::class.java)

        verify(exactly = 0) { repository.deleteByBehandlingId(any()) }
        verify(exactly = 0) { repository.insertAll(any()) }
        verify(exactly = 0) { endringshistorikkService.registrerEndring(any(), any(), any()) }
    }

    @Test
    fun `endringshistorikken viser satsen, men ikke diagnosen eller begrunnelsen`() {
        val detaljer = slot<String>()
        every { endringshistorikkService.registrerEndring(any(), any(), capture(detaljer)) } returns Unit

        service.lagrePeriode(behandlingId, request(tilOgMedDato = LocalDate.of(2026, 6, 30)))

        assertThat(detaljer.captured).isEqualTo("${VilkårType.NØDVENDIGE_EKSTRAUTGIFTER}, Sats: 2, Periode: 01.11.2025 – 30.06.2026")
        assertThat(detaljer.captured).doesNotContain(MS).doesNotContain("bil")
    }

    @Test
    fun `endringshistorikken for en periode som ikke er oppfylt, har verken sats eller hjemmelbokstaver`() {
        val detaljer = slot<String>()
        every { endringshistorikkService.registrerEndring(any(), any(), capture(detaljer)) } returns Unit

        service.lagrePeriode(
            behandlingId,
            request(vurdering = Vurdering.NEI, sats = null, bokstaver = emptySet(), diagnosekoder = emptySet()),
        )

        assertThat(detaljer.captured).isEqualTo("${VilkårType.NØDVENDIGE_EKSTRAUTGIFTER}, Vurdering: ${Vurdering.NEI}, Periode: 01.11.2025 – løpende")
    }

    @Test
    fun `godtar ikke en diagnose som bare er oppfylt på en annen behandling`() {
        assertThatThrownBy { service.lagrePeriode(behandlingId, request(diagnosekoder = setOf("K900"))) }
            .isInstanceOf(UgyldigInput::class.java)
            .hasMessage(EkstrautgifterRegler.DIAGNOSE_IKKE_OPPFYLT_I_HELE_PERIODEN)

        verify(exactly = 0) { vilkårDiagnoseService.hentPerioder(annenBehandlingId) }
        verify(exactly = 0) { repository.insertAll(any()) }
    }

    @Test
    fun `beholder bokstav d når den lagrede perioden har den`() {
        val eksisterende = lagret(setOf(Hjemmelbokstav.D))
        every { repository.findById(eksisterende.id) } returns Optional.of(eksisterende)
        every { repository.findByBehandlingId(behandlingId) } returns listOf(eksisterende)

        val resultat = service.lagrePeriode(behandlingId, request(id = eksisterende.id, bokstaver = setOf(Hjemmelbokstav.D)))

        assertThat(resultat.bokstaver).containsExactly(Hjemmelbokstav.D)
    }

    @Test
    fun `avviser bokstav d i en ny periode før noe skrives`() {
        assertThatThrownBy { service.lagrePeriode(behandlingId, request(bokstaver = setOf(Hjemmelbokstav.D))) }
            .isInstanceOf(UgyldigInput::class.java)
            .hasMessage(EkstrautgifterRegler.BOKSTAV_GJELDER_IKKE_NYE_TILFELLER)

        verify(exactly = 0) { repository.deleteByBehandlingId(any()) }
        verify(exactly = 0) { repository.insertAll(any()) }
    }

    @Test
    fun `avviser en periode-id fra en annen behandling uten å skrive`() {
        val fraAnnenBehandling = lagret(setOf(Hjemmelbokstav.B)).copy(behandlingId = annenBehandlingId)
        every { repository.findById(fraAnnenBehandling.id) } returns Optional.of(fraAnnenBehandling)

        assertThatThrownBy { service.lagrePeriode(behandlingId, request(id = fraAnnenBehandling.id)) }
            .isInstanceOf(Feil::class.java)

        verify(exactly = 0) { repository.deleteByBehandlingId(any()) }
        verify(exactly = 0) { repository.insertAll(any()) }
    }

    @Test
    fun `forkorter en overlappende periode, fordi ekstrautgiftene har én tidslinje`() {
        val eksisterende = lagret(setOf(Hjemmelbokstav.B), tilOgMedDato = LocalDate.of(2026, 12, 31))
        every { repository.findByBehandlingId(behandlingId) } returns listOf(eksisterende)
        val innlagte = slot<List<VilkårEkstrautgifter>>()
        every { repository.insertAll(capture(innlagte)) } answers { firstArg() }

        val ny = service.lagrePeriode(behandlingId, request(fraOgMedDato = LocalDate.of(2026, 7, 1)))

        assertThat(innlagte.captured).hasSize(2)
        val forkortet = innlagte.captured.single { it.id != ny.id }
        assertThat(forkortet.fraOgMedDato).isEqualTo(eksisterende.fraOgMedDato)
        assertThat(forkortet.tilOgMedDato).isEqualTo(LocalDate.of(2026, 6, 30))
        assertThat(forkortet.bokstaver).containsExactly(Hjemmelbokstav.B)
    }

    @Test
    fun `kopierer bare perioder som er oppfylt, med ekstrautgiftene`() {
        val oppfylt = lagret(setOf(Hjemmelbokstav.B), tilOgMedDato = LocalDate.of(2026, 6, 30))
        val ikkeOppfylt = lagret(emptySet(), vurdering = Vurdering.NEI, fraOgMedDato = LocalDate.of(2026, 7, 1))
        every { repository.findByBehandlingId(behandlingId) } returns listOf(oppfylt, ikkeOppfylt)

        val kopier = service.kopierPerioder(fraBehandlingId = behandlingId, tilBehandlingId = annenBehandlingId)

        assertThat(kopier).hasSize(1)
        assertThat(kopier.single().behandlingId).isEqualTo(annenBehandlingId)
        assertThat(kopier.single().id).isNotEqualTo(oppfylt.id)
        assertThat(kopier.single().ekstrautgifter).containsExactly(Ekstrautgift(Hjemmelbokstav.B, setOf(MS)))
    }
}
