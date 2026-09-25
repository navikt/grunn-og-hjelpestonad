package no.nav.grunn.og.hjelpestonad.infotrygd.exodus

import no.nav.grunn.og.hjelpestonad.infotrygd.Application
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@SpringBootTest(classes = [Application::class])
@ActiveProfiles("test")
class ExodusUpsertRepositoryTest {
    @Autowired
    private lateinit var upsertRepository: ExodusUpsertRepository

    @Autowired
    private lateinit var statusRepository: ExodusStatusRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    // Replikatabellene har ikke Flyway-migrering ennå, så testen oppretter dem med ekte kolonnenavn.
    @BeforeEach
    fun opprettTabeller() {
        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS t_gh (
                vedtak_id numeric NOT NULL,
                tidspunkt_reg timestamp NOT NULL,
                trygdetid numeric,
                sats_gs numeric(11, 2),
                brukerid text,
                PRIMARY KEY (vedtak_id, tidspunkt_reg)
            )
            """.trimIndent(),
        )
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_lopenr_fnr (person_lopenr numeric PRIMARY KEY, personnr text)")
        jdbcTemplate.execute("TRUNCATE TABLE t_gh, t_lopenr_fnr, exodus_status")
    }

    @Test
    fun `upsert caster verdier til kolonnetypen og vasker tekst`() {
        upsertRepository.upsert(
            ExodusTabell.T_GH,
            listOf(
                mapOf(
                    "VEDTAK_ID" to "123",
                    "TIDSPUNKT_REG" to "2024-01-31T12:34:56.789",
                    "SATS_GS" to "1234.50",
                    "BRUKERID" to "Z99\u0000   ",
                ),
            ),
        )

        val lagret = jdbcTemplate.queryForMap("SELECT * FROM t_gh")
        assertEquals(BigDecimal("123"), lagret["vedtak_id"])
        assertEquals(
            LocalDateTime.of(2024, 1, 31, 12, 34, 56, 789_000_000),
            (lagret["tidspunkt_reg"] as java.sql.Timestamp).toLocalDateTime(),
        )
        assertEquals(BigDecimal("1234.50"), lagret["sats_gs"])
        assertEquals("Z99", lagret["brukerid"])
    }

    @Test
    fun `upsert oppdaterer eksisterende rad på sammensatt primærnøkkel og tåler null`() {
        val nøkkel = arrayOf("vedtak_id" to "1", "tidspunkt_reg" to "2024-01-01T00:00")
        upsertRepository.upsert(ExodusTabell.T_GH, listOf(mapOf(*nøkkel, "brukerid" to "gammel", "trygdetid" to "40")))
        upsertRepository.upsert(ExodusTabell.T_GH, listOf(mapOf(*nøkkel, "brukerid" to "ny", "trygdetid" to null)))

        val lagret = jdbcTemplate.queryForList("SELECT brukerid, trygdetid FROM t_gh")
        assertEquals(1, lagret.size)
        assertEquals("ny", lagret.single()["brukerid"])
        assertNull(lagret.single()["trygdetid"])
    }

    @Test
    fun `upsert feiler når Exodus sender kolonner som ikke finnes i Postgres`() {
        val feil =
            assertFailsWith<IllegalStateException> {
                upsertRepository.upsert(
                    ExodusTabell.T_LOPENR_FNR,
                    listOf(mapOf("person_lopenr" to "1", "ukjent\"; drop" to "x")),
                )
            }
        assertEquals(true, feil.message?.contains("mangler kolonnene"))
    }

    @Test
    fun `upsert feiler når tabellen ikke finnes`() {
        assertFailsWith<IllegalStateException> {
            upsertRepository.upsert(ExodusTabell.T_VEDTAK, listOf(mapOf("vedtak_id" to "1")))
        }
    }

    @Test
    fun `truncate tømmer alle tabellene og ny baseline nullstiller iteratorene`() {
        upsertRepository.upsert(ExodusTabell.T_GH, listOf(mapOf("vedtak_id" to "1", "tidspunkt_reg" to "2024-01-01T00:00")))
        upsertRepository.upsert(ExodusTabell.T_LOPENR_FNR, listOf(mapOf("person_lopenr" to "1")))
        statusRepository.oppdaterIterator("t_gh", "iterator-1", 1, flereSider = true)
        statusRepository.oppdaterIterator("t_gh", "iterator-2", 2, flereSider = false)

        assertEquals(JobStatus.OK, statusRepository.finn("t_gh")?.jobStatus)
        assertEquals(3, statusRepository.finn("t_gh")?.antallRaderHentet)

        val tabeller = listOf(ExodusTabell.T_GH, ExodusTabell.T_LOPENR_FNR)
        upsertRepository.truncate(tabeller)
        statusRepository.settNyBaseline(tabeller.map { it.tabellnavn })

        tabeller.forEach {
            assertEquals(0, jdbcTemplate.queryForObject("SELECT count(*) FROM ${it.tabellnavn}", Int::class.java))
            val status = statusRepository.finn(it.tabellnavn)
            assertEquals(JobStatus.NY_BASELINE, status?.jobStatus)
            assertNull(status?.iterator)
            assertEquals(0, status?.antallRaderHentet)
        }
    }
}
