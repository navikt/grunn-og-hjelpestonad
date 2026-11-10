package no.nav.grunn.og.hjelpestonad.oppdrag

import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag
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
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.AndelTilkjentYtelse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.IllegalTransactionStateException
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.util.UUID

internal class OppdragRepositoryTest : SpringContextTest() {
    @Autowired
    private lateinit var oppdragRepository: OppdragRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var fagsakPersonRepository: FagsakPersonRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @Test
    fun `lagrer og leser oppdraget som JSONB uten tap`() {
        val fagsak = opprettFagsak()
        val behandling = opprettBehandling(fagsak)
        val utbetalingsoppdrag = oppdrag(behandling)
        val resultat = oppdragRepository.insert(oppdragResultat(behandling, utbetalingsoppdrag, setOf(andel(behandling))))

        val lest = oppdragRepository.findByIdOrNull(behandling.id)!!
        assertThat(lest.utbetalingsoppdrag).isEqualTo(utbetalingsoppdrag)
        assertThat(lest.status).isEqualTo(OppdragStatus.OPPRETTET)
        assertThat(lest.andeler).containsExactlyElementsOf(resultat.andeler)
        assertThat(oppdragRepository.finnForrigeAndeler(fagsak.id, unntattBehandlingId = UUID.randomUUID())).containsExactly(lest.andeler.single())
    }

    @Test
    fun `forrige andeler er andelene til siste behandling med oppdrag, også når den opphørte alt`() {
        val fagsak = opprettFagsak()
        val første = opprettBehandling(fagsak)
        val andre = opprettBehandling(fagsak)
        val tredje = opprettBehandling(fagsak)
        oppdragRepository.insert(oppdragResultat(første, oppdrag(første), setOf(andel(første))))
        oppdragRepository.insert(oppdragResultat(andre, oppdrag(andre), emptySet()))

        assertThat(oppdragRepository.finnForrigeAndeler(fagsak.id, unntattBehandlingId = tredje.id)).isEmpty()
        assertThat(oppdragRepository.finnForrigeAndeler(fagsak.id, unntattBehandlingId = andre.id).map { it.periodeId }).containsExactly(0L)
    }

    @Test
    fun `siste andel per kjede er den første lagrede med høyeste periodeId`() {
        val fagsak = opprettFagsak()
        val første = opprettBehandling(fagsak)
        val andre = opprettBehandling(fagsak)
        oppdragRepository.insert(oppdragResultat(første, oppdrag(første), setOf(andel(første))))
        oppdragRepository.insert(oppdragResultat(andre, oppdrag(andre), setOf(andel(andre).copy(tom = LocalDate.parse("2025-03-31")))))

        val siste = oppdragRepository.finnSisteAndelPerKjede(fagsak.id, unntattBehandlingId = UUID.randomUUID()).single()

        assertThat(siste.tom).isEqualTo(LocalDate.parse("2025-06-30"))
    }

    @Test
    fun `siste andel per kjede finner forrige når siste behandling opphørte alle andelene`() {
        val fagsak = opprettFagsak()
        val første = opprettBehandling(fagsak)
        val andre = opprettBehandling(fagsak)
        oppdragRepository.insert(oppdragResultat(første, oppdrag(første), setOf(andel(første))))
        oppdragRepository.insert(oppdragResultat(andre, oppdrag(andre), emptySet()))

        assertThat(oppdragRepository.finnSisteAndelPerKjede(fagsak.id, UUID.randomUUID()).single().tom)
            .isEqualTo(LocalDate.parse("2025-06-30"))
    }

    @Test
    fun `siste andel per kjede velger høyeste periodeId`() {
        val fagsak = opprettFagsak()
        val behandling = opprettBehandling(fagsak)
        val andelTre = andel(behandling).copy(periodeId = 3)
        val andelFem = andel(behandling).copy(periodeId = 5)
        val andelFire = andel(behandling).copy(periodeId = 4)
        oppdragRepository.insert(oppdragResultat(behandling, oppdrag(behandling), setOf(andelTre, andelFem, andelFire)))

        assertThat(oppdragRepository.finnSisteAndelPerKjede(fagsak.id, UUID.randomUUID()).single().periodeId).isEqualTo(5L)
    }

    @Test
    fun `siste andel per kjede holder unntatt behandling utenfor`() {
        val fagsak = opprettFagsak()
        val første = opprettBehandling(fagsak)
        val andre = opprettBehandling(fagsak)
        oppdragRepository.insert(oppdragResultat(første, oppdrag(første), setOf(andel(første).copy(periodeId = 1))))
        oppdragRepository.insert(oppdragResultat(andre, oppdrag(andre), setOf(andel(andre).copy(periodeId = 2))))

        assertThat(oppdragRepository.finnSisteAndelPerKjede(fagsak.id, andre.id).single().periodeId).isEqualTo(1L)
    }

    @Test
    fun `rekkefølgen følger lagring og ikke opprettet tid`() {
        val fagsak = opprettFagsak()
        val første = opprettBehandling(fagsak)
        val andre = opprettBehandling(fagsak)
        oppdragRepository.insert(oppdragResultat(første, oppdrag(første), setOf(andel(første))))
        oppdragRepository.insert(oppdragResultat(andre, oppdrag(andre), setOf(andel(andre))))
        jdbcTemplate.update("UPDATE oppdrag_resultat SET opprettet_tid = opprettet_tid - INTERVAL '1 hour' WHERE behandling_id = ?", andre.id)

        assertThat(oppdragRepository.finnForrigeAndeler(fagsak.id, UUID.randomUUID()).single().kildeBehandlingId).isEqualTo(andre.id)
    }

    @Test
    fun `oppdaterer status og kvitteringsmelding uten å endre andelene`() {
        val fagsak = opprettFagsak()
        val behandling = opprettBehandling(fagsak)
        val lagret = oppdragRepository.insert(oppdragResultat(behandling, oppdrag(behandling), setOf(andel(behandling))))

        oppdragRepository.oppdaterStatus(behandling.id, OppdragStatus.KVITTERT_OK, "Mottatt")

        val oppdatert = oppdragRepository.findByIdOrNull(behandling.id)!!
        assertThat(oppdatert.status).isEqualTo(OppdragStatus.KVITTERT_OK)
        assertThat(oppdatert.kvitteringsmelding).isEqualTo("Mottatt")
        assertThat(oppdatert.andeler).containsExactlyElementsOf(lagret.andeler)
    }

    @Test
    fun `låser fagsak bare inni en transaksjon`() {
        val fagsak = opprettFagsak()

        assertThatThrownBy { oppdragRepository.låsFagsak(fagsak.id) }
            .isInstanceOf(IllegalTransactionStateException::class.java)
        assertThat(TransactionTemplate(transactionManager).execute { oppdragRepository.låsFagsak(fagsak.id) }).isEqualTo(fagsak.id)
    }

    private fun oppdragResultat(
        behandling: Behandling,
        utbetalingsoppdrag: Utbetalingsoppdrag,
        andeler: Set<Utbetalingsandel>,
    ) = OppdragResultat(
        behandlingId = behandling.id,
        fagsakId = behandling.fagsakId,
        utbetalingsoppdrag = utbetalingsoppdrag,
        status = OppdragStatus.OPPRETTET,
        andeler = andeler,
    )

    private fun oppdrag(behandling: Behandling): Utbetalingsoppdrag =
        lagUtbetalingsoppdrag(
            Oppdragsgrunnlag(
                behandlingId = behandling.id,
                eksternBehandlingId = behandling.eksternId,
                eksternFagsakId = 1L,
                personIdent = PERSONIDENT,
                saksbehandlerId = "Z999999",
                vedtaksdato = LocalDate.parse("2025-07-01"),
                andeler =
                    listOf(
                        AndelTilkjentYtelse(
                            beløp = 1000.toBigDecimal(),
                            fom = LocalDate.parse("2025-01-01"),
                            tom = LocalDate.parse("2025-06-30"),
                            kildeBehandlingId = behandling.id,
                        ),
                    ),
            ),
        )

    private fun andel(behandling: Behandling) =
        Utbetalingsandel(
            fom = LocalDate.parse("2025-01-01"),
            tom = LocalDate.parse("2025-06-30"),
            beløp = 1000,
            ytelsestype = GrunnOgHjelpYtelsestype.GRUNNSTØNAD,
            periodeId = 0,
            forrigePeriodeId = null,
            kildeBehandlingId = behandling.id,
        )

    private fun opprettFagsak(): Fagsak {
        val fagsakPerson = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(Personident(PERSONIDENT))))
        return fagsakRepository.insert(Fagsak(fagsakPersonId = fagsakPerson.id, stønadstype = StønadType.BARNETILSYN))
    }

    private fun opprettBehandling(fagsak: Fagsak): Behandling = behandlingRepository.insert(Behandling(fagsakId = fagsak.id, status = BehandlingStatus.UTREDES, resultat = BehandlingResultat.IKKE_SATT))

    companion object {
        private const val PERSONIDENT = "01010199997"
    }
}
