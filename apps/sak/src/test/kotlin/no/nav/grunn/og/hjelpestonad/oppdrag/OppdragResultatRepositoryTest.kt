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
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.AndelTilkjentYtelse
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.TilkjentYtelse
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.TilkjentYtelseRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.transaction.IllegalTransactionStateException
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal class OppdragResultatRepositoryTest : SpringContextTest() {
    @Autowired
    private lateinit var oppdragResultatRepository: OppdragResultatRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var fagsakPersonRepository: FagsakPersonRepository

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @Autowired
    private lateinit var utbetalingsoppdragService: UtbetalingsoppdragService

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Test
    fun `lagrer og leser oppdraget som JSONB uten tap`() {
        val fagsak = opprettFagsak()
        val behandling = opprettBehandling(fagsak)
        val utbetalingsoppdrag = oppdrag(behandling)
        val resultat = oppdragResultatRepository.insert(oppdragResultat(behandling, utbetalingsoppdrag, setOf(andel(behandling))))

        val lest = oppdragResultatRepository.findByIdOrNull(behandling.id)!!
        assertThat(lest.utbetalingsoppdrag).isEqualTo(utbetalingsoppdrag)
        assertThat(lest.status).isEqualTo(OppdragStatus.OPPRETTET)
        assertThat(lest.andeler).containsExactlyElementsOf(resultat.andeler)
        assertThat(oppdragResultatRepository.finnAndelerFraForrigeBehandling(fagsak.id, unntattBehandlingId = UUID.randomUUID())).containsExactly(lest.andeler.single())
    }

    @Test
    fun `forrige andeler er andelene til siste behandling med oppdrag, også når den opphørte alt`() {
        val fagsak = opprettFagsak()
        val første = opprettBehandling(fagsak)
        val andre = opprettBehandling(fagsak)
        val tredje = opprettBehandling(fagsak)
        oppdragResultatRepository.insert(oppdragResultat(første, oppdrag(første), setOf(andel(første))))
        oppdragResultatRepository.insert(oppdragResultat(andre, oppdrag(andre), emptySet()))

        assertThat(oppdragResultatRepository.finnAndelerFraForrigeBehandling(fagsak.id, unntattBehandlingId = tredje.id)).isEmpty()
        assertThat(oppdragResultatRepository.finnAndelerFraForrigeBehandling(fagsak.id, unntattBehandlingId = andre.id).map { it.periodeId }).containsExactly(0L)
    }

    @Test
    fun `siste andel per kjede er den første lagrede med høyeste periodeId`() {
        val fagsak = opprettFagsak()
        val første = opprettBehandling(fagsak)
        val andre = opprettBehandling(fagsak)
        oppdragResultatRepository.insert(oppdragResultat(første, oppdrag(første), setOf(andel(første))))
        oppdragResultatRepository.insert(oppdragResultat(andre, oppdrag(andre), setOf(andel(andre).copy(tom = LocalDate.parse("2025-03-31")))))

        val siste = oppdragResultatRepository.finnSisteAndelPerKjede(fagsak.id, unntattBehandlingId = UUID.randomUUID()).single()

        assertThat(siste.tom).isEqualTo(LocalDate.parse("2025-06-30"))
    }

    @Test
    fun `siste andel per kjede finner forrige når siste behandling opphørte alle andelene`() {
        val fagsak = opprettFagsak()
        val første = opprettBehandling(fagsak)
        val andre = opprettBehandling(fagsak)
        oppdragResultatRepository.insert(oppdragResultat(første, oppdrag(første), setOf(andel(første))))
        oppdragResultatRepository.insert(oppdragResultat(andre, oppdrag(andre), emptySet()))

        assertThat(oppdragResultatRepository.finnSisteAndelPerKjede(fagsak.id, UUID.randomUUID()).single().tom)
            .isEqualTo(LocalDate.parse("2025-06-30"))
    }

    @Test
    fun `siste andel per kjede velger høyeste periodeId`() {
        val fagsak = opprettFagsak()
        val behandling = opprettBehandling(fagsak)
        val andelTre = andel(behandling).copy(periodeId = 3)
        val andelFem = andel(behandling).copy(periodeId = 5)
        val andelFire = andel(behandling).copy(periodeId = 4)
        oppdragResultatRepository.insert(oppdragResultat(behandling, oppdrag(behandling), setOf(andelTre, andelFem, andelFire)))

        assertThat(oppdragResultatRepository.finnSisteAndelPerKjede(fagsak.id, UUID.randomUUID()).single().periodeId).isEqualTo(5L)
    }

    @Test
    fun `siste andel per kjede holder unntatt behandling utenfor`() {
        val fagsak = opprettFagsak()
        val første = opprettBehandling(fagsak)
        val andre = opprettBehandling(fagsak)
        oppdragResultatRepository.insert(oppdragResultat(første, oppdrag(første), setOf(andel(første).copy(periodeId = 1))))
        oppdragResultatRepository.insert(oppdragResultat(andre, oppdrag(andre), setOf(andel(andre).copy(periodeId = 2))))

        assertThat(oppdragResultatRepository.finnSisteAndelPerKjede(fagsak.id, andre.id).single().periodeId).isEqualTo(1L)
    }

    @Test
    fun `status på siste oppdrag er null når fagsaken ikke har oppdrag`() {
        val fagsak = opprettFagsak()

        assertThat(oppdragRepository.finnStatusPåSisteOppdrag(fagsak.id, UUID.randomUUID())).isNull()
    }

    @Test
    fun `status på siste oppdrag følger sist lagrede oppdrag og unntar behandling`() {
        val fagsak = opprettFagsak()
        val første = opprettBehandling(fagsak)
        val andre = opprettBehandling(fagsak)
        oppdragRepository.insert(oppdragResultat(første, oppdrag(første), emptySet()).copy(status = OppdragStatus.KVITTERT_OK))
        oppdragRepository.insert(oppdragResultat(andre, oppdrag(andre), emptySet()).copy(status = OppdragStatus.LAGT_PÅ_KØ))

        assertThat(oppdragRepository.finnStatusPåSisteOppdrag(fagsak.id, UUID.randomUUID())).isEqualTo(OppdragStatus.LAGT_PÅ_KØ)
        assertThat(oppdragRepository.finnStatusPåSisteOppdrag(fagsak.id, andre.id)).isEqualTo(OppdragStatus.KVITTERT_OK)
    }

    @Test
    fun `oppdaterer status og kvitteringsmelding uten å endre andelene`() {
        val fagsak = opprettFagsak()
        val behandling = opprettBehandling(fagsak)
        val lagret = oppdragResultatRepository.insert(oppdragResultat(behandling, oppdrag(behandling), setOf(andel(behandling))))

        oppdragResultatRepository.oppdaterStatus(behandling.id, OppdragStatus.KVITTERT_OK, "Mottatt")

        val oppdatert = oppdragResultatRepository.findByIdOrNull(behandling.id)!!
        assertThat(oppdatert.status).isEqualTo(OppdragStatus.KVITTERT_OK)
        assertThat(oppdatert.kvitteringsmelding).isEqualTo("Mottatt")
        assertThat(oppdatert.andeler).containsExactlyElementsOf(lagret.andeler)
    }

    @Test
    fun `iverksetting kjeder revurderingen mot førstegangsbehandlingen og lager ikke nytt oppdrag ved nytt forsøk`() {
        val fagsak = opprettFagsak()
        val førstegang = opprettBehandling(fagsak)
        lagreTilkjentYtelse(førstegang, andelTilkjentYtelse(førstegang, 1000, "2025-01-01", "2025-06-30"))
        val førsteOppdrag = iverksettOgKvitter(førstegang)

        val revurdering = opprettBehandling(fagsak)
        lagreTilkjentYtelse(revurdering, andelTilkjentYtelse(revurdering, 1000, "2025-01-01", "2025-03-31"))
        val revurderingOppdrag = utbetalingsoppdragService.lagOgLagreUtbetalingsoppdrag(revurdering.id, "Z999999")!!.oppdragResultat
        val nyttForsøk = utbetalingsoppdragService.lagOgLagreUtbetalingsoppdrag(revurdering.id, "Z999999")!!

        assertThat(førsteOppdrag.utbetalingsoppdrag.utbetalingsperiode.map { it.periodeId }).containsExactly(0L)
        val opphør = revurderingOppdrag.utbetalingsoppdrag.utbetalingsperiode.single()
        assertThat(opphør.periodeId).isEqualTo(0L)
        assertThat(opphør.opphør?.opphørDatoFom).isEqualTo(LocalDate.parse("2025-04-01"))
        assertThat(nyttForsøk.oppdragResultat.utbetalingsoppdrag).isEqualTo(revurderingOppdrag.utbetalingsoppdrag)
        assertThat(nyttForsøk.testandel).isNull()
        assertThat(oppdragRepository.finnForrigeAndeler(fagsak.id, unntattBehandlingId = UUID.randomUUID()).map { it.tom })
            .containsExactly(LocalDate.parse("2025-03-31"))
    }

    @Test
    fun `kjede over tre behandlinger - endring, fullt opphør og ny andel`() {
        val fagsak = opprettFagsak()
        val førstegang = opprettBehandling(fagsak)
        lagreTilkjentYtelse(
            førstegang,
            andelTilkjentYtelse(førstegang, 1000, "2025-01-01", "2025-03-31"),
            andelTilkjentYtelse(førstegang, 2000, "2025-04-01", "2025-06-30"),
        )
        iverksettOgKvitter(førstegang)

        val opphør = opprettBehandling(fagsak)
        lagreTilkjentYtelse(opphør)
        val opphørsperiode = iverksettOgKvitter(opphør).utbetalingsoppdrag.utbetalingsperiode.single()

        val gjenopptak = opprettBehandling(fagsak)
        lagreTilkjentYtelse(gjenopptak, andelTilkjentYtelse(gjenopptak, 1500, "2025-07-01", "2025-12-31"))
        val nyPeriode = iverksettOgKvitter(gjenopptak).utbetalingsoppdrag.utbetalingsperiode.single()

        assertThat(opphørsperiode.periodeId).isEqualTo(1L)
        assertThat(opphørsperiode.opphør?.opphørDatoFom).isEqualTo(LocalDate.parse("2025-01-01"))
        assertThat(nyPeriode.opphør).isNull()
        assertThat(nyPeriode.periodeId).isEqualTo(2L)
        assertThat(nyPeriode.forrigePeriodeId).isEqualTo(1L)
        assertThat(oppdragRepository.finnForrigeAndeler(fagsak.id, unntattBehandlingId = UUID.randomUUID()).map { it.periodeId })
            .containsExactly(2L)
    }

    @Test
    fun `nytt oppdrag stopper når forrige oppdrag på fagsaken ikke er kvittert OK`() {
        listOf(OppdragStatus.OPPRETTET, OppdragStatus.LAGT_PÅ_KØ, "KVITTERT_FUNKSJONELL_FEIL").forEachIndexed { indeks, status ->
            val fagsak = opprettFagsak(personident = "0101019999$indeks")
            val førstegang = opprettBehandling(fagsak)
            lagreTilkjentYtelse(førstegang, andelTilkjentYtelse(førstegang, 1000, "2025-01-01", "2025-06-30"))
            utbetalingsoppdragService.lagOgLagreUtbetalingsoppdrag(førstegang.id, "Z999999")
            oppdragRepository.oppdaterStatus(førstegang.id, status, kvitteringsmelding = null)

            val revurdering = opprettBehandling(fagsak)
            lagreTilkjentYtelse(revurdering, andelTilkjentYtelse(revurdering, 1000, "2025-01-01", "2025-03-31"))

            assertThatThrownBy { utbetalingsoppdragService.lagOgLagreUtbetalingsoppdrag(revurdering.id, "Z999999") }
                .isInstanceOfSatisfying(Feil::class.java) { assertThat(it.httpStatus).isEqualTo(HttpStatus.CONFLICT) }
            assertThat(oppdragRepository.findByIdOrNull(revurdering.id)).isNull()
        }
    }

    @Test
    fun `to samtidige iverksettinger på samme fagsak gir ett oppdrag`() {
        val fagsak = opprettFagsak()
        val behandlinger = List(2) { opprettBehandling(fagsak) }
        behandlinger.forEach { lagreTilkjentYtelse(it, andelTilkjentYtelse(it, 1000, "2025-01-01", "2025-06-30")) }

        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        val resultater =
            try {
                behandlinger
                    .map { behandling ->
                        executor.submit<Result<LagretUtbetalingsoppdrag?>> {
                            start.await()
                            runCatching { utbetalingsoppdragService.lagOgLagreUtbetalingsoppdrag(behandling.id, "Z999999") }
                        }
                    }.also { start.countDown() }
                    .map { it.get(30, TimeUnit.SECONDS) }
            } finally {
                executor.shutdown()
            }

        // Låsen gjør at den andre venter til den første har committet. Da ser den et oppdrag som ikke er kvittert,
        // og stopper. Uten låsen kunne begge ha lest en tom fagsak og fått periodeId 0.
        assertThat(resultater.count { it.isSuccess }).isEqualTo(1)
        assertThat(resultater.mapNotNull { it.exceptionOrNull() }.single())
            .isInstanceOfSatisfying(Feil::class.java) { assertThat(it.httpStatus).isEqualTo(HttpStatus.CONFLICT) }
        assertThat(behandlinger.mapNotNull { oppdragRepository.findByIdOrNull(it.id) }).hasSize(1)
    }

    private fun iverksettOgKvitter(behandling: Behandling): OppdragResultat {
        val oppdrag = utbetalingsoppdragService.lagOgLagreUtbetalingsoppdrag(behandling.id, "Z999999")!!.oppdragResultat
        oppdragRepository.oppdaterStatus(behandling.id, OppdragStatus.KVITTERT_OK, kvitteringsmelding = null)
        return oppdrag
    }

    private fun lagreTilkjentYtelse(
        behandling: Behandling,
        vararg andeler: AndelTilkjentYtelse,
    ) {
        tilkjentYtelseRepository.insert(
            TilkjentYtelse(
                behandlingId = behandling.id,
                startmåned = YearMonth.from(andeler.minOfOrNull { it.fom } ?: LocalDate.parse("2025-01-01")),
                andelerTilkjentYtelse = andeler.toSet(),
            ),
        )
    }

    private fun andelTilkjentYtelse(
        behandling: Behandling,
        beløp: Int,
        fom: String,
        tom: String,
    ) = AndelTilkjentYtelse(beløp = beløp.toBigDecimal(), fom = LocalDate.parse(fom), tom = LocalDate.parse(tom), kildeBehandlingId = behandling.id)

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
                andelerTilkjentYtelse =
                    listOf(
                        AndelTilkjentYtelse(
                            beløp = 1000.toBigDecimal(),
                            fom = LocalDate.parse("2025-01-01"),
                            tom = LocalDate.parse("2025-06-30"),
                            kildeBehandlingId = behandling.id,
                        ),
                    ),
            ),
        ).utbetalingsoppdrag

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

    private fun opprettFagsak(personident: String = PERSONIDENT): Fagsak {
        val fagsakPerson = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(Personident(personident))))
        return fagsakRepository.insert(Fagsak(fagsakPersonId = fagsakPerson.id, stønadstype = StønadType.BARNETILSYN))
    }

    private fun opprettBehandling(fagsak: Fagsak): Behandling = behandlingRepository.insert(Behandling(fagsakId = fagsak.id, status = BehandlingStatus.UTREDES, resultat = BehandlingResultat.IKKE_SATT))

    companion object {
        private const val PERSONIDENT = "01010199997"
    }
}
