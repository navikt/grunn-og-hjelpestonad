package no.nav.gjenlevende.bs.sak.test

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.gjenlevende.bs.sak.test.repository.TestRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/test/database")
@Tag(name = "Test å hente data fra Db", description = "Skal hente test data fra db")
class TestDbController(
    private val testRepository: TestRepository,
) {
    @GetMapping("/testdata")
    @Operation(
        summary = "Hent testdata fra db",
        description = "Skal hente test streng fra db",
    )
    fun hentTestData(testId: Int): List<String> = testRepository.findTestStringMedId(testId)
}
