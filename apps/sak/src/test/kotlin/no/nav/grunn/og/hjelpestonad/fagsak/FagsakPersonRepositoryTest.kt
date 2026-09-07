package no.nav.grunn.og.hjelpestonad.fagsak

import no.nav.grunn.og.hjelpestonad.SpringContextTest
import no.nav.grunn.og.hjelpestonad.fagsak.domain.FagsakPerson
import no.nav.grunn.og.hjelpestonad.fagsak.domain.Personident
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

open class FagsakPersonRepositoryTest : SpringContextTest() {
    @Autowired
    private lateinit var fagsakPersonRepository: FagsakPersonRepository

    @Test
    fun `lagre og hent FagsakPerson`() {
        val person1 =
            fagsakPersonRepository.insert(
                FagsakPerson(
                    identer = setOf(Personident("1"), Personident("3")),
                ),
            )
        val person2 = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(Personident("2"))))

        assertThat(fagsakPersonRepository.findPersonidenter(person1.id)).containsExactlyInAnyOrderElementsOf(person1.identer)
        assertThat(fagsakPersonRepository.findPersonidenter(person2.id)).containsExactlyInAnyOrderElementsOf(person2.identer)
    }
}
