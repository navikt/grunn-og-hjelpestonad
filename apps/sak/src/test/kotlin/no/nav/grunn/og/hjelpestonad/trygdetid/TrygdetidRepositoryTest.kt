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
                antallÅr = 25,
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
        val trygdetid = trygdetid(behandlingId, antallÅr = 25, kilde = TrygdetidKilde.MIGRERING)

        trygdetidRepository.insert(trygdetid)

        assertThat(trygdetidRepository.findByBehandlingId(behandlingId)).containsExactly(trygdetid)
    }

    @Test
    fun `MIGRERING godtar 0 og 40 år`() {
        val behandlingId = opprettBehandling()

        assertThatCode {
            trygdetidRepository.insert(trygdetid(behandlingId, antallÅr = 0, kilde = TrygdetidKilde.MIGRERING))
            trygdetidRepository.insert(trygdetid(behandlingId, antallÅr = 40, kilde = TrygdetidKilde.MIGRERING))
        }.doesNotThrowAnyException()

        assertThat(trygdetidRepository.findByBehandlingId(behandlingId)).hasSize(2)
    }

    @Test
    fun `0 og 40 år godtas`() {
        val behandlingId = opprettBehandling()

        assertThatCode {
            trygdetidRepository.insert(trygdetid(behandlingId, antallÅr = 0, kilde = TrygdetidKilde.ALDERSPENSJON))
            trygdetidRepository.insert(trygdetid(behandlingId, antallÅr = 40, kilde = TrygdetidKilde.UFØRETRYGD))
        }.doesNotThrowAnyException()

        assertThat(trygdetidRepository.findByBehandlingId(behandlingId)).hasSize(2)
    }

    @Test
    fun `under 0 og over 40 år avvises`() {
        val behandlingId = opprettBehandling()

        assertThatThrownBy {
            trygdetidRepository.insert(trygdetid(behandlingId, antallÅr = -1, kilde = TrygdetidKilde.ALDERSPENSJON))
        }.hasStackTraceContaining("trygdetid_antall_aar_omraade")

        assertThatThrownBy {
            trygdetidRepository.insert(trygdetid(behandlingId, antallÅr = 41, kilde = TrygdetidKilde.ALDERSPENSJON))
        }.hasStackTraceContaining("trygdetid_antall_aar_omraade")
    }

    @Test
    fun `databasen avviser kilde som ikke finnes i enumen`() {
        val behandlingId = opprettBehandling()

        assertThatThrownBy {
            jdbcTemplate.update(
                "INSERT INTO trygdetid (id, behandling_id, antall_aar, kilde, opprettet_av, opprettet_tid, endret_av, endret_tid) " +
                    "VALUES (?, ?, 10, 'ANNET', 'test', now(), 'test', now())",
                UUID.randomUUID(),
                behandlingId,
            )
        }.hasStackTraceContaining("trygdetid_kilde_gyldig")
    }

    @Test
    fun `kilde INGEN krever 40 år`() {
        val behandlingId = opprettBehandling()

        assertThatThrownBy {
            trygdetidRepository.insert(trygdetid(behandlingId, antallÅr = 39, kilde = TrygdetidKilde.INGEN))
        }.hasStackTraceContaining("trygdetid_ingen_gir_40")

        assertThatCode {
            trygdetidRepository.insert(trygdetid(behandlingId, antallÅr = 40, kilde = TrygdetidKilde.INGEN))
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
        trygdetidRepository.insert(trygdetid(behandlingId, antallÅr = 30))
        trygdetidRepository.insert(trygdetid(annenBehandlingId, antallÅr = 12))

        behandlingRepository.deleteById(behandlingId)

        assertThat(trygdetidRepository.findByBehandlingId(behandlingId)).isEmpty()
        assertThat(trygdetidRepository.findByBehandlingId(annenBehandlingId)).hasSize(1)
    }

    private fun trygdetid(
        behandlingId: UUID,
        antallÅr: Int = 40,
        kilde: TrygdetidKilde = TrygdetidKilde.ALDERSPENSJON,
        begrunnelse: String = "Test",
        fraOgMedDato: LocalDate? = null,
        tilOgMedDato: LocalDate? = null,
    ) = Trygdetid(
        behandlingId = behandlingId,
        antallÅr = antallÅr,
        kilde = kilde,
        begrunnelse = begrunnelse,
        fraOgMedDato = fraOgMedDato,
        tilOgMedDato = tilOgMedDato,
    )

    private fun opprettBehandling(): UUID {
        val fagsakPerson = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(Personident(tilfeldigIdent()))))
        val fagsak = fagsakRepository.insert(Fagsak(fagsakPersonId = fagsakPerson.id, stønadstype = StønadType.GRUNNSTØNAD))
        return behandlingRepository
            .insert(Behandling(fagsakId = fagsak.id, status = BehandlingStatus.UTREDES, resultat = BehandlingResultat.IKKE_SATT))
            .id
    }

    private fun tilfeldigIdent(): String = (10_000_000_000L..99_999_999_999L).random().toString()
}
