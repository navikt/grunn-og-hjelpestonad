package no.nav.grunn.og.hjelpestonad.infotrygd.diagnose

import no.nav.grunn.og.hjelpestonad.infotrygd.Application
import no.nav.grunn.og.hjelpestonad.infotrygd.ReplikaTestdata
import no.nav.grunn.og.hjelpestonad.infotrygd.stønad.Stønadstype
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.YearMonth
import kotlin.test.assertEquals

@SpringBootTest(classes = [Application::class])
@ActiveProfiles("test")
class DiagnoseRepositoryTest {
    @Autowired
    private lateinit var diagnoseRepository: DiagnoseRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var upsertRepository: no.nav.grunn.og.hjelpestonad.infotrygd.exodus.ExodusUpsertRepository

    private val testdata by lazy { ReplikaTestdata(jdbcTemplate, upsertRepository) }

    @BeforeEach
    fun tømTabeller() {
        testdata.tømAlle()
    }

    @Test
    fun `gir diagnosene fra siste måned, ikke eldre måneder`() {
        val fnr = "01017012345"
        val reversertFnr = "70010112345"
        lagreVedtak(fnr, reversertFnr)
        lagreDiagnose("1", "100", "70010112345", YearMonth.of(2025, 9), "G400 gammel")
        lagreDiagnose("2", "100", "70010112345", YearMonth.of(2025, 10), "G401 ny")

        val diagnoser = diagnoseRepository.hentSisteDiagnoser(setOf(fnr), setOf(Stønadstype.GRUNNSTØNAD))

        assertEquals(1, diagnoser.size)
        assertEquals(YearMonth.of(2025, 10), diagnoser.single().månedDiagnoseNotertIInfotrygd)
        assertEquals("G401", diagnoser.single().diagnosekode)
    }

    @Test
    fun `flere diagnoser i samme måned gir én rad per diagnose`() {
        val fnr = "01017012345"
        lagreVedtak(fnr, "70010112345")
        lagreDiagnose("1", "100", "70010112345", YearMonth.of(2025, 10), "G400 diagnose")
        lagreDiagnose("2", "100", "70010112345", YearMonth.of(2025, 10), "G401 diagnose")

        val diagnoser = diagnoseRepository.hentSisteDiagnoser(setOf(fnr), setOf(Stønadstype.GRUNNSTØNAD))

        assertEquals(listOf("G400", "G401"), diagnoser.map { it.diagnosekode })
    }

    @Test
    fun `siste måned velges per stønadstype`() {
        val fnr = "01017012345"
        lagreVedtak(fnr, "70010112345", Stønadstype.GRUNNSTØNAD, "1", "101", "1001")
        lagreVedtak(fnr, "70010112345", Stønadstype.HJELPESTØNAD, "2", "102", "1002")
        lagreDiagnose("1", "100", "70010112345", YearMonth.of(2025, 9), "G400 GS", Stønadstype.GRUNNSTØNAD)
        lagreDiagnose("2", "200", "70010112345", YearMonth.of(2025, 11), "H500 HS", Stønadstype.HJELPESTØNAD)

        val diagnoser = diagnoseRepository.hentSisteDiagnoser(setOf(fnr), Stønadstype.entries.toSet())

        assertEquals(
            listOf(
                Stønadstype.GRUNNSTØNAD to YearMonth.of(2025, 9),
                Stønadstype.HJELPESTØNAD to YearMonth.of(2025, 11),
            ),
            diagnoser.map { it.stønadstype to it.månedDiagnoseNotertIInfotrygd },
        )
    }

    @Test
    fun `filteret på stønadstype gir bare grunnstønad`() {
        val fnr = "01017012345"
        lagreVedtak(fnr, "70010112345", Stønadstype.GRUNNSTØNAD, "1", "101", "1001")
        lagreVedtak(fnr, "70010112345", Stønadstype.HJELPESTØNAD, "2", "102", "1002")
        lagreDiagnose("1", "100", "70010112345", YearMonth.of(2025, 10), "G400 GS", Stønadstype.GRUNNSTØNAD)
        lagreDiagnose("2", "200", "70010112345", YearMonth.of(2025, 11), "H500 HS", Stønadstype.HJELPESTØNAD)

        val diagnoser = diagnoseRepository.hentSisteDiagnoser(setOf(fnr), setOf(Stønadstype.GRUNNSTØNAD))

        assertEquals(listOf(Stønadstype.GRUNNSTØNAD), diagnoser.map { it.stønadstype })
    }

    @Test
    fun `ignorerer ev05-rader med annen kapittel eller tekstkode`() {
        val fnr = "01017012345"
        val reversertFnr = "70010112345"
        lagreVedtak(fnr, reversertFnr)
        val sekvens = ReplikaTestdata.ev05Sekvens(YearMonth.of(2025, 10))
        testdata.lagreEvValg05("1", "100", sekvens, "GS", reversertFnr)
        testdata.lagreEvValg05("2", "100", sekvens, "GS", reversertFnr, kapittel = "XX")
        testdata.lagreEvValg05("3", "100", sekvens, "GS", reversertFnr, tekstKode = "ANNEN")
        testdata.lagreEvData10("1", "100", sekvens, "G400 diagnose")

        val diagnoser = diagnoseRepository.hentSisteDiagnoser(setOf(fnr), setOf(Stønadstype.GRUNNSTØNAD))

        assertEquals(listOf("G400"), diagnoser.map { it.diagnosekode })
    }

    @Test
    fun `vedtak med endringskode UA eller AN gir ingen rad`() {
        listOf("UA", "AN").forEachIndexed { index, kode ->
            val fnr = if (index == 0) "01017012345" else "02027012345"
            val reversertFnr = if (index == 0) "70010112345" else "70020212345"
            lagreVedtak(fnr, reversertFnr, vedtakId = (100 + index).toString(), endringskode = kode)
            lagreDiagnose((index + 1).toString(), ((index + 1) * 100).toString(), reversertFnr, YearMonth.of(2025, 10), "G400 diagnose")
        }

        assertEquals(
            emptyList(),
            diagnoseRepository.hentSisteDiagnoser(setOf("01017012345", "02027012345"), setOf(Stønadstype.GRUNNSTØNAD)),
        )
    }

    @Test
    fun `vedtak uten delytelse gir ingen rad`() {
        val fnr = "01017012345"
        lagreVedtak(fnr, "70010112345", harDelytelse = false)
        lagreDiagnose("1", "100", "70010112345", YearMonth.of(2025, 10), "G400 diagnose")

        assertEquals(emptyList(), diagnoseRepository.hentSisteDiagnoser(setOf(fnr), setOf(Stønadstype.GRUNNSTØNAD)))
    }

    @Test
    fun `vedtak uten tilhørende GH-sak gir ingen rad`() {
        val fnr = "01017012345"
        lagreVedtak(fnr, "70010112345", saksblokk = "A", saksnummer = "123", lagreTilhørendeSak = false)
        testdata.lagreSaSak10("1", "70010112345", "B", "123")
        testdata.lagreSaSak10("2", "70010112345", "A", "456")
        lagreDiagnose("1", "100", "70010112345", YearMonth.of(2025, 10), "G400 diagnose")

        assertEquals(emptyList(), diagnoseRepository.hentSisteDiagnoser(setOf(fnr), setOf(Stønadstype.GRUNNSTØNAD)))
    }

    @Test
    fun `saksnummer med ledende null treffer vedtakets tallverdi`() {
        val fnr = "01017012345"
        lagreVedtak(fnr, "70010112345", saksnummer = "123")
        testdata.lagreSaSak10("1", "70010112345", "A", "0123")
        lagreDiagnose("1", "100", "70010112345", YearMonth.of(2025, 10), "G400 diagnose")

        val diagnoser = diagnoseRepository.hentSisteDiagnoser(setOf(fnr), setOf(Stønadstype.GRUNNSTØNAD))

        assertEquals(listOf("G400"), diagnoser.map { it.diagnosekode })
    }

    @Test
    fun `flere personer i samme kall får rader med vanlig fnr-format`() {
        val fnr1 = "01017012345"
        val fnr2 = "02027012345"
        lagreVedtak(fnr1, "70010112345", personLøpenr = "1", stønadId = "11", vedtakId = "101")
        lagreVedtak(fnr2, "70020212345", personLøpenr = "2", stønadId = "22", vedtakId = "202", saksnummer = "124")
        lagreDiagnose("1", "100", "70010112345", YearMonth.of(2025, 10), "G400 diagnose")
        lagreDiagnose("2", "200", "70020212345", YearMonth.of(2025, 10), "G401 diagnose")

        val diagnoser = diagnoseRepository.hentSisteDiagnoser(setOf(fnr1, fnr2), setOf(Stønadstype.GRUNNSTØNAD))

        assertEquals(setOf(fnr1, fnr2), diagnoser.map { it.personident }.toSet())
        assertEquals(mapOf(fnr1 to "G400", fnr2 to "G401"), diagnoser.associate { it.personident to it.diagnosekode })
    }

    @Test
    fun `identiske diagnosekoder i samme måned gir én rad`() {
        val fnr = "01017012345"
        lagreVedtak(fnr, "70010112345")
        lagreDiagnose("1", "100", "70010112345", YearMonth.of(2025, 10), "G400 diagnose")
        lagreDiagnose("2", "100", "70010112345", YearMonth.of(2025, 10), "G400 diagnose")

        val diagnoser = diagnoseRepository.hentSisteDiagnoser(setOf(fnr), setOf(Stønadstype.GRUNNSTØNAD))

        assertEquals(1, diagnoser.size)
        assertEquals("G400", diagnoser.single().diagnosekode)
    }

    private fun lagreDiagnose(
        id: String,
        personkey: String,
        reversertFnr: String,
        måned: YearMonth,
        tekst: String,
        stønadstype: Stønadstype = Stønadstype.GRUNNSTØNAD,
    ) {
        val sekvens = ReplikaTestdata.ev05Sekvens(måned)
        testdata.lagreEvValg05(id, personkey, sekvens, stønadstype.kodeRutine, reversertFnr)
        testdata.lagreEvData10(id, personkey, sekvens, tekst)
    }

    private fun lagreVedtak(
        fnr: String,
        reversertFnr: String,
        stønadstype: Stønadstype = Stønadstype.GRUNNSTØNAD,
        personLøpenr: String = "1",
        stønadId: String = "10",
        vedtakId: String = "100",
        saksblokk: String = "A",
        saksnummer: String = "123",
        endringskode: String = "F",
        harDelytelse: Boolean = true,
        lagreTilhørendeSak: Boolean = true,
    ) {
        testdata.lagreTLopenrFnr(personLøpenr, fnr)
        testdata.lagreTStonad(stønadId, personLøpenr, stønadstype.kodeRutine)
        testdata.lagreTVedtak(vedtakId, stønadId, personLøpenr, stønadstype.kodeRutine, saksblokk, saksnummer)
        testdata.lagreTEndring(vedtakId, endringskode)
        if (harDelytelse) testdata.lagreTDelytelse(vedtakId)
        if (lagreTilhørendeSak) testdata.lagreSaSak10("$vedtakId", reversertFnr, saksblokk, saksnummer)
    }
}
