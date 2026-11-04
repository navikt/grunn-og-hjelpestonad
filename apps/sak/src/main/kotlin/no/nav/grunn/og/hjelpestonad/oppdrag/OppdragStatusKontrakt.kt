package no.nav.grunn.og.hjelpestonad.oppdrag

/**
 * Speiler `no.nav.familie.kontrakter.felles.oppdrag.OppdragId`. `familie-oppdrag-backend` finner oppdraget med
 * fagsystem, person og behandlings-ID-en på første utbetalingsperiode (vår eksterne behandlings-ID).
 */
data class OppdragId(
    val fagsystem: String,
    val personIdent: String,
    val behandlingsId: String,
) {
    override fun toString(): String = "OppdragId(fagsystem=$fagsystem, behandlingsId=$behandlingsId)"
}

/**
 * [status] er navnet på `OppdragStatus` i familie-kontrakter, for eksempel `LAGT_PÅ_KØ` eller `KVITTERT_OK`.
 * [melding] er beskrivelsen fra kvitteringen når OS har svart.
 */
data class OppdragStatusSvar(
    val status: String,
    val melding: String?,
)
