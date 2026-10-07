package no.nav.grunn.og.hjelpestonad.behandlingsgrunnlag.pdl

import no.nav.grunn.og.hjelpestonad.pdl.Bostedsadresse
import no.nav.grunn.og.hjelpestonad.pdl.Doedsfall
import no.nav.grunn.og.hjelpestonad.pdl.Folkeregistermetadata
import no.nav.grunn.og.hjelpestonad.pdl.Folkeregisterpersonstatus
import no.nav.grunn.og.hjelpestonad.pdl.InnflyttingTilNorge
import no.nav.grunn.og.hjelpestonad.pdl.Metadata
import no.nav.grunn.og.hjelpestonad.pdl.Opphold
import no.nav.grunn.og.hjelpestonad.pdl.Oppholdsadresse
import no.nav.grunn.og.hjelpestonad.pdl.Statsborgerskap
import no.nav.grunn.og.hjelpestonad.pdl.UtflyttingFraNorge
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class PdlBehandlingsgrunnlag(
    val hentetTidspunkt: LocalDateTime,
    val folkeregisterpersonstatus: List<PdlFolkeregisterpersonstatus>,
    val bostedsadresse: List<PdlBostedsadresse>,
    val statsborgerskap: List<PdlStatsborgerskap>,
    val opphold: List<PdlOpphold>,
    val innflyttingTilNorge: List<PdlInnflyttingTilNorge>,
    val utflyttingFraNorge: List<PdlUtflyttingFraNorge>,
    val oppholdsadresse: List<PdlOppholdsadresse>,
    val doedsfall: List<PdlDoedsfall>,
)

data class PdlMetadata(
    val historisk: Boolean,
    val master: String,
    val gyldighetstidspunkt: LocalDateTime? = null,
    val opphoerstidspunkt: LocalDateTime? = null,
)

@Table("behandlingsgrunnlag_pdl_folkeregisterpersonstatus")
data class PdlFolkeregisterpersonstatus(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val status: String,
    val forenkletStatus: String,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val metadata: PdlMetadata,
)

enum class PdlAdressetype {
    VEGADRESSE,
    MATRIKKELADRESSE,
    UTENLANDSK_ADRESSE,
    UKJENT_BOSTED,
}

@Table("behandlingsgrunnlag_pdl_bostedsadresse")
data class PdlBostedsadresse(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val adressetype: PdlAdressetype?,
    val kommunenummer: String? = null,
    val bostedskommune: String? = null,
    val landkode: String? = null,
    val gyldigFraOgMed: LocalDateTime? = null,
    val gyldigTilOgMed: LocalDateTime? = null,
    val angittFlyttedato: LocalDate? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val metadata: PdlMetadata,
)

@Table("behandlingsgrunnlag_pdl_statsborgerskap")
data class PdlStatsborgerskap(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val land: String,
    val gyldigFraOgMed: LocalDate? = null,
    val gyldigTilOgMed: LocalDate? = null,
    val bekreftelsesdato: LocalDate? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val metadata: PdlMetadata,
)

@Table("behandlingsgrunnlag_pdl_opphold")
data class PdlOpphold(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val type: String,
    val oppholdFra: LocalDate? = null,
    val oppholdTil: LocalDate? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val metadata: PdlMetadata,
)

@Table("behandlingsgrunnlag_pdl_innflytting_til_norge")
data class PdlInnflyttingTilNorge(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val fraflyttingsland: String? = null,
    val fraflyttingsstedIUtlandet: String? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val metadata: PdlMetadata,
)

@Table("behandlingsgrunnlag_pdl_utflytting_fra_norge")
data class PdlUtflyttingFraNorge(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val tilflyttingsland: String? = null,
    val tilflyttingsstedIUtlandet: String? = null,
    val utflyttingsdato: LocalDate? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val metadata: PdlMetadata,
)

/** Bare utenlandske oppholdsadresser, og bare landet. */
@Table("behandlingsgrunnlag_pdl_oppholdsadresse")
data class PdlOppholdsadresse(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val landkode: String,
    val gyldigFraOgMed: LocalDateTime? = null,
    val gyldigTilOgMed: LocalDateTime? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val metadata: PdlMetadata,
)

@Table("behandlingsgrunnlag_pdl_doedsfall")
data class PdlDoedsfall(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val doedsdato: LocalDate? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val metadata: PdlMetadata,
)

fun Folkeregisterpersonstatus.tilPdlFolkeregisterpersonstatus(behandlingId: UUID) =
    PdlFolkeregisterpersonstatus(
        behandlingId = behandlingId,
        status = status,
        forenkletStatus = forenkletStatus,
        metadata = tilPdlMetadata(metadata, folkeregistermetadata),
    )

fun Bostedsadresse.tilPdlBostedsadresse(behandlingId: UUID) =
    PdlBostedsadresse(
        behandlingId = behandlingId,
        adressetype =
            when {
                vegadresse != null -> PdlAdressetype.VEGADRESSE
                matrikkeladresse != null -> PdlAdressetype.MATRIKKELADRESSE
                utenlandskAdresse != null -> PdlAdressetype.UTENLANDSK_ADRESSE
                ukjentBosted != null -> PdlAdressetype.UKJENT_BOSTED
                else -> null
            },
        kommunenummer = vegadresse?.kommunenummer ?: matrikkeladresse?.kommunenummer,
        bostedskommune = ukjentBosted?.bostedskommune,
        landkode = utenlandskAdresse?.landkode,
        gyldigFraOgMed = gyldigFraOgMed,
        gyldigTilOgMed = gyldigTilOgMed,
        angittFlyttedato = angittFlyttedato,
        metadata = tilPdlMetadata(metadata, folkeregistermetadata),
    )

fun Statsborgerskap.tilPdlStatsborgerskap(behandlingId: UUID) =
    PdlStatsborgerskap(
        behandlingId = behandlingId,
        land = land,
        gyldigFraOgMed = gyldigFraOgMed,
        gyldigTilOgMed = gyldigTilOgMed,
        bekreftelsesdato = bekreftelsesdato,
        metadata = tilPdlMetadata(metadata, folkeregistermetadata),
    )

fun Opphold.tilPdlOpphold(behandlingId: UUID) =
    PdlOpphold(
        behandlingId = behandlingId,
        type = type,
        oppholdFra = oppholdFra,
        oppholdTil = oppholdTil,
        metadata = tilPdlMetadata(metadata, folkeregistermetadata),
    )

fun InnflyttingTilNorge.tilPdlInnflyttingTilNorge(behandlingId: UUID) =
    PdlInnflyttingTilNorge(
        behandlingId = behandlingId,
        fraflyttingsland = fraflyttingsland,
        fraflyttingsstedIUtlandet = fraflyttingsstedIUtlandet,
        metadata = tilPdlMetadata(metadata, folkeregistermetadata),
    )

fun UtflyttingFraNorge.tilPdlUtflyttingFraNorge(behandlingId: UUID) =
    PdlUtflyttingFraNorge(
        behandlingId = behandlingId,
        tilflyttingsland = tilflyttingsland,
        tilflyttingsstedIUtlandet = tilflyttingsstedIUtlandet,
        utflyttingsdato = utflyttingsdato,
        metadata = tilPdlMetadata(metadata, folkeregistermetadata),
    )

fun Oppholdsadresse.tilPdlOppholdsadresse(behandlingId: UUID): PdlOppholdsadresse? =
    utenlandskAdresse?.let {
        PdlOppholdsadresse(
            behandlingId = behandlingId,
            landkode = it.landkode,
            gyldigFraOgMed = gyldigFraOgMed,
            gyldigTilOgMed = gyldigTilOgMed,
            metadata = tilPdlMetadata(metadata, folkeregistermetadata),
        )
    }

fun Doedsfall.tilPdlDoedsfall(behandlingId: UUID) =
    PdlDoedsfall(
        behandlingId = behandlingId,
        doedsdato = doedsdato,
        metadata = tilPdlMetadata(metadata, folkeregistermetadata),
    )

private fun tilPdlMetadata(
    metadata: Metadata,
    folkeregistermetadata: Folkeregistermetadata?,
) = PdlMetadata(
    historisk = metadata.historisk,
    master = metadata.master,
    gyldighetstidspunkt = folkeregistermetadata?.gyldighetstidspunkt,
    opphoerstidspunkt = folkeregistermetadata?.opphoerstidspunkt,
)
