package no.nav.grunn.og.hjelpestonad.infotrygd

import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.DiagnoserResponse
import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.InfotrygdDiagnose
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class InfotrygdDiagnoseResponseTest {
    @Test
    fun `gyldig ICD-10-kode gir tilhørende tekst`() {
        val response = diagnoserResponse("E10.9").tilInfotrygdDiagnoserResponse()

        assertThat(response.diagnoser.single().icd10Tekst).isEqualTo("Diabetes mellitus type 1 uten komplikasjoner")
    }

    @Test
    fun `ugyldig kode beholdes uten ICD-10-tekst`() {
        val response = diagnoserResponse("T89").tilInfotrygdDiagnoserResponse()

        assertThat(response.diagnoser.single().diagnosekode).isEqualTo("T89")
        assertThat(response.diagnoser.single().icd10Tekst).isNull()
    }

    @Test
    fun `kode trimmes og normaliseres ved oppslag`() {
        val response = diagnoserResponse(" e10.9 ").tilInfotrygdDiagnoserResponse()

        assertThat(response.diagnoser.single().diagnosekode).isEqualTo("e10.9")
        assertThat(response.diagnoser.single().icd10Tekst).isEqualTo("Diabetes mellitus type 1 uten komplikasjoner")
    }

    @Test
    fun `personident er ikke del av responsfeltene`() {
        assertThat(InfotrygdDiagnoseResponse::class.java.declaredFields.map { it.name })
            .doesNotContain("personident")
    }

    private fun diagnoserResponse(kode: String) =
        DiagnoserResponse(
            diagnoser =
                listOf(
                    InfotrygdDiagnose(
                        personident = "12345678901",
                        stønadstype = InfotrygdDiagnose.Stønadstype.GRUNNSTØNAD,
                        månedDiagnoseNotertIInfotrygd = "2020-01",
                        diagnosekode = kode,
                        datoEndretIKilde = LocalDateTime.parse("2020-01-01T12:00:00"),
                    ),
                ),
        )
}
