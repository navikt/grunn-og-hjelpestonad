package no.nav.grunn.og.hjelpestonad.infotrygd.periode

import no.nav.grunn.og.hjelpestonad.infotrygd.Application
import no.nav.grunn.og.hjelpestonad.infotrygd.ReplikaTestdata
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
import kotlin.test.assertEquals
import kotlin.test.assertNull

@SpringBootTest(classes = [Application::class])
@ActiveProfiles("test")
class PeriodeServiceTest {
    @Autowired
    private lateinit var periodeService: PeriodeService

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var upsertRepository: ExodusUpsertRepository

    private val testdata by lazy { ReplikaTestdata(jdbcTemplate, upsertRepository) }

    private val personident = "01017012345"

    @BeforeEach
    fun tømTabeller() {
        testdata.tømAlle()
        testdata.lagre(ExodusTabell.T_LOPENR_FNR, "person_lopenr" to "1", "personnr" to personident)
    }

    private fun lagreVedtak(
        stønadId: String,
        vedtakId: String,
        kodeRutine: String,
        fom: String = "2020-01-01",
        tom: String? = null,
        endringskode: String = "F",
        oppdragId: String? = "1",
        personLøpenr: String = "1",
    ) {
        testdata.lagre(
            ExodusTabell.T_STONAD,
            "stonad_id" to stønadId,
            "person_lopenr" to personLøpenr,
            "kode_rutine" to kodeRutine,
            "dato_opphor" to null,
            "oppdrag_id" to oppdragId,
        )
        testdata.lagre(
            ExodusTabell.T_VEDTAK,
            "vedtak_id" to vedtakId,
            "stonad_id" to stønadId,
            "person_lopenr" to personLøpenr,
            "kode_rutine" to kodeRutine,
            "type_sak" to "S ",
            "kode_resultat" to "I ",
            "dato_innv_fom" to fom,
            "dato_innv_tom" to tom,
        )
        testdata.lagre(ExodusTabell.T_ENDRING, "vedtak_id" to vedtakId, "kode" to endringskode)
    }

    private fun lagreDelytelse(
        vedtakId: String,
        fom: String,
        tom: String?,
        beløp: String = "1000.50",
        type: String = "GS",
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

    private fun hentPerioder() = periodeService.hentPerioder(PeriodeRequest(setOf(personident)))

    @Test
    fun `grupperer delytelser per stønadstype`() {
        lagreVedtak(stønadId = "10", vedtakId = "100", kodeRutine = "GS")
        lagreDelytelse("100", fom = "2020-01-01", tom = null)
        lagreVedtak(stønadId = "11", vedtakId = "110", kodeRutine = "HS", tom = "2021-12-31")
        lagreDelytelse("110", fom = "2020-01-01", tom = "2021-12-31", type = "HS")

        val perioder = hentPerioder()

        val grunnstønad = perioder.getValue(Stønadstype.GRUNNSTØNAD).single()
        assertEquals(personident, grunnstønad.personident)
        assertEquals(100L, grunnstønad.vedtakId)
        assertEquals("S", grunnstønad.sakstype)
        assertEquals("I", grunnstønad.vedtakKodeResultat)
        assertEquals("F", grunnstønad.kode)
        assertEquals(BigDecimal("1000.50"), grunnstønad.beløp)
        assertNull(grunnstønad.stønadTom)
        assertNull(grunnstønad.innvilgetTom)
        assertEquals(listOf(110L), perioder.getValue(Stønadstype.HJELPESTØNAD).map { it.vedtakId })
    }

    @Test
    fun `gir én periode per delytelse, sortert med nyeste fom først`() {
        lagreVedtak(stønadId = "10", vedtakId = "100", kodeRutine = "GS")
        lagreDelytelse("100", fom = "2020-01-01", tom = "2020-12-31", tidspunktReg = "2020-01-01T00:00")
        lagreDelytelse("100", fom = "2021-01-01", tom = null, tidspunktReg = "2021-01-01T00:00")

        val perioder = hentPerioder().getValue(Stønadstype.GRUNNSTØNAD)

        assertEquals(listOf(LocalDate.of(2021, 1, 1), LocalDate.of(2020, 1, 1)), perioder.map { it.stønadFom })
    }

    @Test
    fun `gir én periode per endringskode på vedtaket`() {
        lagreVedtak(stønadId = "10", vedtakId = "100", kodeRutine = "GS")
        testdata.lagre(ExodusTabell.T_ENDRING, "vedtak_id" to "100", "kode" to "O ")
        lagreDelytelse("100", fom = "2020-01-01", tom = null)

        val perioder = hentPerioder().getValue(Stønadstype.GRUNNSTØNAD)

        assertEquals(setOf("F", "O"), perioder.map { it.kode }.toSet())
    }

    @Test
    fun `henter alle registreringer i t_gh for vedtaket, nyeste først`() {
        lagreVedtak(stønadId = "10", vedtakId = "100", kodeRutine = "GS")
        lagreDelytelse("100", fom = "2020-01-01", tom = null)
        testdata.lagre(ExodusTabell.T_GH, "vedtak_id" to "100", "tidspunkt_reg" to "2020-01-01T00:00", "trygdetid" to "20", "sats_gs" to "1")
        testdata.lagre(ExodusTabell.T_GH, "vedtak_id" to "100", "tidspunkt_reg" to "2021-01-01T00:00", "trygdetid" to "40", "sats_gs" to "2")
        testdata.lagre(ExodusTabell.T_GH, "vedtak_id" to "999", "tidspunkt_reg" to "2021-01-01T00:00", "trygdetid" to "10")

        val periode = hentPerioder().getValue(Stønadstype.GRUNNSTØNAD).single()

        assertEquals(listOf(40L, 20L), periode.trygdetidOgSats.map { it.trygdetid })
        assertEquals(listOf(2L, 1L), periode.trygdetidOgSats.map { it.satsGs })
        assertEquals(LocalDateTime.of(2021, 1, 1, 0, 0), periode.trygdetidOgSats.first().tidspunktRegistrert)
    }

    @Test
    fun `mangler t_gh gir tom liste`() {
        lagreVedtak(stønadId = "10", vedtakId = "100", kodeRutine = "GS")
        lagreDelytelse("100", fom = "2020-01-01", tom = null)

        val periode = hentPerioder().getValue(Stønadstype.GRUNNSTØNAD).single()

        assertEquals(emptyList(), periode.trygdetidOgSats)
    }

    @Test
    fun `filtrerer bort perioder uten oppdrag med beløp, men beholder 0 kr`() {
        lagreVedtak(stønadId = "10", vedtakId = "100", kodeRutine = "GS", oppdragId = null)
        lagreDelytelse("100", fom = "2020-01-01", tom = null, beløp = "1000")
        lagreVedtak(stønadId = "11", vedtakId = "110", kodeRutine = "GS", oppdragId = null)
        lagreDelytelse("110", fom = "2020-01-01", tom = null, beløp = "0")

        assertEquals(listOf(110L), hentPerioder().getValue(Stønadstype.GRUNNSTØNAD).map { it.vedtakId })
    }

    @Test
    fun `filtrerer bort vedtak med tom før eller lik fom, andre stønader og andre personer`() {
        testdata.lagre(ExodusTabell.T_LOPENR_FNR, "person_lopenr" to "2", "personnr" to "02027012345")
        lagreVedtak(stønadId = "10", vedtakId = "100", kodeRutine = "GS", fom = "2020-01-01", tom = "2020-01-01")
        lagreDelytelse("100", fom = "2020-01-01", tom = null)
        lagreVedtak(stønadId = "11", vedtakId = "110", kodeRutine = "EO")
        lagreDelytelse("110", fom = "2020-01-01", tom = null)
        lagreVedtak(stønadId = "12", vedtakId = "120", kodeRutine = "GS", personLøpenr = "2")
        lagreDelytelse("120", fom = "2020-01-01", tom = null)

        assertEquals(emptyMap(), hentPerioder())
    }

    @Test
    fun `henter kun oppgitte stønadstyper`() {
        lagreVedtak(stønadId = "10", vedtakId = "100", kodeRutine = "GS")
        lagreDelytelse("100", fom = "2020-01-01", tom = null)
        lagreVedtak(stønadId = "11", vedtakId = "110", kodeRutine = "HS")
        lagreDelytelse("110", fom = "2020-01-01", tom = null, type = "HS")

        val perioder = periodeService.hentPerioder(PeriodeRequest(setOf(personident), setOf(Stønadstype.HJELPESTØNAD)))

        assertEquals(setOf(Stønadstype.HJELPESTØNAD), perioder.keys)
    }
}
