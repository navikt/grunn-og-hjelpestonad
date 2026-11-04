package no.nav.grunn.og.hjelpestonad.felles.periode

import org.junit.jupiter.api.Disabled
import kotlin.test.Test

/**
 * 🔴 Rød sone, ticket 02: testskjelett for detaljene i endringshistorikken.
 *
 * Fellesdelen skal bygge detaljene av faste felter, slik at ingen underklasse kan legge
 * diagnosekode eller diagnosebeskrivelse der (GDPR artikkel 9). Lag en testperiode og en
 * testservice i denne filen som arver fra [PeriodeMedBegrunnelseService], og fyll inn testene
 * når API-et for type og vurdering er bestemt. Testen i `VilkårDiagnoseServiceTest` om at
 * endringshistorikken ikke røper diagnosen skal fortsatt være grønn uten endringer.
 */
class PeriodeMedBegrunnelseServiceTest {
    @Test
    @Disabled("Rød sone, ticket 02: skrives av utvikleren")
    fun `detaljene for en periode uten vurdering har bare type og periode`() {
        // Gitt en periode uten vurdering, slik trygdetid blir i ticket 08,
        // når perioden lagres med fra og med 01.01.2025 og til og med 30.06.2025,
        // så er detaljene «<TYPE>, Periode: 01.01.2025 – 30.06.2025».
        TODO()
    }
}
