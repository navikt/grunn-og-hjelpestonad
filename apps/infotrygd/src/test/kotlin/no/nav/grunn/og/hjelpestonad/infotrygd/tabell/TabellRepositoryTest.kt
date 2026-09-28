package no.nav.grunn.og.hjelpestonad.infotrygd.tabell

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
import kotlin.test.assertEquals

@SpringBootTest(classes = [Application::class])
@ActiveProfiles("test")
class TabellRepositoryTest {
    @Autowired
    private lateinit var tabellRepository: TabellRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var upsertRepository: ExodusUpsertRepository

    private val testdata by lazy { ReplikaTestdata(jdbcTemplate, upsertRepository) }

    @BeforeEach
    fun tømTabeller() {
        testdata.tømAlle()
    }

    @Test
    fun `henter kolonnene i hver replikerte tabell i kolonnerekkefølge`() {
        val kolonner = tabellRepository.hentKolonnerPerTabell()

        assertEquals(ExodusTabell.entries.map { it.tabellnavn }.toSet(), kolonner.keys)
        assertEquals(listOf("person_lopenr", "personnr", "opprettet", "oppdatert", "db_splitt"), kolonner["t_lopenr_fnr"])
    }

    @Test
    fun `teller rader i hver replikerte tabell`() {
        testdata.lagre(ExodusTabell.T_LOPENR_FNR, "person_lopenr" to "1", "personnr" to "01017012345")
        testdata.lagre(ExodusTabell.T_LOPENR_FNR, "person_lopenr" to "2", "personnr" to "02027012345")

        val antallRader = tabellRepository.hentAntallRaderPerTabell()

        assertEquals(ExodusTabell.entries.map { it.tabellnavn }.toSet(), antallRader.keys)
        assertEquals(2L, antallRader["t_lopenr_fnr"])
        assertEquals(0L, antallRader["t_stonad"])
    }
}
