package no.nav.grunn.og.hjelpestonad.trygdetid

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringType
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.ApiExceptionHandler
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.util.Optional
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFailsWith

class TrygdetidServiceTest {
    private val repository = mockk<TrygdetidRepository>(relaxed = true)
    private val endringshistorikkService = mockk<EndringshistorikkService>(relaxed = true)
    private val service =
        TrygdetidService(
            repository,
            mockk<BehandlingService>(relaxed = true),
            endringshistorikkService,
            mockk<AnsvarligSaksbehandlerService>(relaxed = true),
        )

    private val behandlingId = UUID.randomUUID()

    init {
        every { repository.insertAll(any()) } answers { firstArg() }
        every { repository.findByBehandlingId(any()) } returns emptyList()
    }

    @Test
    fun `lagrePeriode uten id lagrer ny trygdetid med teller og kilde`() {
        val resultat = service.lagrePeriode(behandlingId, request(teller = 25, kilde = TrygdetidKilde.UFØRETRYGD))

        assertThat(resultat.behandlingId).isEqualTo(behandlingId)
        assertThat(resultat.teller).isEqualTo(25)
        assertThat(resultat.kilde).isEqualTo(TrygdetidKilde.UFØRETRYGD)
    }

    @Test
    fun `teller -1 avvises med HTTP 400 og uten skriving`() {
        avvisesUtenSkriving(request(teller = -1))
    }

    @Test
    fun `teller 41 avvises med HTTP 400 og uten skriving`() {
        avvisesUtenSkriving(request(teller = 41))
    }

    @Test
    fun `kilde INGEN med teller 39 avvises med HTTP 400 og uten skriving`() {
        avvisesUtenSkriving(request(teller = 39, kilde = TrygdetidKilde.INGEN))
    }

    @Test
    fun `teller 0 og 40 er gyldige grenser for alderspensjon og uføretrygd`() {
        listOf(0, 40).forEach { teller ->
            listOf(TrygdetidKilde.ALDERSPENSJON, TrygdetidKilde.UFØRETRYGD).forEach { kilde ->
                val resultat = service.lagrePeriode(behandlingId, request(teller = teller, kilde = kilde))

                assertThat(resultat.teller).isEqualTo(teller)
                assertThat(resultat.kilde).isEqualTo(kilde)
            }
        }
    }

    @Test
    fun `kilde INGEN med teller 40 er gyldig`() {
        val resultat = service.lagrePeriode(behandlingId, request(teller = 40, kilde = TrygdetidKilde.INGEN))

        assertThat(resultat.teller).isEqualTo(40)
        assertThat(resultat.kilde).isEqualTo(TrygdetidKilde.INGEN)
    }

    @Test
    fun `opprett, oppdater og slett bruker trygdetidens egne endringstyper`() {
        val lagret = service.lagrePeriode(behandlingId, request())
        every { repository.findById(lagret.id) } returns Optional.of(lagret)

        service.lagrePeriode(behandlingId, request(id = lagret.id))
        service.slettPeriode(behandlingId, lagret.id)

        verify(exactly = 1) { endringshistorikkService.registrerEndring(behandlingId, EndringType.TRYGDETID_OPPRETTET, any()) }
        verify(exactly = 1) { endringshistorikkService.registrerEndring(behandlingId, EndringType.TRYGDETID_OPPDATERT, any()) }
        verify(exactly = 1) { endringshistorikkService.registrerEndring(behandlingId, EndringType.TRYGDETID_SLETTET, any()) }
    }

    @Test
    fun `historikken har bare type og periode for trygdetid`() {
        val detaljer = slot<String>()
        every { endringshistorikkService.registrerEndring(any(), any(), capture(detaljer)) } returns Unit

        service.lagrePeriode(
            behandlingId,
            request(
                teller = 25,
                kilde = TrygdetidKilde.UFØRETRYGD,
                fraOgMedDato = LocalDate.of(2025, 1, 1),
                tilOgMedDato = LocalDate.of(2025, 6, 30),
            ),
        )

        assertThat(detaljer.captured).isEqualTo("TRYGDETID, Periode: 01.01.2025 – 30.06.2025")
    }

    @Test
    fun `lagrePeriode med id fra annen behandling gir 404 uten skriving`() {
        val annen = trygdetid(behandlingId = UUID.randomUUID())
        every { repository.findById(annen.id) } returns Optional.of(annen)

        val feil = assertFailsWith<Feil> { service.lagrePeriode(behandlingId, request(id = annen.id)) }

        assertThat(feil.httpStatus).isEqualTo(HttpStatus.NOT_FOUND)
        verify(exactly = 0) { repository.deleteByBehandlingId(any()) }
        verify(exactly = 0) { repository.insertAll(any()) }
        verify(exactly = 0) { endringshistorikkService.registrerEndring(any(), any(), any()) }
    }

    @Test
    fun `slettPeriode med id fra annen behandling gir 404 uten skriving`() {
        val annen = trygdetid(behandlingId = UUID.randomUUID())
        every { repository.findById(annen.id) } returns Optional.of(annen)

        val feil = assertFailsWith<Feil> { service.slettPeriode(behandlingId, annen.id) }

        assertThat(feil.httpStatus).isEqualTo(HttpStatus.NOT_FOUND)
        verify(exactly = 0) { repository.deleteById(any()) }
        verify(exactly = 0) { endringshistorikkService.registrerEndring(any(), any(), any()) }
    }

    @Test
    fun `kopierPerioder kopierer alle kilder med ny id og behandling`() {
        val nyBehandlingId = UUID.randomUUID()
        val perioder =
            listOf(
                trygdetid(
                    teller = 30,
                    kilde = TrygdetidKilde.ALDERSPENSJON,
                    begrunnelse = "Alderspensjon",
                    fraOgMedDato = LocalDate.of(1990, 1, 1),
                    tilOgMedDato = LocalDate.of(2019, 12, 31),
                ),
                trygdetid(
                    teller = 12,
                    kilde = TrygdetidKilde.UFØRETRYGD,
                    begrunnelse = "Uføretrygd",
                    fraOgMedDato = LocalDate.of(2020, 1, 1),
                    tilOgMedDato = LocalDate.of(2025, 6, 30),
                ),
                trygdetid(teller = 40, kilde = TrygdetidKilde.INGEN, begrunnelse = "Ingen kilde"),
            )
        every { repository.findByBehandlingId(behandlingId) } returns perioder

        val kopier = service.kopierPerioder(fraBehandlingId = behandlingId, tilBehandlingId = nyBehandlingId)

        assertThat(kopier).hasSize(3)
        assertThat(kopier.map { it.kilde })
            .containsExactlyInAnyOrder(TrygdetidKilde.ALDERSPENSJON, TrygdetidKilde.UFØRETRYGD, TrygdetidKilde.INGEN)
        assertThat(kopier.map { it.behandlingId }).containsOnly(nyBehandlingId)
        assertThat(kopier.map { it.id }).doesNotContainAnyElementsOf(perioder.map { it.id })
        perioder.forEach { original ->
            val kopi = kopier.single { it.kilde == original.kilde }
            assertThat(kopi.teller).isEqualTo(original.teller)
            assertThat(kopi.begrunnelse).isEqualTo(original.begrunnelse)
            assertThat(kopi.fraOgMedDato).isEqualTo(original.fraOgMedDato)
            assertThat(kopi.tilOgMedDato).isEqualTo(original.tilOgMedDato)
        }
        assertThat(perioder.map { it.behandlingId }).containsOnly(behandlingId)
        assertThat(perioder.map { it.kilde })
            .containsExactly(TrygdetidKilde.ALDERSPENSJON, TrygdetidKilde.UFØRETRYGD, TrygdetidKilde.INGEN)
        verify(exactly = 1) { repository.insertAll(kopier) }
    }

    @Test
    fun `til og med-dato før fra og med-dato avvises av tidslinjen uten skriving`() {
        assertThatThrownBy {
            service.lagrePeriode(
                behandlingId,
                request(fraOgMedDato = LocalDate.of(2025, 6, 1), tilOgMedDato = LocalDate.of(2025, 1, 1)),
            )
        }.isInstanceOfAny(IllegalArgumentException::class.java, IllegalStateException::class.java)

        verify(exactly = 0) { repository.deleteByBehandlingId(any()) }
        verify(exactly = 0) { repository.insertAll(any()) }
    }

    private fun avvisesUtenSkriving(ugyldig: TrygdetidRequest) {
        val feil = assertFailsWith<UgyldigInput> { service.lagrePeriode(behandlingId, ugyldig) }

        assertThat(ApiExceptionHandler().handleUgyldigInput(feil).statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        verify(exactly = 0) { repository.deleteByBehandlingId(any()) }
        verify(exactly = 0) { repository.insertAll(any()) }
        verify(exactly = 0) { endringshistorikkService.registrerEndring(any(), any(), any()) }
    }

    private fun request(
        id: UUID? = null,
        teller: Int = 40,
        kilde: TrygdetidKilde = TrygdetidKilde.ALDERSPENSJON,
        fraOgMedDato: LocalDate? = null,
        tilOgMedDato: LocalDate? = null,
    ) = TrygdetidRequest(
        id = id,
        teller = teller,
        kilde = kilde,
        begrunnelse = "Test",
        fraOgMedDato = fraOgMedDato,
        tilOgMedDato = tilOgMedDato,
    )

    private fun trygdetid(
        behandlingId: UUID = this.behandlingId,
        teller: Int = 40,
        kilde: TrygdetidKilde = TrygdetidKilde.ALDERSPENSJON,
        begrunnelse: String = "Test",
        fraOgMedDato: LocalDate? = null,
        tilOgMedDato: LocalDate? = null,
    ) = Trygdetid(
        behandlingId = behandlingId,
        teller = teller,
        kilde = kilde,
        begrunnelse = begrunnelse,
        fraOgMedDato = fraOgMedDato,
        tilOgMedDato = tilOgMedDato,
    )
}
