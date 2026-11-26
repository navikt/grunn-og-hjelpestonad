package no.nav.grunn.og.hjelpestonad.faktagrunnlag.pdl

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class MedlemskapFaktagrunnlagResponseTest {
    private val behandlingId = UUID.randomUUID()

    private fun metadata(
        gyldig: LocalDateTime? = null,
        opphør: LocalDateTime? = null,
    ) = PdlMetadata(historisk = false, master = "FREG", gyldighetstidspunkt = gyldig, opphoerstidspunkt = opphør)

    @Test
    fun `gir tomme lister og hentetTidspunkt null når grunnlaget ikke er hentet`() {
        val response = null.tilMedlemskapFaktagrunnlagResponse()

        assertThat(response.hentetTidspunkt).isNull()
        assertThat(response.bosted).isEmpty()
        assertThat(response.statsborgerskap).isEmpty()
        assertThat(response.oppholdstillatelse).isEmpty()
        assertThat(response.dødsfall).isEmpty()
    }

    @Test
    fun `viser adresser, personstatus og flyttinger under bosted med nyeste først`() {
        val grunnlag =
            grunnlag(
                bostedsadresse =
                    listOf(
                        PdlBostedsadresse(
                            behandlingId = behandlingId,
                            adressetype = PdlAdressetype.UTENLANDSK_ADRESSE,
                            landkode = "AUS",
                            gyldigFraOgMed = LocalDateTime.of(2011, 9, 1, 0, 0),
                            gyldigTilOgMed = LocalDateTime.of(2019, 6, 14, 0, 0),
                            metadata = metadata(),
                        ),
                        PdlBostedsadresse(
                            behandlingId = behandlingId,
                            adressetype = PdlAdressetype.VEGADRESSE,
                            kommunenummer = "4601",
                            gyldigFraOgMed = LocalDateTime.of(2019, 6, 15, 0, 0),
                            metadata = metadata(),
                        ),
                    ),
                folkeregisterpersonstatus =
                    listOf(
                        PdlFolkeregisterpersonstatus(
                            behandlingId = behandlingId,
                            status = "utflyttet",
                            forenkletStatus = "ikkeBosatt",
                            metadata = metadata(LocalDateTime.of(2011, 9, 1, 0, 0), LocalDateTime.of(2019, 6, 15, 0, 0)),
                        ),
                    ),
                innflyttingTilNorge =
                    listOf(
                        PdlInnflyttingTilNorge(
                            behandlingId = behandlingId,
                            fraflyttingsland = "AUS",
                            metadata = metadata(LocalDateTime.of(2019, 6, 15, 0, 0)),
                        ),
                    ),
                utflyttingFraNorge =
                    listOf(
                        PdlUtflyttingFraNorge(
                            behandlingId = behandlingId,
                            tilflyttingsland = "AUS",
                            tilflyttingsstedIUtlandet = "Sydney",
                            utflyttingsdato = LocalDate.of(2011, 9, 1),
                            metadata = metadata(),
                        ),
                    ),
            )

        val bosted = grunnlag.tilMedlemskapFaktagrunnlagResponse().bosted

        assertThat(bosted.map { it.fraOgMedDato }).isSortedAccordingTo(compareByDescending { it })
        assertThat(bosted.map { it.beskrivelse }).containsExactlyInAnyOrder(
            "Bostedsadresse i Australia",
            "Bostedsadresse i Norge",
            "Personstatus: utflyttet",
            "Innflyttet fra Australia",
            "Utflyttet til Australia (Sydney)",
        )
        assertThat(bosted.single { it.beskrivelse == "Innflyttet fra Australia" })
            .isEqualTo(OpplysningResponse(LocalDate.of(2019, 6, 15), LocalDate.of(2019, 6, 15), "Innflyttet fra Australia"))
        assertThat(bosted.single { it.beskrivelse == "Personstatus: utflyttet" })
            .isEqualTo(OpplysningResponse(LocalDate.of(2011, 9, 1), LocalDate.of(2019, 6, 15), "Personstatus: utflyttet"))
    }

    @Test
    fun `oversetter statsborgerskap og oppholdstillatelse til tekst`() {
        val grunnlag =
            grunnlag(
                statsborgerskap =
                    listOf(
                        PdlStatsborgerskap(behandlingId = behandlingId, land = "POL", metadata = metadata()),
                        PdlStatsborgerskap(
                            behandlingId = behandlingId,
                            land = "NOR",
                            gyldigFraOgMed = LocalDate.of(2021, 3, 10),
                            metadata = metadata(),
                        ),
                        PdlStatsborgerskap(behandlingId = behandlingId, land = "XXX", metadata = metadata()),
                    ),
                opphold =
                    listOf(
                        PdlOpphold(
                            behandlingId = behandlingId,
                            type = "PERMANENT",
                            oppholdFra = LocalDate.of(2019, 9, 1),
                            oppholdTil = LocalDate.of(2021, 3, 9),
                            metadata = metadata(),
                        ),
                    ),
            )

        val response = grunnlag.tilMedlemskapFaktagrunnlagResponse()

        assertThat(response.statsborgerskap.first()).isEqualTo(OpplysningResponse(LocalDate.of(2021, 3, 10), null, "Norge"))
        assertThat(response.statsborgerskap.map { it.beskrivelse }).containsExactly("Norge", "Polen", "Statsløs")
        assertThat(response.oppholdstillatelse)
            .containsExactly(OpplysningResponse(LocalDate.of(2019, 9, 1), LocalDate.of(2021, 3, 9), "Permanent oppholdstillatelse"))
    }

    @Test
    fun `viser utenlandsk oppholdsadresse under bosted og dødsfall under dødsfall`() {
        val grunnlag =
            grunnlag(
                oppholdsadresse =
                    listOf(
                        PdlOppholdsadresse(
                            behandlingId = behandlingId,
                            landkode = "ESP",
                            gyldigFraOgMed = LocalDateTime.of(2022, 3, 1, 0, 0),
                            gyldigTilOgMed = LocalDateTime.of(2023, 6, 30, 0, 0),
                            metadata = metadata(),
                        ),
                    ),
                doedsfall =
                    listOf(
                        PdlDoedsfall(behandlingId = behandlingId, doedsdato = LocalDate.of(2026, 9, 1), metadata = metadata()),
                    ),
            )

        val response = grunnlag.tilMedlemskapFaktagrunnlagResponse()

        assertThat(response.bosted).containsExactly(
            OpplysningResponse(LocalDate.of(2022, 3, 1), LocalDate.of(2023, 6, 30), "Oppholdsadresse i Spania"),
        )
        assertThat(response.dødsfall).containsExactly(
            OpplysningResponse(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1), "Død"),
        )
    }

    @Test
    fun `bruker ikke gyldighetstidspunkt som dødsdato når doedsdato mangler`() {
        val grunnlag =
            grunnlag(
                doedsfall =
                    listOf(
                        PdlDoedsfall(behandlingId = behandlingId, doedsdato = null, metadata = metadata(LocalDateTime.of(2026, 9, 2, 0, 0))),
                    ),
            )

        val dødsfall = grunnlag.tilMedlemskapFaktagrunnlagResponse().dødsfall

        assertThat(dødsfall).containsExactly(OpplysningResponse(null, null, "Død"))
    }

    @Test
    fun `sorterer dødsfall med nyeste først`() {
        val grunnlag =
            grunnlag(
                doedsfall =
                    listOf(
                        PdlDoedsfall(behandlingId = behandlingId, doedsdato = LocalDate.of(2020, 1, 1), metadata = metadata()),
                        PdlDoedsfall(behandlingId = behandlingId, doedsdato = LocalDate.of(2026, 9, 1), metadata = metadata()),
                    ),
            )

        val dødsfall = grunnlag.tilMedlemskapFaktagrunnlagResponse().dødsfall

        assertThat(dødsfall.map { it.fraOgMedDato })
            .containsExactly(LocalDate.of(2026, 9, 1), LocalDate.of(2020, 1, 1))
    }

    @Test
    fun `falsk identitet havner ikke i bosted og eksponeres ikke i JSON`() {
        val grunnlag =
            grunnlag(
                bostedsadresse =
                    listOf(
                        PdlBostedsadresse(
                            behandlingId = behandlingId,
                            adressetype = PdlAdressetype.VEGADRESSE,
                            kommunenummer = "4601",
                            gyldigFraOgMed = LocalDateTime.of(2019, 6, 15, 0, 0),
                            metadata = metadata(),
                        ),
                    ),
                falskIdentitet = PdlFalskIdentitet(behandlingId = behandlingId, erFalsk = true, metadata = metadata()),
            )

        val response = grunnlag.tilMedlemskapFaktagrunnlagResponse()
        val json = jacksonObjectMapper().writeValueAsString(response)

        assertThat(response.bosted)
            .containsExactly(OpplysningResponse(LocalDate.of(2019, 6, 15), null, "Bostedsadresse i Norge"))
        assertThat(json).contains("\"bosted\"").doesNotContain("harFalskIdentitet")
    }

    private fun grunnlag(
        folkeregisterpersonstatus: List<PdlFolkeregisterpersonstatus> = emptyList(),
        bostedsadresse: List<PdlBostedsadresse> = emptyList(),
        statsborgerskap: List<PdlStatsborgerskap> = emptyList(),
        opphold: List<PdlOpphold> = emptyList(),
        innflyttingTilNorge: List<PdlInnflyttingTilNorge> = emptyList(),
        utflyttingFraNorge: List<PdlUtflyttingFraNorge> = emptyList(),
        oppholdsadresse: List<PdlOppholdsadresse> = emptyList(),
        doedsfall: List<PdlDoedsfall> = emptyList(),
        falskIdentitet: PdlFalskIdentitet? = null,
    ) = PdlFaktagrunnlag(
        hentetTidspunkt = LocalDateTime.of(2026, 10, 1, 9, 12),
        folkeregisterpersonstatus = folkeregisterpersonstatus,
        bostedsadresse = bostedsadresse,
        statsborgerskap = statsborgerskap,
        opphold = opphold,
        innflyttingTilNorge = innflyttingTilNorge,
        utflyttingFraNorge = utflyttingFraNorge,
        oppholdsadresse = oppholdsadresse,
        doedsfall = doedsfall,
        falskIdentitet = falskIdentitet,
    )
}
