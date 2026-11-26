package no.nav.grunn.og.hjelpestonad.migrering

import no.nav.grunn.og.hjelpestonad.pdl.DødsfallEnkel
import no.nav.grunn.og.hjelpestonad.pdl.FalskIdentitetEnkel
import no.nav.grunn.og.hjelpestonad.pdl.PersonopplysningerForMigrering
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate
import kotlin.test.Test

class MigreringPersonValideringTest {
    @Test
    fun `godkjenner person uten dødsfall og uten falsk identitet`() {
        val personopplysningerForMigreringValideringResultat =
            MigreringPersonValidering.valider(
                PersonopplysningerForMigrering(
                    falskIdentitet = null,
                    dødsfall = emptyList(),
                ),
            )

        assertThat(personopplysningerForMigreringValideringResultat.kanMigreres).isTrue()
        assertThat(personopplysningerForMigreringValideringResultat.blokkeringer).isEmpty()
    }

    @Test
    fun `godkjenner person når falskIdentitet har erFalsk false`() {
        val personopplysningerForMigreringValideringResultat =
            MigreringPersonValidering.valider(
                PersonopplysningerForMigrering(
                    falskIdentitet = FalskIdentitetEnkel(erFalsk = false),
                    dødsfall = emptyList(),
                ),
            )

        assertThat(personopplysningerForMigreringValideringResultat.kanMigreres).isTrue()
        assertThat(personopplysningerForMigreringValideringResultat.blokkeringer).isEmpty()
    }

    @Test
    fun `avviser person med falsk identitet`() {
        val personopplysningerForMigreringValideringResultat =
            MigreringPersonValidering.valider(
                PersonopplysningerForMigrering(
                    falskIdentitet = FalskIdentitetEnkel(erFalsk = true),
                    dødsfall = emptyList(),
                ),
            )

        assertThat(personopplysningerForMigreringValideringResultat.kanMigreres).isFalse()
        assertThat(personopplysningerForMigreringValideringResultat.blokkeringer).containsExactly(PersonblokkeringForMigrering.FALSK_IDENTITET)
    }

    @Test
    fun `avviser død person med dødsdato`() {
        val personopplysningerForMigreringValideringResultat =
            MigreringPersonValidering.valider(
                PersonopplysningerForMigrering(
                    falskIdentitet = null,
                    dødsfall = listOf(DødsfallEnkel(dødsdato = LocalDate.of(2025, 5, 1))),
                ),
            )

        assertThat(personopplysningerForMigreringValideringResultat.kanMigreres).isFalse()
        assertThat(personopplysningerForMigreringValideringResultat.blokkeringer).containsExactly(PersonblokkeringForMigrering.DØD)
    }

    @Test
    fun `avviser død person også når dødsdato er null`() {
        val personopplysningerForMigreringValideringResultat =
            MigreringPersonValidering.valider(
                PersonopplysningerForMigrering(
                    falskIdentitet = null,
                    dødsfall = listOf(DødsfallEnkel(dødsdato = null)),
                ),
            )

        assertThat(personopplysningerForMigreringValideringResultat.kanMigreres).isFalse()
        assertThat(personopplysningerForMigreringValideringResultat.blokkeringer).containsExactly(PersonblokkeringForMigrering.DØD)
    }

    @Test
    fun `returnerer begge blokkeringer når personen både er død og har falsk identitet`() {
        val personopplysningerForMigreringValideringResultat =
            MigreringPersonValidering.valider(
                PersonopplysningerForMigrering(
                    falskIdentitet = FalskIdentitetEnkel(erFalsk = true),
                    dødsfall = listOf(DødsfallEnkel(dødsdato = LocalDate.of(2024, 1, 10))),
                ),
            )

        assertThat(personopplysningerForMigreringValideringResultat.kanMigreres).isFalse()
        assertThat(personopplysningerForMigreringValideringResultat.blokkeringer).containsExactlyInAnyOrder(
            PersonblokkeringForMigrering.DØD,
            PersonblokkeringForMigrering.FALSK_IDENTITET,
        )
    }
}
