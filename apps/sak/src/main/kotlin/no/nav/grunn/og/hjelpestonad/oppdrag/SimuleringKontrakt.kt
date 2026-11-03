package no.nav.grunn.og.hjelpestonad.oppdrag

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal
import java.time.LocalDate

/*
 * Speiler `Ressurs` og `no.nav.familie.kontrakter.felles.simulering` i familie-kontrakter,
 * slik at vi slipper avhengigheten. Enum-felter leses som tekst, og ukjente felter ignoreres.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
data class Ressurs<T>(
    val data: T?,
    val status: String,
    val melding: String? = null,
    val frontendFeilmelding: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DetaljertSimuleringResultat(
    val simuleringMottaker: List<SimuleringMottaker> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SimuleringMottaker(
    val simulertPostering: List<SimulertPostering> = emptyList(),
    // mottakerNummer (fødselsnummer eller orgnr) leses ikke, så det ikke sendes videre til nettleseren.
    val mottakerType: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SimulertPostering(
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
