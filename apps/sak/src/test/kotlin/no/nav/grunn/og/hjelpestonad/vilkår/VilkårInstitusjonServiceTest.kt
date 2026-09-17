package no.nav.grunn.og.hjelpestonad.vilkår

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.vilkår.institusjon.Oppholdstype
import no.nav.grunn.og.hjelpestonad.vilkår.institusjon.Unntakshjemmel
import no.nav.grunn.og.hjelpestonad.vilkår.institusjon.VilkårInstitusjonRepository
import no.nav.grunn.og.hjelpestonad.vilkår.institusjon.VilkårInstitusjonRequest
import no.nav.grunn.og.hjelpestonad.vilkår.institusjon.VilkårInstitusjonService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.springframework.http.HttpStatus
import java.util.UUID
import kotlin.test.Test

class VilkårInstitusjonServiceTest {
    private val repository = mockk<VilkårInstitusjonRepository>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val endringshistorikkService = mockk<EndringshistorikkService>(relaxed = true)
    private val ansvarligSaksbehandlerService = mockk<AnsvarligSaksbehandlerService>(relaxed = true)
    private val service = VilkårInstitusjonService(repository, behandlingService, endringshistorikkService, ansvarligSaksbehandlerService)

    private val behandlingId = UUID.randomUUID()

    init {
        every { repository.insert(any()) } answers { firstArg() }
        every { repository.update(any()) } answers { firstArg() }
        every { repository.findByBehandlingId(any()) } returns emptyList()
    }

    private fun request(
        oppholdstype: Oppholdstype? = null,
        unntakshjemmel: Unntakshjemmel? = null,
        vurdering: Vurdering = Vurdering.JA,
    ) = VilkårInstitusjonRequest(
        oppholdstype = oppholdstype,
        unntakshjemmel = unntakshjemmel,
        vurdering = vurdering,
        begrunnelse = "Test",
    )

    @Test
    fun `lagrePeriode godtar periode uten opphold i institusjon`() {
        val resultat = service.lagrePeriode(behandlingId, request())

        assertThat(resultat.oppholdstype).isNull()
        assertThat(resultat.unntakshjemmel).isNull()
        assertThat(resultat.erVilkårOppfylt()).isTrue()
    }

    @Test
    fun `lagrePeriode godtar unntak når det foreligger et opphold`() {
        val resultat =
            service.lagrePeriode(
                behandlingId,
                request(
                    oppholdstype = Oppholdstype.SPESIALISTHELSETJENESTEN,
                    unntakshjemmel = Unntakshjemmel.KORTTIDSOPPHOLD,
                ),
            )

        assertThat(resultat.oppholdstype).isEqualTo(Oppholdstype.SPESIALISTHELSETJENESTEN)
        assertThat(resultat.unntakshjemmel).isEqualTo(Unntakshjemmel.KORTTIDSOPPHOLD)
    }

    @Test
    fun `lagrePeriode avviser unntakshjemmel uten opphold`() {
        assertThatThrownBy {
            service.lagrePeriode(behandlingId, request(unntakshjemmel = Unntakshjemmel.KORTTIDSOPPHOLD))
        }.isInstanceOf(Feil::class.java)
            .hasMessageContaining("uten at det er registrert et opphold")
            .extracting { (it as Feil).httpStatus }
            .isEqualTo(HttpStatus.BAD_REQUEST)

        verify(exactly = 0) { repository.insert(any()) }
    }

    @Test
    fun `lagrePeriode beholder hjemmelen for ekstrautgifter institusjonen ikke dekker`() {
        val resultat =
            service.lagrePeriode(
                behandlingId,
                request(
                    oppholdstype = Oppholdstype.PSYKISK_HELSEVERN,
                    unntakshjemmel = Unntakshjemmel.EKSTRAUTGIFTER_IKKE_DEKKET_AV_INSTITUSJONEN,
                ),
            )

        assertThat(resultat.unntakshjemmel).isEqualTo(Unntakshjemmel.EKSTRAUTGIFTER_IKKE_DEKKET_AV_INSTITUSJONEN)
    }

    @Test
    fun `vurdering NEI betyr at stønaden faller bort etter paragraf 6-8`() {
        val resultat =
            service.lagrePeriode(
                behandlingId,
                request(oppholdstype = Oppholdstype.HELSE_OG_OMSORGSINSTITUSJON, vurdering = Vurdering.NEI),
            )

        assertThat(resultat.erVilkårOppfylt()).isFalse()
    }
}
