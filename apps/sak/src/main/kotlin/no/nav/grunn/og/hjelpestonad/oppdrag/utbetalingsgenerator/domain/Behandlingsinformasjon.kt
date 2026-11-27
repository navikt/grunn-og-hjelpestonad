package no.nav.grunn.og.hjelpestonad.oppdrag.utbetalingsgenerator.domain

import java.time.LocalDate
import java.time.YearMonth

data class Behandlingsinformasjon(
    val saksbehandlerId: String,
    val behandlingId: String,
    val eksternBehandlingId: Long,
    val eksternFagsakId: Long,
    val fagsystem: Fagsystem,
    val personIdent: String,
    val vedtaksdato: LocalDate,
    val opphørAlleKjederFra: YearMonth?,
    val utbetalesTil: String? = null,
    val erGOmregning: Boolean = false,
    val opphørKjederFraFørsteUtbetaling: Boolean = false,
)

interface Fagsystem {
    val kode: String
    val gyldigeSatstyper: Set<Ytelsestype>
}
