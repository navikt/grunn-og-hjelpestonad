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
    ) {
        testdata.lagre(ExodusTabell.T_STONAD, "stonad_id" to stønadId, "person_lopenr" to personLøpenr, "kode_rutine" to kodeRutine, "dato_opphor" to opphør)
        testdata.lagre(
            ExodusTabell.T_VEDTAK,
            "vedtak_id" to vedtakId,
            "stonad_id" to stønadId,
            "person_lopenr" to personLøpenr,
            "kode_rutine" to kodeRutine,
            "dato_innv_tom" to tom,
        )
    }

    @Test
    fun `finner vedtak per stønadstype og om de er løpende`() {
        testdata.lagre(ExodusTabell.T_LOPENR_FNR, "person_lopenr" to "1", "personnr" to "01017012345")
        lagreVedtak("1", stønadId = "10", vedtakId = "100", kodeRutine = "GS", tom = null)
        lagreVedtak("1", stønadId = "11", vedtakId = "110", kodeRutine = "HS", tom = "2025-06-30")

        val treff = stønadRepository.finnVedtakstreff(setOf("01017012345"), dagensDato)

        assertEquals(
            listOf(
                Vedtakstreff("01017012345", Stønadstype.GRUNNSTØNAD, harLøpendeVedtak = true),
                Vedtakstreff("01017012345", Stønadstype.HJELPESTØNAD, harLøpendeVedtak = false),
            ),
            treff,
        )
    }

    @Test
    fun `opphørsdato på stønaden gjelder foran sluttdato på vedtaket`() {
        testdata.lagre(ExodusTabell.T_LOPENR_FNR, "person_lopenr" to "1", "personnr" to "01017012345")
        lagreVedtak("1", stønadId = "10", vedtakId = "100", kodeRutine = "GS", tom = null, opphør = "2025-12-31")

        val treff = stønadRepository.finnVedtakstreff(setOf("01017012345"), dagensDato)

        assertEquals(listOf(Vedtakstreff("01017012345", Stønadstype.GRUNNSTØNAD, harLøpendeVedtak = false)), treff)
    }

    @Test
    fun `ignorerer andre stønader og andre personer`() {
        testdata.lagre(ExodusTabell.T_LOPENR_FNR, "person_lopenr" to "1", "personnr" to "01017012345")
        testdata.lagre(ExodusTabell.T_LOPENR_FNR, "person_lopenr" to "2", "personnr" to "02027012345")
        lagreVedtak("1", stønadId = "10", vedtakId = "100", kodeRutine = "EO", tom = null)
        lagreVedtak("2", stønadId = "20", vedtakId = "200", kodeRutine = "GS", tom = null)

        assertEquals(emptyList(), stønadRepository.finnVedtakstreff(setOf("01017012345"), dagensDato))
    }
}
