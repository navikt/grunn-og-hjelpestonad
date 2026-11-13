package no.nav.grunn.og.hjelpestonad.trygdetid

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
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.util.UUID

class TrygdetidRepositoryTest(
    private val trygdetidRepository: TrygdetidRepository,
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val fagsakPersonRepository: FagsakPersonRepository,
    private val jdbcTemplate: JdbcTemplate,
) : SpringContextTest() {
    @Test
    fun `trygdetid lagres og hentes på behandling`() {
        val behandlingId = opprettBehandling()
        val trygdetid =
            trygdetid(
                behandlingId,
                teller = 25,
                kilde = TrygdetidKilde.UFØRETRYGD,
                begrunnelse = "Uføretrygd fra 2010",
                fraOgMedDato = LocalDate.of(2010, 1, 1),
                tilOgMedDato = LocalDate.of(2024, 12, 31),
            )

        trygdetidRepository.insert(trygdetid)

        assertThat(trygdetidRepository.findByBehandlingId(behandlingId)).containsExactly(trygdetid)
    }

    @Test
    fun `MIGRERING lagres og hentes på behandling`() {
        val behandlingId = opprettBehandling()
        val trygdetid = trygdetid(behandlingId, teller = 25, kilde = TrygdetidKilde.MIGRERING)

        trygdetidRepository.insert(trygdetid)

        assertThat(trygdetidRepository.findByBehandlingId(behandlingId)).containsExactly(trygdetid)
    }

    @Test
    fun `MIGRERING godtar teller 0 og 40`() {
        val behandlingId = opprettBehandling()

        assertThatCode {
            trygdetidRepository.insert(trygdetid(behandlingId, teller = 0, kilde = TrygdetidKilde.MIGRERING))
            trygdetidRepository.insert(trygdetid(behandlingId, teller = 40, kilde = TrygdetidKilde.MIGRERING))
        }.doesNotThrowAnyException()

        assertThat(trygdetidRepository.findByBehandlingId(behandlingId)).hasSize(2)
    }

    @Test
    fun `teller 0 og 40 godtas`() {
        val behandlingId = opprettBehandling()

        assertThatCode {
            trygdetidRepository.insert(trygdetid(behandlingId, teller = 0, kilde = TrygdetidKilde.ALDERSPENSJON))
            trygdetidRepository.insert(trygdetid(behandlingId, teller = 40, kilde = TrygdetidKilde.UFØRETRYGD))
        }.doesNotThrowAnyException()

        assertThat(trygdetidRepository.findByBehandlingId(behandlingId)).hasSize(2)
    }

    @Test
    fun `teller under 0 og over 40 avvises`() {
        val behandlingId = opprettBehandling()

        assertThatThrownBy {
            trygdetidRepository.insert(trygdetid(behandlingId, teller = -1, kilde = TrygdetidKilde.ALDERSPENSJON))
        }.hasStackTraceContaining("trygdetid_teller_omraade")

        assertThatThrownBy {
            trygdetidRepository.insert(trygdetid(behandlingId, teller = 41, kilde = TrygdetidKilde.ALDERSPENSJON))
        }.hasStackTraceContaining("trygdetid_teller_omraade")
    }

    @Test
    fun `databasen avviser kilde som ikke finnes i enumen`() {
        val behandlingId = opprettBehandling()

        assertThatThrownBy {
            jdbcTemplate.update(
                "INSERT INTO trygdetid (id, behandling_id, teller, kilde, opprettet_av, opprettet_tid, endret_av, endret_tid) " +
                    "VALUES (?, ?, 10, 'ANNET', 'test', now(), 'test', now())",
                UUID.randomUUID(),
                behandlingId,
            )
        }.hasStackTraceContaining("trygdetid_kilde_gyldig")
    }

    @Test
    fun `kilde INGEN krever teller 40`() {
        val behandlingId = opprettBehandling()

        assertThatThrownBy {
            trygdetidRepository.insert(trygdetid(behandlingId, teller = 39, kilde = TrygdetidKilde.INGEN))
        }.hasStackTraceContaining("trygdetid_ingen_gir_40")

        assertThatCode {
            trygdetidRepository.insert(trygdetid(behandlingId, teller = 40, kilde = TrygdetidKilde.INGEN))
        }.doesNotThrowAnyException()
    }

    @Test
    fun `til og med-dato før fra og med-dato avvises`() {
        val behandlingId = opprettBehandling()

        assertThatThrownBy {
            trygdetidRepository.insert(
                trygdetid(
                    behandlingId,
                    fraOgMedDato = LocalDate.of(2025, 6, 1),
                    tilOgMedDato = LocalDate.of(2025, 1, 1),
                ),
            )
        }.hasStackTraceContaining("trygdetid_datorekkefolge")
    }

    @Test
    fun `til og med-dato lik fra og med-dato godtas`() {
        val behandlingId = opprettBehandling()
        val dato = LocalDate.of(2025, 1, 1)

        assertThatCode {
            trygdetidRepository.insert(trygdetid(behandlingId, fraOgMedDato = dato, tilOgMedDato = dato))
        }.doesNotThrowAnyException()
    }

    @Test
    fun `sletting av behandlingen sletter trygdetidene til den behandlingen`() {
        val behandlingId = opprettBehandling()
        val annenBehandlingId = opprettBehandling()
        trygdetidRepository.insert(trygdetid(behandlingId, teller = 30))
        trygdetidRepository.insert(trygdetid(annenBehandlingId, teller = 12))

        behandlingRepository.deleteById(behandlingId)

        assertThat(trygdetidRepository.findByBehandlingId(behandlingId)).isEmpty()
        assertThat(trygdetidRepository.findByBehandlingId(annenBehandlingId)).hasSize(1)
    }

    private fun trygdetid(
        behandlingId: UUID,
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

    private fun opprettBehandling(): UUID {
        val fagsakPerson = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(Personident(tilfeldigIdent()))))
        val fagsak = fagsakRepository.insert(Fagsak(fagsakPersonId = fagsakPerson.id, stønadstype = StønadType.BARNETILSYN))
        return behandlingRepository
            .insert(Behandling(fagsakId = fagsak.id, status = BehandlingStatus.UTREDES, resultat = BehandlingResultat.IKKE_SATT))
            .id
    }

    private fun tilfeldigIdent(): String = (10_000_000_000L..99_999_999_999L).random().toString()
}
