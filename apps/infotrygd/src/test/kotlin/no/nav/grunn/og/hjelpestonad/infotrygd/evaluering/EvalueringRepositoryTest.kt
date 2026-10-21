package no.nav.grunn.og.hjelpestonad.infotrygd.evaluering

import no.nav.grunn.og.hjelpestonad.infotrygd.Application
import no.nav.grunn.og.hjelpestonad.infotrygd.ReplikaTestdata
import no.nav.grunn.og.hjelpestonad.infotrygd.exodus.ExodusUpsertRepository
import no.nav.grunn.og.hjelpestonad.infotrygd.stønad.Stønadstype
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.YearMonth
import kotlin.test.assertEquals
import kotlin.test.assertNull

@SpringBootTest(classes = [Application::class])
@ActiveProfiles("test")
class EvalueringRepositoryTest {
    @Autowired
    private lateinit var evalueringService: EvalueringService

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var upsertRepository: ExodusUpsertRepository

    private val testdata by lazy { ReplikaTestdata(jdbcTemplate, upsertRepository) }

    // ev_valg_05, ev_data_10 og sa_sak_10 har primærnøkkel på id, og upsert overskriver ved samme id, så hver rad må ha egen id.
    private var neste05Id = 1000L
    private var neste10Id = 1000L
    private var nesteSakId = 1000L

    private val fnr = "01017012345"
    private val reversertFnr = "70010112345"
    private val personkey = "100"
    private val september = YearMonth.of(2025, 9)
    private val oktober = YearMonth.of(2025, 10)

    @BeforeEach
    fun tømTabeller() {
        testdata.tømAlle()
    }

    @Test
    fun `vedtaket får evalueringen fra sin egen sak, med alle fem posisjonene`() {
        lagreVedtakMedSak(100L)
        lagreEvaluering(
            sak = "A123",
            måned = oktober,
            felt =
                listOf(
                    "055" to "G400 epilepsi",
                    "100" to "G401 annen",
                    "150" to "0123 sats",
                    "160" to "B  hjemmel",
                    "165" to "0826",
                ),
        )

        val evaluering = hentEvaluering(100L)

        assertEquals("G400", evaluering.diagnose1)
        assertEquals("G401", evaluering.diagnose2)
        assertEquals("0123 sats", evaluering.sats)
        assertEquals("B  hjemmel", evaluering.hjemmelbokstav)
        assertEquals(YearMonth.of(2026, 8), evaluering.virkningstidspunkt)
        assertEquals(emptySet(), evaluering.motstridendeFelt)
    }

    @Test
    fun `siste måned for saken velges, ikke eldre måneder`() {
        lagreVedtakMedSak(100L)
        lagreEvaluering(sak = "A123", måned = september, felt = femPosisjoner("G400 gammel"))
        lagreEvaluering(sak = "A123", måned = oktober, felt = femPosisjoner("G401 ny"))

        val evaluering = hentEvaluering(100L)

        assertEquals(oktober, evaluering.månedNotertIInfotrygd)
        assertEquals("G401", evaluering.diagnose1)
        assertEquals(emptySet(), evaluering.motstridendeFelt)
    }

    @Test
    fun `vedtak uten evaluering er ikke med i kartet`() {
        lagreVedtakMedSak(100L, saksnummer = "123")
        lagreVedtakMedSak(200L, saksnummer = "456")
        lagreEvaluering(sak = "A456", måned = oktober)

        val evalueringer = evalueringService.hentEvalueringerForVedtak(setOf(100L, 200L))

        assertEquals(setOf(200L), evalueringer.keys)
    }

    @Test
    fun `ev05_sak for en annen sak hos samme person blir ikke tatt med`() {
        lagreVedtakMedSak(100L, saksnummer = "123")
        lagreSak(saksnummer = "124")
        lagreEvaluering(sak = "A123", måned = september, felt = femPosisjoner("G400 egen sak"))
        lagreEvaluering(sak = "A124", måned = oktober, felt = femPosisjoner("H500 annen sak"))

        val evaluering = hentEvaluering(100L)

        assertEquals(september, evaluering.månedNotertIInfotrygd)
        assertEquals("G400", evaluering.diagnose1)
        assertEquals(emptySet(), evaluering.motstridendeFelt)
    }

    @Test
    fun `s10_saksnr med ledende null treffer t_vedtak_saksnr`() {
        lagreVedtakMedSak(100L, saksnummer = "123", saksnummerISak = "0123")
        lagreEvaluering(sak = "A0123", måned = oktober)

        assertEquals("G400", hentEvaluering(100L).diagnose1)
    }

    @Test
    fun `to vedtak i samme sak får samme evaluering`() {
        lagreVedtak(100L)
        lagreVedtak(101L)
        lagreSak()
        lagreEvaluering(sak = "A123", måned = oktober)

        val evalueringer = evalueringService.hentEvalueringerForVedtak(setOf(100L, 101L))

        assertEquals(setOf(100L, 101L), evalueringer.keys)
        assertEquals(evalueringer.getValue(100L), evalueringer.getValue(101L))
        assertEquals("G400", evalueringer.getValue(101L).diagnose1)
    }

    @Test
    fun `to saker med GS og HS i samme måned med samme verdi gir alle feltene for begge vedtakene`() {
        lagreVedtakMedSak(100L, saksnummer = "123", stønadstype = Stønadstype.GRUNNSTØNAD)
        lagreVedtakMedSak(200L, saksnummer = "124", stønadstype = Stønadstype.HJELPESTØNAD)
        lagreEvValgForSak(sak = "A123", måned = oktober, stønadstype = Stønadstype.GRUNNSTØNAD)
        lagreEvValgForSak(sak = "A124", måned = oktober, stønadstype = Stønadstype.HJELPESTØNAD)
        lagreEvDataForMåned(oktober, femPosisjoner() + femPosisjoner())

        val evalueringer = evalueringService.hentEvalueringerForVedtak(setOf(100L, 200L))

        assertEquals(setOf(100L, 200L), evalueringer.keys)
        evalueringer.values.forEach { evaluering ->
            assertEquals("G400", evaluering.diagnose1)
            assertEquals("G401", evaluering.diagnose2)
            assertEquals("0123 sats", evaluering.sats)
            assertEquals("B  hjemmel", evaluering.hjemmelbokstav)
            assertEquals(YearMonth.of(2026, 8), evaluering.virkningstidspunkt)
            assertEquals(emptySet(), evaluering.motstridendeFelt)
        }
    }

    @Test
    fun `to GS-saker i samme måned der bare én rad har posisjon 100 gir DIAGNOSE_2 i motstridendeFelt`() {
        lagreVedtakMedSak(100L, saksnummer = "123")
        lagreVedtakMedSak(200L, saksnummer = "124")
        lagreEvValgForSak(sak = "A123", måned = oktober)
        lagreEvValgForSak(sak = "A124", måned = oktober)
        lagreEvDataForMåned(oktober, femPosisjoner() + femPosisjoner().filterNot { it.first == "100" })

        val evalueringer = evalueringService.hentEvalueringerForVedtak(setOf(100L, 200L))

        assertEquals(setOf(100L, 200L), evalueringer.keys)
        evalueringer.values.forEach { evaluering ->
            assertEquals("G400", evaluering.diagnose1)
            assertNull(evaluering.diagnose2)
            assertEquals(setOf(Ev10Posisjon.DIAGNOSE_2), evaluering.motstridendeFelt)
        }
    }

    @Test
    fun `lik tekst i samme posisjon slås sammen`() {
        lagreVedtakMedSak(100L)
        lagreEvValgForSak(sak = "A123", måned = oktober)
        lagreEvDataForMåned(oktober, femPosisjoner() + ("055" to "G400 diagnose"))

        val evaluering = hentEvaluering(100L)

        assertEquals("G400", evaluering.diagnose1)
        assertEquals(emptySet(), evaluering.motstridendeFelt)
    }

    @Test
    fun `ulik tekst i samme posisjon gir motstridende felt`() {
        lagreVedtakMedSak(100L)
        lagreEvValgForSak(sak = "A123", måned = oktober)
        lagreEvDataForMåned(oktober, femPosisjoner() + ("055" to "G401 diagnose"))

        val evaluering = hentEvaluering(100L)

        assertNull(evaluering.diagnose1)
        assertEquals(setOf(Ev10Posisjon.DIAGNOSE_1), evaluering.motstridendeFelt)
        assertEquals("G401", evaluering.diagnose2)
        assertEquals("0123 sats", evaluering.sats)
        assertEquals("B  hjemmel", evaluering.hjemmelbokstav)
        assertEquals(YearMonth.of(2026, 8), evaluering.virkningstidspunkt)
    }

    @Test
    fun `posisjoner utenfor Ev10Posisjon hentes ikke`() {
        lagreVedtakMedSak(100L)
        lagreEvaluering(sak = "A123", måned = oktober)
        val før = hentEvaluering(100L)

        lagreEvDataForMåned(oktober, listOf("050" to "F050 felt", "999" to "G400 ukjent"))
        val etter = hentEvaluering(100L)

        assertEquals(før, etter)
        assertEquals(emptySet(), etter.motstridendeFelt)
    }

    @Test
    fun `tomt sett med vedtak gir tomt kart`() {
        lagreVedtakMedSak(100L)
        lagreEvaluering(sak = "A123", måned = oktober)

        assertEquals(emptyMap<Long, InfotrygdEvaluering>(), evalueringService.hentEvalueringerForVedtak(emptySet()))
    }

    private fun hentEvaluering(vedtakId: Long): InfotrygdEvaluering = evalueringService.hentEvalueringerForVedtak(setOf(vedtakId)).getValue(vedtakId)

    /** Personen, stønaden og vedtaket, og GH-saken i sa_sak_10 som vedtaket hører til. */
    private fun lagreVedtakMedSak(
        vedtakId: Long,
        saksblokk: String = "A",
        saksnummer: String = "123",
        stønadstype: Stønadstype = Stønadstype.GRUNNSTØNAD,
        saksnummerISak: String = saksnummer,
    ) {
        lagreVedtak(vedtakId, saksblokk, saksnummer, stønadstype)
        lagreSak(saksblokk, saksnummerISak, stønadstype)
    }

    /** Personen, stønaden og vedtaket. Saken lagres med [lagreSak]. */
    private fun lagreVedtak(
        vedtakId: Long,
        saksblokk: String = "A",
        saksnummer: String = "123",
        stønadstype: Stønadstype = Stønadstype.GRUNNSTØNAD,
    ) {
        testdata.lagreTLopenrFnr("1", fnr)
        testdata.lagreTStonad(vedtakId.toString(), "1", stønadstype.kodeRutine)
        testdata.lagreTVedtak(vedtakId.toString(), vedtakId.toString(), "1", stønadstype.kodeRutine, saksblokk, saksnummer)
    }

    /** GH-saken i sa_sak_10 for personen. [saksnummer] er s10_saksnr, som kan ha ledende null. */
    private fun lagreSak(
        saksblokk: String = "A",
        saksnummer: String = "123",
        stønadstype: Stønadstype = Stønadstype.GRUNNSTØNAD,
    ) {
        testdata.lagreSaSak10(
            id = (nesteSakId++).toString(),
            reversertFnr = reversertFnr,
            saksblokk = saksblokk,
            saksnummer = saksnummer,
            valg = stønadstype.kodeRutine,
        )
    }

    /** Raden i ev_valg_05 for saken [sak], som er saksblokk og saksnummer slått sammen, slik ev05_sak er bygd. */
    private fun lagreEvValgForSak(
        sak: String,
        måned: YearMonth,
        stønadstype: Stønadstype = Stønadstype.GRUNNSTØNAD,
    ) {
        testdata.lagreEvValg05(
            id = (neste05Id++).toString(),
            personkey = personkey,
            årMånedSekvens = ReplikaTestdata.ev05Sekvens(måned),
            stønadstype = stønadstype.kodeRutine,
            reversertFnr = reversertFnr,
            sak = sak,
        )
    }

    /** Rader i ev_data_10 for personen i [måned]. Raden har ingen sak, bare personkey og måned. */
    private fun lagreEvDataForMåned(
        måned: YearMonth,
        felt: List<Pair<String, String>>,
    ) {
        felt.forEach { (posisjon, tekst) ->
            testdata.lagreEvData10(
                id = (neste10Id++).toString(),
                personkey = personkey,
                årMånedSekvens = ReplikaTestdata.ev05Sekvens(måned),
                tekst = tekst,
                posisjon = posisjon,
            )
        }
    }

    /** Én GH-sak med evaluering i [måned]. [felt] er alle posisjonene vi vil ha i ev_data_10. */
    private fun lagreEvaluering(
        sak: String,
        måned: YearMonth,
        felt: List<Pair<String, String>> = femPosisjoner(),
    ) {
        lagreEvValgForSak(sak, måned)
        lagreEvDataForMåned(måned, felt)
    }

    /** De fem posisjonene i Ev10Posisjon, med [diagnose] i posisjon 055. */
    private fun femPosisjoner(diagnose: String = "G400 diagnose"): List<Pair<String, String>> =
        listOf(
            "055" to diagnose,
            "100" to "G401 annen",
            "150" to "0123 sats",
            "160" to "B  hjemmel",
            "165" to "0826",
        )
}
