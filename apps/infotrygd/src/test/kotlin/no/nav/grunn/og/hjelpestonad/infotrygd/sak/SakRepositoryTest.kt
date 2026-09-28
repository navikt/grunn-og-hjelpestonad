package no.nav.grunn.og.hjelpestonad.infotrygd.sak

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
import kotlin.test.assertNull

@SpringBootTest(classes = [Application::class])
@ActiveProfiles("test")
class SakRepositoryTest {
    @Autowired
    private lateinit var sakRepository: SakRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var upsertRepository: ExodusUpsertRepository

    private val testdata by lazy { ReplikaTestdata(jdbcTemplate, upsertRepository) }

    @BeforeEach
    fun lagreSaker() {
        testdata.tømAlle()
        // F_NR er lagret reversert (ÅÅMMDDPPPPP) i sa_sak_10.
        testdata.lagre(
            ExodusTabell.SA_SAK_10,
            "id_sak" to "1",
            "f_nr" to "70040312345",
            "s10_kapittelnr" to "GH",
            "s10_valg" to "GS",
            "s10_resultat" to "I ",
            "s10_reg_dato" to "1012011",
            "s10_vedtaksdato" to "0",
        )
        testdata.lagre(ExodusTabell.SA_SAK_10, "id_sak" to "2", "f_nr" to "70040312345", "s10_kapittelnr" to "GH", "s10_valg" to "GS")
        testdata.lagre(ExodusTabell.SA_SAK_10, "id_sak" to "3", "f_nr" to "80050612345", "s10_kapittelnr" to "GH", "s10_valg" to "HS")
    }

    @Test
    fun `finnSaker reverserer fnr og leser datoer`() {
        val saker = sakRepository.finnSaker(setOf("03047012345"))

        assertEquals(listOf(1L, 2L), saker.map { it.id })
        val sak = saker.first()
        assertEquals("03047012345", sak.personident)
        assertEquals("GH", sak.kapittelnr)
        assertEquals("I", sak.resultat)
        assertEquals(LocalDate.of(2011, 1, 1), sak.registrertDato)
        assertNull(sak.vedtaksdato)
    }

    @Test
    fun `finnesSaker gir ett treff per kapittel og valg`() {
        assertEquals(
            listOf(Saktreff("03047012345", "GH", "GS"), Saktreff("06058012345", "GH", "HS")),
            sakRepository.finnesSaker(setOf("03047012345", "06058012345")),
        )
    }
}
