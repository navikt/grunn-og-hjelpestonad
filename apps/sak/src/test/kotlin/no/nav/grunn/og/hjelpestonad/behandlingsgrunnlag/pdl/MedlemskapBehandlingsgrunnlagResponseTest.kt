package no.nav.grunn.og.hjelpestonad.behandlingsgrunnlag.pdl

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class MedlemskapBehandlingsgrunnlagResponseTest {
    private val behandlingId = UUID.randomUUID()

    private fun metadata(
        gyldig: LocalDateTime? = null,
        opphør: LocalDateTime? = null,
    ) = PdlMetadata(historisk = false, master = "FREG", gyldighetstidspunkt = gyldig, opphoerstidspunkt = opphør)

    @Test
    fun `gir tomme lister og hentetTidspunkt null når grunnlaget ikke er hentet`() {
        val response = null.tilMedlemskapBehandlingsgrunnlagResponse()

        assertThat(response.hentetTidspunkt).isNull()
        assertThat(response.bosted).isEmpty()
        assertThat(response.statsborgerskap).isEmpty()
        assertThat(response.oppholdstillatelse).isEmpty()
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

        val bosted = grunnlag.tilMedlemskapBehandlingsgrunnlagResponse().bosted

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

        val response = grunnlag.tilMedlemskapBehandlingsgrunnlagResponse()

        assertThat(response.statsborgerskap.first()).isEqualTo(OpplysningResponse(LocalDate.of(2021, 3, 10), null, "Norge"))
        assertThat(response.statsborgerskap.map { it.beskrivelse }).containsExactly("Norge", "Polen", "Statsløs")
        assertThat(response.oppholdstillatelse)
            .containsExactly(OpplysningResponse(LocalDate.of(2019, 9, 1), LocalDate.of(2021, 3, 9), "Permanent oppholdstillatelse"))
    }

    private fun grunnlag(
        folkeregisterpersonstatus: List<PdlFolkeregisterpersonstatus> = emptyList(),
        bostedsadresse: List<PdlBostedsadresse> = emptyList(),
        statsborgerskap: List<PdlStatsborgerskap> = emptyList(),
        opphold: List<PdlOpphold> = emptyList(),
        innflyttingTilNorge: List<PdlInnflyttingTilNorge> = emptyList(),
        utflyttingFraNorge: List<PdlUtflyttingFraNorge> = emptyList(),
    ) = PdlBehandlingsgrunnlag(
        hentetTidspunkt = LocalDateTime.of(2026, 10, 1, 9, 12),
        folkeregisterpersonstatus = folkeregisterpersonstatus,
        bostedsadresse = bostedsadresse,
        statsborgerskap = statsborgerskap,
        opphold = opphold,
        innflyttingTilNorge = innflyttingTilNorge,
        utflyttingFraNorge = utflyttingFraNorge,
    )
}
