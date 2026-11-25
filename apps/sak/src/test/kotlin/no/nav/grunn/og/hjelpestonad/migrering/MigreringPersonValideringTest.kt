package no.nav.grunn.og.hjelpestonad.migrering

import no.nav.grunn.og.hjelpestonad.pdl.DoedsfallEnkel
import no.nav.grunn.og.hjelpestonad.pdl.FalskIdentitetEnkel
import no.nav.grunn.og.hjelpestonad.pdl.PersonIdentitetOgDoedsfall
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate
import kotlin.test.Test

class MigreringPersonValideringTest {
    @Test
    fun `godkjenner person uten dødsfall og uten falsk identitet`() {
        val valideringsresultat =
            MigreringPersonValidering.valider(
                PersonIdentitetOgDoedsfall(
                    falskIdentitet = null,
                    doedsfall = emptyList(),
                ),
            )

        assertThat(valideringsresultat.kanMigreres).isTrue()
        assertThat(valideringsresultat.blokkeringer).isEmpty()
    }

    @Test
    fun `godkjenner person når falskIdentitet har erFalsk false`() {
        val valideringsresultat =
            MigreringPersonValidering.valider(
                PersonIdentitetOgDoedsfall(
                    falskIdentitet = FalskIdentitetEnkel(erFalsk = false),
                    doedsfall = emptyList(),
                ),
            )

        assertThat(valideringsresultat.kanMigreres).isTrue()
        assertThat(valideringsresultat.blokkeringer).isEmpty()
    }

    @Test
    fun `avviser person med falsk identitet`() {
        val valideringsresultat =
            MigreringPersonValidering.valider(
                PersonIdentitetOgDoedsfall(
                    falskIdentitet = FalskIdentitetEnkel(erFalsk = true),
                    doedsfall = emptyList(),
                ),
            )

        assertThat(valideringsresultat.kanMigreres).isFalse()
        assertThat(valideringsresultat.blokkeringer).containsExactly(MigreringPersonBlokkering.FALSK_IDENTITET)
    }

    @Test
    fun `avviser død person med dødsdato`() {
        val valideringsresultat =
            MigreringPersonValidering.valider(
                PersonIdentitetOgDoedsfall(
                    falskIdentitet = null,
                    doedsfall = listOf(DoedsfallEnkel(doedsdato = LocalDate.of(2025, 5, 1))),
                ),
            )

        assertThat(valideringsresultat.kanMigreres).isFalse()
        assertThat(valideringsresultat.blokkeringer).containsExactly(MigreringPersonBlokkering.DØD)
    }

    @Test
    fun `avviser død person også når doedsdato er null`() {
        val valideringsresultat =
            MigreringPersonValidering.valider(
                PersonIdentitetOgDoedsfall(
                    falskIdentitet = null,
                    doedsfall = listOf(DoedsfallEnkel(doedsdato = null)),
                ),
            )

        assertThat(valideringsresultat.kanMigreres).isFalse()
        assertThat(valideringsresultat.blokkeringer).containsExactly(MigreringPersonBlokkering.DØD)
    }

    @Test
    fun `returnerer begge blokkeringer når personen både er død og har falsk identitet`() {
        val valideringsresultat =
            MigreringPersonValidering.valider(
                PersonIdentitetOgDoedsfall(
                    falskIdentitet = FalskIdentitetEnkel(erFalsk = true),
                    doedsfall = listOf(DoedsfallEnkel(doedsdato = LocalDate.of(2024, 1, 10))),
                ),
            )

        assertThat(valideringsresultat.kanMigreres).isFalse()
        assertThat(valideringsresultat.blokkeringer).containsExactlyInAnyOrder(
            MigreringPersonBlokkering.DØD,
            MigreringPersonBlokkering.FALSK_IDENTITET,
        )
    }
}
