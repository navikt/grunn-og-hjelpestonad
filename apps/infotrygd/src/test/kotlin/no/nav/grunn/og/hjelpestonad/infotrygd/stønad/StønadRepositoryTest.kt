package no.nav.grunn.og.hjelpestonad.infotrygd.stønad

import no.nav.grunn.og.hjelpestonad.infotrygd.Application
import no.nav.grunn.og.hjelpestonad.infotrygd.ReplikaTestdata
import no.nav.grunn.og.hjelpestonad.infotrygd.exodus.ExodusTabell
import no.nav.grunn.og.hjelpestonad.infotrygd.exodus.ExodusUpsertRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import kotlin.test.assertEquals

@SpringBootTest(classes = [Application::class])
@ActiveProfiles("test")
class StønadRepositoryTest {
    @Autowired
    private lateinit var stønadRepository: StønadRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var upsertRepository: ExodusUpsertRepository

    private val testdata by lazy { ReplikaTestdata(jdbcTemplate, upsertRepository) }

    private val dagensDato = LocalDate.of(2026, 1, 1)

    @BeforeEach
    fun tømTabeller() {
        testdata.tømAlle()
    }

    private fun lagreVedtak(
        personLøpenr: String,
        stønadId: String,
        vedtakId: String,
        kodeRutine: String,
        tom: String?,
        opphør: String? = null,
        endringskode: String = "F",
        oppdragId: String? = "1",
    ) {
        testdata.lagre(
            ExodusTabell.T_STONAD,
            "stonad_id" to stønadId,
            "person_lopenr" to personLøpenr,
            "kode_rutine" to kodeRutine,
            "dato_opphor" to opphør,
            "oppdrag_id" to oppdragId,
        )
        testdata.lagre(
            ExodusTabell.T_VEDTAK,
            "vedtak_id" to vedtakId,
            "stonad_id" to stønadId,
            "person_lopenr" to personLøpenr,
            "kode_rutine" to kodeRutine,
            "dato_innv_tom" to tom,
        )
        testdata.lagre(ExodusTabell.T_ENDRING, "vedtak_id" to vedtakId, "kode" to endringskode)
    }

    @Test
    fun `finner vedtak per stønadstype og om de er løpende`() {
        testdata.lagre(ExodusTabell.T_LOPENR_FNR, "person_lopenr" to "1", "personnr" to "01017012345")
        lagreVedtak("1", stønadId = "10", vedtakId = "100", kodeRutine = "GS", tom = null)
        lagreVedtak("1", stønadId = "11", vedtakId = "110", kodeRutine = "HS", tom = "2025-06-30")

        val treff = stønadRepository.finnVedtakstreff(setOf("01017012345"), dagensDato)

        assertEquals(
            listOf(
                VedtakstreffResponse("01017012345", Stønadstype.GRUNNSTØNAD, harLøpendeVedtak = true),
                VedtakstreffResponse("01017012345", Stønadstype.HJELPESTØNAD, harLøpendeVedtak = false),
            ),
            treff,
        )
    }

    @Test
    fun `opphørsdato på stønaden gjelder foran sluttdato på vedtaket`() {
        testdata.lagre(ExodusTabell.T_LOPENR_FNR, "person_lopenr" to "1", "personnr" to "01017012345")
        lagreVedtak("1", stønadId = "10", vedtakId = "100", kodeRutine = "GS", tom = null, opphør = "2025-12-31")

        val treff = stønadRepository.finnVedtakstreff(setOf("01017012345"), dagensDato)

        assertEquals(listOf(VedtakstreffResponse("01017012345", Stønadstype.GRUNNSTØNAD, harLøpendeVedtak = false)), treff)
    }

    @Test
    fun `ignorerer andre stønader og andre personer`() {
        testdata.lagre(ExodusTabell.T_LOPENR_FNR, "person_lopenr" to "1", "personnr" to "01017012345")
        testdata.lagre(ExodusTabell.T_LOPENR_FNR, "person_lopenr" to "2", "personnr" to "02027012345")
        lagreVedtak("1", stønadId = "10", vedtakId = "100", kodeRutine = "EO", tom = null)
        lagreVedtak("2", stønadId = "20", vedtakId = "200", kodeRutine = "GS", tom = null)

        assertEquals(emptyList(), stønadRepository.finnVedtakstreff(setOf("01017012345"), dagensDato))
    }

    private fun lagrePerson(
        personLøpenr: String,
        personnr: String,
    ) = testdata.lagre(ExodusTabell.T_LOPENR_FNR, "person_lopenr" to personLøpenr, "personnr" to personnr)

    @Test
    fun `migreringspersoner er de der siste vedtak på stønaden løper forbi starten av neste måned`() {
        lagrePerson("1", "01017012345")
        lagrePerson("2", "02027012345")
        lagrePerson("3", "03037012345")
        lagrePerson("4", "04047012345")
        lagrePerson("5", "05057012345")
        lagreVedtak("1", stønadId = "10", vedtakId = "100", kodeRutine = "GS", tom = null)
        lagreVedtak("2", stønadId = "20", vedtakId = "200", kodeRutine = "HS", tom = "2026-12-31")
        lagreVedtak("3", stønadId = "30", vedtakId = "300", kodeRutine = "GS", tom = "2026-02-01")
        lagreVedtak("4", stønadId = "40", vedtakId = "400", kodeRutine = "GS", tom = null)
        lagreVedtak("4", stønadId = "40", vedtakId = "401", kodeRutine = "GS", tom = "2026-01-31")
        lagreVedtak("5", stønadId = "50", vedtakId = "500", kodeRutine = "GS", tom = null, opphør = "2026-01-31")

        assertEquals(setOf("01017012345", "02027012345"), stønadRepository.finnPersonerForMigrering(antall = 10, dagensDato))
    }

    @Test
    fun `migreringspersoner utelater annullerte og uavklarte vedtak, stønader uten oppdrag og andre stønadstyper`() {
        lagrePerson("1", "01017012345")
        lagrePerson("2", "02027012345")
        lagrePerson("3", "03037012345")
        lagrePerson("4", "04047012345")
        lagreVedtak("1", stønadId = "10", vedtakId = "100", kodeRutine = "GS", tom = null, endringskode = "AN")
        lagreVedtak("2", stønadId = "20", vedtakId = "200", kodeRutine = "GS", tom = null, endringskode = "UA")
        lagreVedtak("3", stønadId = "30", vedtakId = "300", kodeRutine = "GS", tom = null, oppdragId = null)
        lagreVedtak("4", stønadId = "40", vedtakId = "400", kodeRutine = "EO", tom = null)

        assertEquals(emptySet(), stønadRepository.finnPersonerForMigrering(antall = 10, dagensDato))
    }

    @Test
    fun `migreringspersoner begrenses til oppgitt antall`() {
        lagrePerson("1", "01017012345")
        lagrePerson("2", "02027012345")
        lagreVedtak("1", stønadId = "10", vedtakId = "100", kodeRutine = "GS", tom = null)
        lagreVedtak("2", stønadId = "20", vedtakId = "200", kodeRutine = "GS", tom = null)

        assertEquals(setOf("01017012345"), stønadRepository.finnPersonerForMigrering(antall = 1, dagensDato))
    }
}
