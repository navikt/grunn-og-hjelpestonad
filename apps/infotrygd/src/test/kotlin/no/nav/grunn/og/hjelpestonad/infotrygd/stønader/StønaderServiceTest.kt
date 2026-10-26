package no.nav.grunn.og.hjelpestonad.infotrygd.stønader

import no.nav.grunn.og.hjelpestonad.infotrygd.Application
import no.nav.grunn.og.hjelpestonad.infotrygd.ReplikaTestdata
import no.nav.grunn.og.hjelpestonad.infotrygd.evaluering.Ev10Posisjon
import no.nav.grunn.og.hjelpestonad.infotrygd.exodus.ExodusTabell
import no.nav.grunn.og.hjelpestonad.infotrygd.exodus.ExodusUpsertRepository
import no.nav.grunn.og.hjelpestonad.infotrygd.stønad.Stønadstype
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import kotlin.test.assertEquals
import kotlin.test.assertNull

@SpringBootTest(classes = [Application::class])
@ActiveProfiles("test")
class StønaderServiceTest {
    @Autowired
    private lateinit var stønaderService: StønaderService

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var upsertRepository: ExodusUpsertRepository

    private val testdata by lazy { ReplikaTestdata(jdbcTemplate, upsertRepository) }

    // sa_sak_10, ev_valg_05 og ev_data_10 har primærnøkkel på id, så hver rad må ha egen id.
    private var nesteId = 1000L

    private val personident = "01017012345"
    private val reversertPersonident = "70010112345"
    private val personkey = "100"
    private val oktober = YearMonth.of(2025, 10)

    @BeforeEach
    fun tømTabeller() {
        testdata.tømAlle()
        testdata.lagreTLopenrFnr("1", personident)
    }

    @Test
    fun `bygger treet fra stønad til endringer, delytelser og gh, med trimmet tekst`() {
        lagreStønad("10", kodeOpphor = "O ")
        lagreVedtak("100", stønadId = "10", typeSak = "S ", kodeResultat = "I ", datoMottattSak = "2019-12-15")
        lagreEndring("100", "F ")
        lagreDelytelse("100", type = "GS", fom = "2020-01-01", tom = null, beløp = "1000.50")
        testdata.lagre(ExodusTabell.T_GH, "vedtak_id" to "100", "tidspunkt_reg" to "2020-01-01T00:00", "trygdetid" to "40", "sats_gs" to "2")

        val stønad = hentGrunnstønad().single()

        assertEquals(10L, stønad.stonadId)
        assertEquals(personident, stønad.personnr)
        assertEquals("GS", stønad.kodeRutine)
        assertEquals("O", stønad.kodeOpphor)
        assertEquals(1L, stønad.oppdragId)

        val vedtak = stønad.vedtak.single()
        assertEquals(100L, vedtak.vedtakId)
        assertEquals("S", vedtak.typeSak)
        assertEquals("I", vedtak.kodeResultat)
        assertEquals("A", vedtak.saksblokk)
        assertEquals(123L, vedtak.saksnr)
        assertEquals(LocalDate.of(2019, 12, 15), vedtak.datoMottattSak)
        assertNull(vedtak.datoInnvTom)
        assertEquals(listOf("F"), vedtak.endringer)

        val delytelse = vedtak.delytelser.single()
        assertEquals("GS", delytelse.typeDelytelse)
        assertEquals(BigDecimal("1000.50"), delytelse.belop)
        assertEquals(LocalDate.of(2020, 1, 1), delytelse.fom)
        assertNull(delytelse.tom)

        val gh = vedtak.gh.single()
        assertEquals(40L, gh.trygdetid)
        assertEquals(2L, gh.satsGs)
        assertEquals(LocalDateTime.of(2020, 1, 1, 0, 0), gh.tidspunktReg)
    }

    @Test
    fun `vedtak med to endringskoder og to delytelser gir ett vedtak uten duplikater`() {
        lagreStønad("10")
        lagreVedtak("100", stønadId = "10")
        lagreEndring("100", "F")
        lagreEndring("100", "O")
        lagreDelytelse("100", fom = "2020-01-01", tom = "2020-12-31", tidspunktReg = "2020-01-01T00:00")
        lagreDelytelse("100", fom = "2021-01-01", tom = null, tidspunktReg = "2021-01-01T00:00")

        val vedtak = hentGrunnstønad().single().vedtak.single()

        assertEquals(listOf("F", "O"), vedtak.endringer)
        assertEquals(listOf(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1)), vedtak.delytelser.map { it.fom })
    }

    @Test
    fun `to registreringer av samme delytelse med lik fom, tom og beløp gir to delytelser`() {
        lagreStønad("10")
        lagreVedtak("100", stønadId = "10")
        lagreDelytelse("100", fom = "2020-01-01", tom = null, beløp = "1000", tidspunktReg = "2020-01-01T00:00")
        lagreDelytelse("100", fom = "2020-01-01", tom = null, beløp = "1000", tidspunktReg = "2020-02-01T00:00")

        val delytelser =
            hentGrunnstønad()
                .single()
                .vedtak
                .single()
                .delytelser

        assertEquals(
            listOf(LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2020, 2, 1, 0, 0)),
            delytelser.map { it.tidspunktReg },
        )
    }

    @Test
    fun `stønad uten vedtak og vedtak uten endringer, delytelser og gh tas med`() {
        lagreStønad("10")
        lagreStønad("11")
        lagreVedtak("110", stønadId = "11")

        val stønader = hentGrunnstønad()

        assertEquals(listOf(10L, 11L), stønader.map { it.stonadId })
        assertEquals(emptyList(), stønader[0].vedtak)
        val vedtak = stønader[1].vedtak.single()
        assertEquals(emptyList(), vedtak.endringer)
        assertEquals(emptyList(), vedtak.delytelser)
        assertEquals(emptyList(), vedtak.gh)
        assertNull(vedtak.evaluering)
    }

    @Test
    fun `henter bare oppgitt stønadstype og oppgitte personer`() {
        testdata.lagreTLopenrFnr("2", "02027012345")
        lagreStønad("10", kodeRutine = "GS")
        lagreStønad("20", kodeRutine = "HS")
        lagreStønad("30", kodeRutine = "GS", personLøpenr = "2")

        assertEquals(listOf(10L), hentGrunnstønad().map { it.stonadId })
        assertEquals(listOf(20L), hent(Stønadstype.HJELPESTØNAD).map { it.stonadId })
    }

    @Test
    fun `tar med ikke iverksatt stønad og vedtak med innvilget tom lik fom`() {
        lagreStønad("10", oppdragId = null)
        lagreVedtak("100", stønadId = "10", fom = "2020-01-01", tom = "2020-01-01")
        lagreDelytelse("100", fom = "2020-01-01", tom = null, beløp = "1000")

        val stønad = hentGrunnstønad().single()

        assertNull(stønad.oppdragId)
        assertEquals(LocalDate.of(2020, 1, 1), stønad.vedtak.single().datoInnvTom)
        assertEquals(
            BigDecimal("1000.00"),
            stønad.vedtak
                .single()
                .delytelser
                .single()
                .belop,
        )
    }

    @Test
    fun `vedtak med løpenummer uten fødselsnummer tas ikke med, selv om stønaden er personens`() {
        lagreStønad("10")
        lagreVedtak("100", stønadId = "10")
        lagreVedtak("101", stønadId = "10", personLøpenr = "99")

        assertEquals(listOf(100L), hentGrunnstønad().single().vedtak.map { it.vedtakId })
    }

    @Test
    fun `vedtak på en person utenfor forespørselen tas ikke med, selv om stønaden er personens`() {
        testdata.lagreTLopenrFnr("2", "02027012345")
        lagreStønad("10")
        lagreVedtak("100", stønadId = "10", personLøpenr = "2")

        assertEquals(emptyList(), hentGrunnstønad().single().vedtak)
    }

    @Test
    fun `vedtak på en annen ident i forespørselen tas med`() {
        val annenIdent = "02027012345"
        testdata.lagreTLopenrFnr("2", annenIdent)
        lagreStønad("10")
        lagreVedtak("100", stønadId = "10", personLøpenr = "2")

        val stønader = stønaderService.hentStønader(StonaderRequest(setOf(personident, annenIdent), Stønadstype.GRUNNSTØNAD)).stonader

        assertEquals(listOf(100L), stønader.single().vedtak.map { it.vedtakId })
    }

    @Test
    fun `vedtaket får evalueringen fra saken sin`() {
        lagreStønad("10")
        lagreVedtak("100", stønadId = "10", saksblokk = "A", saksnr = "123")
        lagreSakMedEvaluering(saksnr = "123", stønadstype = Stønadstype.GRUNNSTØNAD)
        lagreEvData(femPosisjoner("G400 diagnose"))

        val evaluering =
            hentGrunnstønad()
                .single()
                .vedtak
                .single()
                .evaluering

        assertEquals("G400", evaluering?.diagnose1)
        assertEquals(oktober, evaluering?.månedNotertIInfotrygd)
        assertEquals(emptySet(), evaluering?.motstridendeFelt)
    }

    @Test
    fun `GS- og HS-sak i samme måned med ulik diagnose gir motstridende felt, også når bare grunnstønad hentes`() {
        lagreStønad("10", kodeRutine = "GS")
        lagreVedtak("100", stønadId = "10", kodeRutine = "GS", saksblokk = "A", saksnr = "123")
        lagreStønad("20", kodeRutine = "HS")
        lagreVedtak("200", stønadId = "20", kodeRutine = "HS", saksblokk = "A", saksnr = "124")
        lagreSakMedEvaluering(saksnr = "123", stønadstype = Stønadstype.GRUNNSTØNAD)
        lagreSakMedEvaluering(saksnr = "124", stønadstype = Stønadstype.HJELPESTØNAD)
        // ev_data_10 har ingen kolonne for sak, så vi kan ikke vite hvilken diagnose som hører til GS-saken.
        lagreEvData(femPosisjoner("G400 diagnose") + femPosisjoner("H500 annen diagnose"))

        val evaluering =
            hentGrunnstønad()
                .single()
                .vedtak
                .single()
                .evaluering

        assertNull(evaluering?.diagnose1)
        assertEquals(setOf(Ev10Posisjon.DIAGNOSE_1), evaluering?.motstridendeFelt)
        assertEquals("G401", evaluering?.diagnose2)
    }

    private fun hentGrunnstønad() = hent(Stønadstype.GRUNNSTØNAD)

    private fun hent(stønadstype: Stønadstype) = stønaderService.hentStønader(StonaderRequest(setOf(personident), stønadstype)).stonader

    private fun lagreStønad(
        stønadId: String,
        kodeRutine: String = "GS",
        personLøpenr: String = "1",
        oppdragId: String? = "1",
        kodeOpphor: String? = null,
    ) = testdata.lagre(
        ExodusTabell.T_STONAD,
        "stonad_id" to stønadId,
        "person_lopenr" to personLøpenr,
        "kode_rutine" to kodeRutine,
        "oppdrag_id" to oppdragId,
        "kode_opphor" to kodeOpphor,
        "dato_opphor" to null,
    )

    private fun lagreVedtak(
        vedtakId: String,
        stønadId: String,
        kodeRutine: String = "GS",
        fom: String = "2020-01-01",
        tom: String? = null,
        saksblokk: String = "A",
        saksnr: String = "123",
        typeSak: String = "S",
        kodeResultat: String = "I",
        datoMottattSak: String = "2020-01-01",
        personLøpenr: String = "1",
    ) = testdata.lagre(
        ExodusTabell.T_VEDTAK,
        "vedtak_id" to vedtakId,
        "stonad_id" to stønadId,
        "person_lopenr" to personLøpenr,
        "kode_rutine" to kodeRutine,
        "type_sak" to typeSak,
        "kode_resultat" to kodeResultat,
        "dato_innv_fom" to fom,
        "dato_innv_tom" to tom,
        "dato_mottatt_sak" to datoMottattSak,
        "saksblokk" to saksblokk,
        "saksnr" to saksnr,
    )

    private fun lagreEndring(
        vedtakId: String,
        kode: String,
    ) = testdata.lagreTEndring(vedtakId, kode)

    private fun lagreDelytelse(
        vedtakId: String,
        fom: String,
        tom: String?,
        type: String = "GS",
        beløp: String = "1000",
        tidspunktReg: String = "2020-01-01T00:00",
    ) = testdata.lagre(
        ExodusTabell.T_DELYTELSE,
        "vedtak_id" to vedtakId,
        "type_delytelse" to type,
        "tidspunkt_reg" to tidspunktReg,
        "fom" to fom,
        "tom" to tom,
        "belop" to beløp,
    )

    /** GH-saken i sa_sak_10 og raden i ev_valg_05 for saken i oktober. Radene i ev_data_10 lagres med [lagreEvData]. */
    private fun lagreSakMedEvaluering(
        saksnr: String,
        stønadstype: Stønadstype,
    ) {
        testdata.lagreSaSak10((nesteId++).toString(), reversertPersonident, "A", saksnr, valg = stønadstype.kodeRutine)
        testdata.lagreEvValg05(
            id = (nesteId++).toString(),
            personkey = personkey,
            årMånedSekvens = ReplikaTestdata.ev05Sekvens(oktober),
            stønadstype = stønadstype.kodeRutine,
            reversertFnr = reversertPersonident,
            sak = "A$saksnr",
        )
    }

    /** Rader i ev_data_10 for personen i oktober. Radene har ingen sak, bare personkey og måned. */
    private fun lagreEvData(felt: List<Pair<String, String>>) =
        felt.forEach { (posisjon, tekst) ->
            testdata.lagreEvData10((nesteId++).toString(), personkey, ReplikaTestdata.ev05Sekvens(oktober), tekst, posisjon)
        }

    private fun femPosisjoner(diagnose: String): List<Pair<String, String>> =
        listOf(
            "055" to diagnose,
            "100" to "G401 annen",
            "150" to "0123 sats",
            "160" to "B  hjemmel",
            "165" to "0826",
        )
}
