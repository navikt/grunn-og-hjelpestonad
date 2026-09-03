package no.nav.gjenlevende.bs.sak

import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskLogg
import no.nav.gjenlevende.bs.sak.barn.BehandlingBarn
import no.nav.gjenlevende.bs.sak.behandling.Behandling
import no.nav.gjenlevende.bs.sak.behandling.årsak.ÅrsakBehandling
import no.nav.gjenlevende.bs.sak.brev.Brev
import no.nav.gjenlevende.bs.sak.fagsak.domain.Fagsak
import no.nav.gjenlevende.bs.sak.fagsak.domain.FagsakPerson
import no.nav.gjenlevende.bs.sak.tilkjentytelse.TilkjentYtelse
import no.nav.gjenlevende.bs.sak.vedtak.Vedtak
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.data.jdbc.core.JdbcAggregateOperations
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [ApplicationLocalSetup::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(
    "integrasjonstest",
)
open class SpringContextTest {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var jdbcAggregateOperations: JdbcAggregateOperations

    @AfterEach
    fun reset() {
        resetWiremockServers()
        resetDatabase()
    }

    private fun resetDatabase() {
        jdbcAggregateOperations.deleteAll(BehandlingBarn::class.java)
        jdbcAggregateOperations.deleteAll(TilkjentYtelse::class.java)
        jdbcAggregateOperations.deleteAll(Vedtak::class.java)
        jdbcAggregateOperations.deleteAll(ÅrsakBehandling::class.java)
        jdbcAggregateOperations.deleteAll(Brev::class.java)
        jdbcAggregateOperations.deleteAll(Behandling::class.java)
        jdbcAggregateOperations.deleteAll(TaskLogg::class.java)
        jdbcAggregateOperations.deleteAll(Task::class.java)
        jdbcAggregateOperations.deleteAll(Fagsak::class.java)
        jdbcAggregateOperations.deleteAll(FagsakPerson::class.java)
    }

    private fun resetWiremockServers() {
        applicationContext.getBeansOfType(WireMockServer::class.java).values.forEach(WireMockServer::resetRequests)
    }
}
