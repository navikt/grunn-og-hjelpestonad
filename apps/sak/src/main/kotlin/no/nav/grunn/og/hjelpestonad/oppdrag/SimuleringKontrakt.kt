package no.nav.grunn.og.hjelpestonad.oppdrag

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal
import java.time.LocalDate

/*
 * Speiler `no.nav.familie.kontrakter.felles.simulering` i familie-kontrakter,
 * slik at vi slipper avhengigheten. Felter leses som tekst og gjøres om i `SimuleringOppsummerer`. Ukjente navn på
 * `mottakerType`, `posteringType` og `betalingType` gir `UgyldigInput`. `fagOmrådeKode` leses som tekst: ikke-tomme koder
 * som ikke er grunn- eller hjelpestønad havner i `ukjenteFagområdekoder`. Ukjente JSON-felter ignoreres.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
data class DetaljertSimuleringResultat(
    val simuleringMottaker: List<SimuleringMottaker> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SimuleringMottaker(
    val simulertPostering: List<SimulertPostering> = emptyList(),
    val mottakerNummer: String? = null,
    val mottakerType: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SimulertPostering(
    /**
     * Navnet på fagområdekoden fra Oppdrag, for eksempel `GRUNNSTØNAD_INFOTRYGD`. Lest som tekst. Koder som ikke finnes i
     * [FagOmrådeKode] havner i `ukjenteFagområdekoder`. Mangler feltet eller er det tomt, gir det `UgyldigInput`.
     */
    val fagOmrådeKode: String? = null,
    val erFeilkonto: Boolean? = null,
    val fom: LocalDate? = null,
    val tom: LocalDate? = null,
    val betalingType: String? = null,
    val beløp: BigDecimal? = null,
    val posteringType: String? = null,
    val forfallsdato: LocalDate? = null,
    val utenInntrekk: Boolean? = null,
    val klassekode: String? = null,
)

/**
 * Fagområdekodene for grunn- og hjelpestønad. Oppdrag svarer med posteringer for alle fagsakene personen har, også andre
 * ytelser. Andre ikke-tomme fagområdekoder havner i `ukjenteFagområdekoder`. familie-oppdrag sender navnet på verdien.
 */
enum class FagOmrådeKode {
    GRUNNSTØNAD_INFOTRYGD,
    GRUNNSTØNAD_INFOTRYGD_MANUELT,
    HJELPESTØNAD_INFOTRYGD,
    HJELPESTØNAD_INFOTRYGD_MANUELT,
    GRUNN_OG_HJELPESTØNAD_TILBAKEKREVING_MANUELT,
}
