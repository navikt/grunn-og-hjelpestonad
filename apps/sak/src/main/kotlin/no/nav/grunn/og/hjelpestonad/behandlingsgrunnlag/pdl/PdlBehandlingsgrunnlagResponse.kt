package no.nav.grunn.og.hjelpestonad.behandlingsgrunnlag.pdl

import java.time.LocalDate
import java.time.LocalDateTime

/** `hentetTidspunkt` er `null` når behandlingsgrunnlaget ikke er hentet fra PDL. */
data class PdlBehandlingsgrunnlagResponse(
    val hentetTidspunkt: LocalDateTime?,
    val folkeregisterpersonstatus: List<PdlFolkeregisterpersonstatusResponse>,
    val bostedsadresse: List<PdlBostedsadresseResponse>,
    val statsborgerskap: List<PdlStatsborgerskapResponse>,
    val opphold: List<PdlOppholdResponse>,
    val innflyttingTilNorge: List<PdlInnflyttingTilNorgeResponse>,
    val utflyttingFraNorge: List<PdlUtflyttingFraNorgeResponse>,
) {
    companion object {
        val IKKE_HENTET =
            PdlBehandlingsgrunnlagResponse(
                hentetTidspunkt = null,
                folkeregisterpersonstatus = emptyList(),
                bostedsadresse = emptyList(),
                statsborgerskap = emptyList(),
                opphold = emptyList(),
                innflyttingTilNorge = emptyList(),
                utflyttingFraNorge = emptyList(),
            )
    }
}

data class PdlMetadataResponse(
    val historisk: Boolean,
    val master: String,
    val gyldighetstidspunkt: LocalDateTime?,
    val opphoerstidspunkt: LocalDateTime?,
)

data class PdlFolkeregisterpersonstatusResponse(
    val status: String,
    val forenkletStatus: String,
    val metadata: PdlMetadataResponse,
)

data class PdlBostedsadresseResponse(
    val adressetype: PdlAdressetype?,
    val kommunenummer: String?,
    val bostedskommune: String?,
    val landkode: String?,
    val gyldigFraOgMed: LocalDateTime?,
    val gyldigTilOgMed: LocalDateTime?,
    val angittFlyttedato: LocalDate?,
    val metadata: PdlMetadataResponse,
)

data class PdlStatsborgerskapResponse(
    val land: String,
    val gyldigFraOgMed: LocalDate?,
    val gyldigTilOgMed: LocalDate?,
    val bekreftelsesdato: LocalDate?,
    val metadata: PdlMetadataResponse,
)

data class PdlOppholdResponse(
    val type: String,
    val oppholdFra: LocalDate?,
    val oppholdTil: LocalDate?,
    val metadata: PdlMetadataResponse,
)

data class PdlInnflyttingTilNorgeResponse(
    val fraflyttingsland: String?,
    val fraflyttingsstedIUtlandet: String?,
    val metadata: PdlMetadataResponse,
)

data class PdlUtflyttingFraNorgeResponse(
    val tilflyttingsland: String?,
    val tilflyttingsstedIUtlandet: String?,
    val utflyttingsdato: LocalDate?,
    val metadata: PdlMetadataResponse,
)

fun PdlBehandlingsgrunnlag.tilResponse() =
    PdlBehandlingsgrunnlagResponse(
        hentetTidspunkt = hentetTidspunkt,
        folkeregisterpersonstatus =
            folkeregisterpersonstatus.map {
                PdlFolkeregisterpersonstatusResponse(
                    status = it.status,
                    forenkletStatus = it.forenkletStatus,
                    metadata = it.metadata.tilResponse(),
                )
            },
        bostedsadresse =
            bostedsadresse.map {
                PdlBostedsadresseResponse(
                    adressetype = it.adressetype,
                    kommunenummer = it.kommunenummer,
                    bostedskommune = it.bostedskommune,
                    landkode = it.landkode,
                    gyldigFraOgMed = it.gyldigFraOgMed,
                    gyldigTilOgMed = it.gyldigTilOgMed,
                    angittFlyttedato = it.angittFlyttedato,
                    metadata = it.metadata.tilResponse(),
                )
            },
        statsborgerskap =
            statsborgerskap.map {
                PdlStatsborgerskapResponse(
                    land = it.land,
                    gyldigFraOgMed = it.gyldigFraOgMed,
                    gyldigTilOgMed = it.gyldigTilOgMed,
                    bekreftelsesdato = it.bekreftelsesdato,
                    metadata = it.metadata.tilResponse(),
                )
            },
        opphold =
            opphold.map {
                PdlOppholdResponse(
                    type = it.type,
                    oppholdFra = it.oppholdFra,
                    oppholdTil = it.oppholdTil,
                    metadata = it.metadata.tilResponse(),
                )
            },
        innflyttingTilNorge =
            innflyttingTilNorge.map {
                PdlInnflyttingTilNorgeResponse(
                    fraflyttingsland = it.fraflyttingsland,
                    fraflyttingsstedIUtlandet = it.fraflyttingsstedIUtlandet,
                    metadata = it.metadata.tilResponse(),
                )
            },
        utflyttingFraNorge =
            utflyttingFraNorge.map {
                PdlUtflyttingFraNorgeResponse(
                    tilflyttingsland = it.tilflyttingsland,
                    tilflyttingsstedIUtlandet = it.tilflyttingsstedIUtlandet,
                    utflyttingsdato = it.utflyttingsdato,
                    metadata = it.metadata.tilResponse(),
                )
            },
    )

private fun PdlMetadata.tilResponse() =
    PdlMetadataResponse(
        historisk = historisk,
        master = master,
        gyldighetstidspunkt = gyldighetstidspunkt,
        opphoerstidspunkt = opphoerstidspunkt,
    )
