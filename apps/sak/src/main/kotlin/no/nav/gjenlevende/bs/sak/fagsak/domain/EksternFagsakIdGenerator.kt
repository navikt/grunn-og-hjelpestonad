package no.nav.gjenlevende.bs.sak.fagsak.domain

import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.stereotype.Component

@Component
class EksternFagsakIdGenerator(
    private val jdbcTemplate: JdbcTemplate,
) : BeforeConvertCallback<Fagsak> {
    override fun onBeforeConvert(fagsak: Fagsak): Fagsak {
        if (fagsak.eksternId == 0L) {
            val id = jdbcTemplate.queryForObject<Long>("SELECT nextval('fagsak_ekstern_id_seq')") ?: throw IllegalStateException("Sekvens behandling_ekstern_id_seq returnerte null, det skal ikke kunne skje.")
            return fagsak.copy(eksternId = id)
        }

        return fagsak
    }
}
