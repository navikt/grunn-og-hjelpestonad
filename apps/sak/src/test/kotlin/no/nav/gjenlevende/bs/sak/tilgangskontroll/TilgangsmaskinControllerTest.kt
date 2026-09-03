package no.nav.gjenlevende.bs.sak.tilgangskontroll

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.gjenlevende.bs.sak.felles.sikkerhet.SikkerhetContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TilgangsmaskinControllerTest {
    private val tilgangsmaskinClient = mockk<TilgangsmaskinClient>()
    private val controller = TilgangsmaskinController(tilgangsmaskinClient)

    @BeforeEach
    fun setup() {
        mockkObject(SikkerhetContext)
        every { SikkerhetContext.hentSaksbehandler() } returns "Z123456"
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SikkerhetContext)
    }

    @Nested
    inner class SjekkTilgangBulk {
        @Test
        fun `bulk sjekk med blanding av tilganger og avvisninger returnerer riktig forenklet respons`() {
            val navIdent = "Z123456"
            val personidentMedTilgang = "12345678901"
            val personidentUtenTilgang = "12345678902"
            val personidentEgenFamilie = "12345678903"

            every { SikkerhetContext.hentSaksbehandler() } returns navIdent
            every {
                tilgangsmaskinClient.sjekkTilgangBulk(
                    personidenter = any(),
                    regelType = RegelType.KJERNE_REGELTYPE,
                )
            } returns
                BulkTilgangsResponse(
                    navIdent = navIdent,
                    resultater =
                        setOf(
                            TilgangsResultat(
                                personident = personidentMedTilgang,
                                status = 204,
                                detaljer = null,
                            ),
                            TilgangsResultat(
                                personident = personidentUtenTilgang,
                                status = 403,
                                detaljer =
                                    mapOf(
                                        "title" to "AVVIST_SKJERMING",
                                        "begrunnelse" to "Du har ikke tilgang til Nav-ansatte og andre skjermede brukere",
                                    ),
                            ),
                            TilgangsResultat(
                                personident = personidentEgenFamilie,
                                status = 403,
                                detaljer =
                                    mapOf(
                                        "title" to "AVVIST_HABILITET",
                                        "begrunnelse" to "Du har ikke tilgang til data om deg selv eller dine nærstående",
                                    ),
                            ),
                        ),
                )

            val request =
                BulkTilgangsRequest(
                    personidenter = listOf(personidentMedTilgang, personidentUtenTilgang, personidentEgenFamilie),
                )

            val resultat = controller.sjekkTilgangBulkForenklet(request)

            assertEquals(navIdent, resultat.navIdent)
            assertEquals(3, resultat.resultater.size)

            val medTilgang = resultat.resultater.first { it.personident == personidentMedTilgang }
            assertTrue(medTilgang.harTilgang)
            assertNull(medTilgang.avvisningskode)
            assertNull(medTilgang.begrunnelse)

            val skjermet = resultat.resultater.first { it.personident == personidentUtenTilgang }
            assertFalse(skjermet.harTilgang)
            assertEquals(Avvisningskode.AVVIST_SKJERMING, skjermet.avvisningskode)
            assertEquals("Du har ikke tilgang til Nav-ansatte og andre skjermede brukere", skjermet.begrunnelse)

            val familie = resultat.resultater.first { it.personident == personidentEgenFamilie }
            assertFalse(familie.harTilgang)
            assertEquals(Avvisningskode.AVVIST_HABILITET, familie.avvisningskode)
            assertEquals("Du har ikke tilgang til data om deg selv eller dine nærstående", familie.begrunnelse)
        }

        @Test
        fun `bulk sjekk der alle har identer har tilgang returnerer harTilgang satt true`() {
            val navIdent = "Z123456"
            val personident1 = "12345678901"
            val personident2 = "12345678902"

            every {
                tilgangsmaskinClient.sjekkTilgangBulk(
                    personidenter = any(),
                    regelType = RegelType.KJERNE_REGELTYPE,
                )
            } returns
                BulkTilgangsResponse(
                    navIdent = navIdent,
                    resultater =
                        setOf(
                            TilgangsResultat(personident = personident1, status = 204, detaljer = null),
                            TilgangsResultat(personident = personident2, status = 204, detaljer = null),
                        ),
                )

            val request = BulkTilgangsRequest(personidenter = listOf(personident1, personident2))

            val resultat = controller.sjekkTilgangBulkForenklet(request)

            assertEquals(2, resultat.resultater.size)
            assertTrue(resultat.resultater.all { it.harTilgang })
            assertTrue(resultat.resultater.all { it.avvisningskode == null })
        }

        @Test
        fun `bulk sjekk der ingen ident har tilgang returnerer alle med harTilgang false`() {
            val navIdent = "Z123456"
            val egenIdent = "12345678901"
            val barnIdent = "12345678902"
            val partnerIdent = "12345678903"

            every { SikkerhetContext.hentSaksbehandler() } returns navIdent
            every {
                tilgangsmaskinClient.sjekkTilgangBulk(
                    personidenter = any(),
                    regelType = RegelType.KJERNE_REGELTYPE,
                )
            } returns
                BulkTilgangsResponse(
                    navIdent = navIdent,
                    resultater =
                        setOf(
                            TilgangsResultat(
                                personident = egenIdent,
                                status = 403,
                                detaljer =
                                    mapOf(
                                        "title" to "AVVIST_HABILITET",
                                        "begrunnelse" to "Du har ikke tilgang til data om deg selv eller dine nærstående",
                                    ),
                            ),
                            TilgangsResultat(
                                personident = barnIdent,
                                status = 403,
                                detaljer =
                                    mapOf(
                                        "title" to "AVVIST_HABILITET",
                                        "begrunnelse" to "Du har ikke tilgang til data om deg selv eller dine nærstående",
                                    ),
                            ),
                            TilgangsResultat(
                                personident = partnerIdent,
                                status = 403,
                                detaljer =
                                    mapOf(
                                        "title" to "AVVIST_HABILITET",
                                        "begrunnelse" to "Du har ikke tilgang til data om deg selv eller dine nærstående",
                                    ),
                            ),
                        ),
                )

            val request = BulkTilgangsRequest(personidenter = listOf(egenIdent, barnIdent, partnerIdent))

            val resultat = controller.sjekkTilgangBulkForenklet(request)

            assertEquals(3, resultat.resultater.size)
            assertTrue(resultat.resultater.none { it.harTilgang })
            assertTrue(resultat.resultater.all { it.avvisningskode == Avvisningskode.AVVIST_HABILITET })
        }

        @Test
        fun `bulk sjekk med tom liste returnerer tom respons`() {
            val navIdent = "Z123456"

            every { SikkerhetContext.hentSaksbehandler() } returns navIdent
            every {
                tilgangsmaskinClient.sjekkTilgangBulk(
                    personidenter = any(),
                    regelType = RegelType.KJERNE_REGELTYPE,
                )
            } returns BulkTilgangsResponse(navIdent = navIdent, resultater = emptySet())

            val request = BulkTilgangsRequest(personidenter = emptyList())

            val resultat = controller.sjekkTilgangBulkForenklet(request)

            assertEquals(navIdent, resultat.navIdent)
            assertTrue(resultat.resultater.isEmpty())
        }

        @Test
        fun `bulk sjekk med ulike avvisningskoder mapper riktig`() {
            val navIdent = "Z123456"
            val strengtFortrolig = "12345678901"
            val fortrolig = "12345678902"
            val skjermet = "12345678903"
            val geografisk = "12345678904"

            every { SikkerhetContext.hentSaksbehandler() } returns navIdent
            every {
                tilgangsmaskinClient.sjekkTilgangBulk(
                    personidenter = any(),
                    regelType = RegelType.KJERNE_REGELTYPE,
                )
            } returns
                BulkTilgangsResponse(
                    navIdent = navIdent,
                    resultater =
                        setOf(
                            TilgangsResultat(
                                personident = strengtFortrolig,
                                status = 403,
                                detaljer =
                                    mapOf(
                                        "title" to "AVVIST_STRENGT_FORTROLIG_ADRESSE",
                                        "begrunnelse" to "Du har ikke tilgang til brukere med strengt fortrolig adresse",
                                    ),
                            ),
                            TilgangsResultat(
                                personident = fortrolig,
                                status = 403,
                                detaljer =
                                    mapOf(
                                        "title" to "AVVIST_FORTROLIG_ADRESSE",
                                        "begrunnelse" to "Du har ikke tilgang til brukere med fortrolig adresse",
                                    ),
                            ),
                            TilgangsResultat(
                                personident = skjermet,
                                status = 403,
                                detaljer =
                                    mapOf(
                                        "title" to "AVVIST_SKJERMING",
                                        "begrunnelse" to "Du har ikke tilgang til Nav-ansatte og andre skjermede brukere",
                                    ),
                            ),
                            TilgangsResultat(
                                personident = geografisk,
                                status = 403,
                                detaljer =
                                    mapOf(
                                        "title" to "AVVIST_GEOGRAFISK",
                                        "begrunnelse" to "Du har ikke tilgang til brukerens geografiske område",
                                    ),
                            ),
                        ),
                )

            val request =
                BulkTilgangsRequest(
                    personidenter = listOf(strengtFortrolig, fortrolig, skjermet, geografisk),
                )

            val resultat = controller.sjekkTilgangBulkForenklet(request)

            assertEquals(4, resultat.resultater.size)

            val strengtFortroligResultat = resultat.resultater.first { it.personident == strengtFortrolig }
            assertEquals(Avvisningskode.AVVIST_STRENGT_FORTROLIG_ADRESSE, strengtFortroligResultat.avvisningskode)

            val fortroligResultat = resultat.resultater.first { it.personident == fortrolig }
            assertEquals(Avvisningskode.AVVIST_FORTROLIG_ADRESSE, fortroligResultat.avvisningskode)

            val skjermetResultat = resultat.resultater.first { it.personident == skjermet }
            assertEquals(Avvisningskode.AVVIST_SKJERMING, skjermetResultat.avvisningskode)

            val geografiskResultat = resultat.resultater.first { it.personident == geografisk }
            assertEquals(Avvisningskode.AVVIST_GEOGRAFISK, geografiskResultat.avvisningskode)
        }
    }
}
