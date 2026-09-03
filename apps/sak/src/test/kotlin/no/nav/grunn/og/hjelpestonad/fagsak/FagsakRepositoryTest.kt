package no.nav.grunn.og.hjelpestonad.fagsak

import no.nav.grunn.og.hjelpestonad.SpringContextTest
import no.nav.grunn.og.hjelpestonad.fagsak.domain.Fagsak
import no.nav.grunn.og.hjelpestonad.fagsak.domain.FagsakPerson
import no.nav.grunn.og.hjelpestonad.fagsak.domain.Personident
import no.nav.grunn.og.hjelpestonad.fagsak.domain.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FagsakRepositoryTest : SpringContextTest() {
    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var fagsakPersonRepository: FagsakPersonRepository

    @Test
    fun `insert fagsak`() {
        val ident = "01010199999"
        val fagsakPerson = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(Personident(ident))))

        fagsakRepository.insert(Fagsak(fagsakPersonId = fagsakPerson.id, stønadstype = StønadType.BARNETILSYN))
        val alleFagsaker = fagsakRepository.findAll()

        assertThat(alleFagsaker).hasSize(1)
        val fagsak = alleFagsaker.first()
        assertThat(fagsak.id).isEqualTo(fagsak.id)
        assertThat(fagsak.eksternId).isGreaterThanOrEqualTo(200_000_000)
    }
}
