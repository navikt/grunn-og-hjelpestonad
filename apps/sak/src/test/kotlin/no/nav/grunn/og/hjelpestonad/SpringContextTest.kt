package no.nav.grunn.og.hjelpestonad

import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskLogg
import no.nav.grunn.og.hjelpestonad.barn.BehandlingBarn
import no.nav.grunn.og.hjelpestonad.behandling.Behandling
import no.nav.grunn.og.hjelpestonad.behandling.årsak.ÅrsakBehandling
import no.nav.grunn.og.hjelpestonad.brev.Brev
import no.nav.grunn.og.hjelpestonad.fagsak.domain.Fagsak
import no.nav.grunn.og.hjelpestonad.fagsak.domain.FagsakPerson
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.TilkjentYtelse
import no.nav.grunn.og.hjelpestonad.vedtak.Vedtak
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
