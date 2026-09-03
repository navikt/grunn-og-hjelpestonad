package no.nav.gjenlevende.bs.sak.test.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column

data class TestData(
    @Id
    val id: Int = 1,
    @Column("test_string")
    val testString: String? = null,
)
