package no.nav.grunn.og.hjelpestonad.infotrygd.stønad

import no.nav.grunn.og.hjelpestonad.infotrygd.Application
import no.nav.grunn.og.hjelpestonad.infotrygd.ReplikaTestdata
import no.nav.grunn.og.hjelpestonad.infotrygd.exodus.ExodusTabell
import no.nav.grunn.og.hjelpestonad.infotrygd.exodus.ExodusUpsertRepository
import no.nav.grunn.og.hjelpestonad.infotrygd.util.reverserFnr
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.assertEquals

@SpringBootTest(classes = [Application::class])
@ActiveProfiles("test")
class FørstegangsvedtakForMigreringTest {
    @Autowired
    private lateinit var stønadRepository: StønadRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var upsertRepository: ExodusUpsertRepository

    private val testdata by lazy { ReplikaTestdata(jdbcTemplate, upsertRepository) }

    // ev_valg_05, ev_data_10 og sa_sak_10 har primærnøkkel på id, og upsert overskriver ved samme id, så hver rad må ha egen id.
    private var nesteRadId = 1000L

    private val dagensDato = LocalDate.of(2026, 1, 1)
    private val september = YearMonth.of(2025, 9)
    private val oktober = YearMonth.of(2025, 10)

    private val fnrPerson1 = "01017012345"
    private val fnrPerson2 = "02027012345"

    @BeforeEach
    fun tømTabeller() {
        testdata.tømAlle()
    }

    @Test
    fun `finner person med ett førstegangsvedtak, riktig sats og riktig hjemmelbokstav`() {
        lagreFørstegangsvedtakMedEvaluering(personLøpenr = "1", fnr = fnrPerson1, stønadId = "10", vedtakId = "100")

        assertEquals(setOf(fnrPerson1), finn(sats = 3, hjemmelbokstav = "F"))
    }

    @Test
    fun `stønad med et senere vedtak er ikke med`() {
        lagreFørstegangsvedtakMedEvaluering(personLøpenr = "1", fnr = fnrPerson1, stønadId = "10", vedtakId = "100")
        lagreVedtak(personLøpenr = "1", stønadId = "10", vedtakId = "101", endringskode = "S")

        assertEquals(emptySet(), finn(sats = 3, hjemmelbokstav = "F"))
    }

    @Test
    fun `annullert og uavklart vedtak teller ikke som senere vedtak`() {
        lagreFørstegangsvedtakMedEvaluering(personLøpenr = "1", fnr = fnrPerson1, stønadId = "10", vedtakId = "100")
        lagreVedtak(personLøpenr = "1", stønadId = "10", vedtakId = "101", endringskode = "AN")
        lagreVedtak(personLøpenr = "1", stønadId = "10", vedtakId = "102", endringskode = "UA")

        assertEquals(setOf(fnrPerson1), finn(sats = 3, hjemmelbokstav = "F"))
    }

    @Test
    fun `eneste vedtak uten endringskode F er ikke med`() {
        lagreFørstegangsvedtakMedEvaluering(personLøpenr = "1", fnr = fnrPerson1, stønadId = "10", vedtakId = "100", endringskode = "S")

        assertEquals(emptySet(), finn(sats = 3, hjemmelbokstav = "F"))
    }

    @Test
    fun `annen sats er ikke med`() {
        lagreFørstegangsvedtakMedEvaluering(personLøpenr = "1", fnr = fnrPerson1, stønadId = "10", vedtakId = "100", sats = "2")

        assertEquals(emptySet(), finn(sats = 3, hjemmelbokstav = "F"))
    }

    @Test
    fun `vedtak uten t_gh er ikke med`() {
        lagreFørstegangsvedtakMedEvaluering(personLøpenr = "1", fnr = fnrPerson1, stønadId = "10", vedtakId = "100", sats = null)

        assertEquals(emptySet(), finn(sats = 3, hjemmelbokstav = "F"))
    }

    @Test
    fun `annen hjemmelbokstav er ikke med`() {
        lagreFørstegangsvedtakMedEvaluering(personLøpenr = "1", fnr = fnrPerson1, stønadId = "10", vedtakId = "100", hjemmelbokstav = "B")

        assertEquals(emptySet(), finn(sats = 3, hjemmelbokstav = "F"))
    }

    @Test
    fun `hjemmelbokstav med mellomrom rundt gir treff`() {
        lagreFørstegangsvedtakMedEvaluering(personLøpenr = "1", fnr = fnrPerson1, stønadId = "10", vedtakId = "100", hjemmelbokstav = " F ")

        assertEquals(setOf(fnrPerson1), finn(sats = 3, hjemmelbokstav = "F"))
    }

    @Test
    fun `hjemmelbokstaven leses fra siste evaluering for saken`() {
        lagreFørstegangsvedtakMedEvaluering(
            personLøpenr = "1",
            fnr = fnrPerson1,
            stønadId = "10",
            vedtakId = "100",
            hjemmelbokstav = "F",
            evalueringsmåned = oktober,
        )
        lagreEvaluering(personkey = "1", fnr = fnrPerson1, sak = "A10", måned = september, hjemmelbokstav = "B")

        assertEquals(setOf(fnrPerson1), finn(sats = 3, hjemmelbokstav = "F"))
        assertEquals(emptySet(), finn(sats = 3, hjemmelbokstav = "B"))
    }

    @Test
    fun `person med løpende hjelpestønad er ikke med`() {
        lagreFørstegangsvedtakMedEvaluering(personLøpenr = "1", fnr = fnrPerson1, stønadId = "10", vedtakId = "100")
        lagreVedtak(personLøpenr = "1", stønadId = "11", vedtakId = "110", kodeRutine = "HS")

        assertEquals(emptySet(), finn(sats = 3, hjemmelbokstav = "F"))
    }

    @Test
    fun `person med opphørt hjelpestønad er med`() {
        lagreFørstegangsvedtakMedEvaluering(personLøpenr = "1", fnr = fnrPerson1, stønadId = "10", vedtakId = "100")
        lagreVedtak(personLøpenr = "1", stønadId = "11", vedtakId = "110", kodeRutine = "HS", opphør = "2025-06-30")

        assertEquals(setOf(fnrPerson1), finn(sats = 3, hjemmelbokstav = "F"))
    }

    @Test
    fun `to GH-saker i evalueringsmåneden gjør hjemmelbokstaven tvetydig, og personen er ikke med`() {
        lagreFørstegangsvedtakMedEvaluering(personLøpenr = "1", fnr = fnrPerson1, stønadId = "10", vedtakId = "100")
        // En annen GH-sak, for eksempel en avsluttet hjelpestønad, er evaluert samme måned. ev_data_10 har ingen sak.
        lagreEvValg(personkey = "1", fnr = fnrPerson1, sak = "B99", måned = oktober, stønadstype = "HS")

        assertEquals(emptySet(), finn(sats = 3, hjemmelbokstav = "F"))
    }

    @Test
    fun `ulike hjemmelbokstaver i samme evalueringsmåned gir ikke treff`() {
        lagreFørstegangsvedtakMedEvaluering(personLøpenr = "1", fnr = fnrPerson1, stønadId = "10", vedtakId = "100")
        lagreEvData(personkey = "1", måned = oktober, hjemmelbokstav = "2")

        assertEquals(emptySet(), finn(sats = 3, hjemmelbokstav = "F"))
    }

    @Test
    fun `stønad som ikke er løpende fra neste måned er ikke med`() {
        lagreFørstegangsvedtakMedEvaluering(
            personLøpenr = "1",
            fnr = fnrPerson1,
            stønadId = "10",
            vedtakId = "100",
            opphør = "2026-01-31",
        )

        assertEquals(emptySet(), finn(sats = 3, hjemmelbokstav = "F"))
    }

    @Test
    fun `personene sorteres på stonad_id og begrenses til antall`() {
        lagreFørstegangsvedtakMedEvaluering(personLøpenr = "1", fnr = fnrPerson1, stønadId = "20", vedtakId = "200")
        lagreFørstegangsvedtakMedEvaluering(personLøpenr = "2", fnr = fnrPerson2, stønadId = "10", vedtakId = "100")

        assertEquals(setOf(fnrPerson2), finn(sats = 3, hjemmelbokstav = "F", antall = 1))
    }

    private fun finn(
        sats: Int,
        hjemmelbokstav: String,
        antall: Int = 10,
    ): Set<String> =
        stønadRepository.finnPersonerMedFørstegangsvedtakForMigrering(
            antall = antall,
            sats = sats,
            hjemmelbokstav = hjemmelbokstav,
            dagensDato = dagensDato,
        )

    /**
     * Personen, en løpende GS-stønad med ett vedtak, sats i t_gh, GH-saken i sa_sak_10 og en EVGH-evaluering
     * med hjemmelbokstaven i posisjon 160. Saken er saksblokk A og saksnummer [stønadId]. Personkey er
     * [personLøpenr]. [sats] null lagrer ingen rad i t_gh.
     */
    private fun lagreFørstegangsvedtakMedEvaluering(
        personLøpenr: String,
        fnr: String,
        stønadId: String,
        vedtakId: String,
        sats: String? = "3",
        hjemmelbokstav: String = "F",
        endringskode: String = "F",
        opphør: String? = null,
        evalueringsmåned: YearMonth = oktober,
    ) {
        testdata.lagreTLopenrFnr(personLøpenr, fnr)
        lagreVedtak(personLøpenr, stønadId, vedtakId, endringskode = endringskode, opphør = opphør)
        if (sats != null) {
            testdata.lagre(ExodusTabell.T_GH, "vedtak_id" to vedtakId, "sats_gs" to sats, "trygdetid" to "40")
        }
        testdata.lagreSaSak10(
            id = (nesteRadId++).toString(),
            reversertFnr = fnr.reverserFnr(),
            saksblokk = "A",
            saksnummer = stønadId,
        )
        lagreEvaluering(personLøpenr, fnr, sak = "A$stønadId", måned = evalueringsmåned, hjemmelbokstav = hjemmelbokstav)
    }

    private fun lagreVedtak(
        personLøpenr: String,
        stønadId: String,
        vedtakId: String,
        kodeRutine: String = "GS",
        endringskode: String = "F",
        opphør: String? = null,
    ) {
        testdata.lagre(
            ExodusTabell.T_STONAD,
            "stonad_id" to stønadId,
            "person_lopenr" to personLøpenr,
            "kode_rutine" to kodeRutine,
            "dato_opphor" to opphør,
            "oppdrag_id" to "1",
        )
        testdata.lagre(
            ExodusTabell.T_VEDTAK,
            "vedtak_id" to vedtakId,
            "stonad_id" to stønadId,
            "person_lopenr" to personLøpenr,
            "kode_rutine" to kodeRutine,
            "saksblokk" to "A",
            "saksnr" to stønadId,
            "dato_innv_tom" to null,
        )
        testdata.lagreTEndring(vedtakId, endringskode)
    }

    private fun lagreEvaluering(
        personkey: String,
        fnr: String,
        sak: String,
        måned: YearMonth,
        hjemmelbokstav: String,
    ) {
        lagreEvValg(personkey, fnr, sak, måned)
        lagreEvData(personkey, måned, hjemmelbokstav)
    }

    private fun lagreEvValg(
        personkey: String,
        fnr: String,
        sak: String,
        måned: YearMonth,
        stønadstype: String = "GS",
    ) = testdata.lagreEvValg05(
        id = (nesteRadId++).toString(),
        personkey = personkey,
        årMånedSekvens = ReplikaTestdata.ev05Sekvens(måned),
        stønadstype = stønadstype,
        reversertFnr = fnr.reverserFnr(),
        sak = sak,
    )

    private fun lagreEvData(
        personkey: String,
        måned: YearMonth,
        hjemmelbokstav: String,
    ) = testdata.lagreEvData10(
        id = (nesteRadId++).toString(),
        personkey = personkey,
        årMånedSekvens = ReplikaTestdata.ev05Sekvens(måned),
        tekst = hjemmelbokstav,
        posisjon = "160",
    )
}
