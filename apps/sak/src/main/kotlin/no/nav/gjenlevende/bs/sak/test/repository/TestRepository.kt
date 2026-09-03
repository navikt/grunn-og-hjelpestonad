package no.nav.gjenlevende.bs.sak.test.repository

import no.nav.gjenlevende.bs.sak.test.domain.TestData
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TestRepository : CrudRepository<TestData, UUID> {
    @Query(
        "SELECT test_string FROM test_table WHERE id = :testId",
    )
    fun findTestStringMedId(testId: Int): List<String>
}
