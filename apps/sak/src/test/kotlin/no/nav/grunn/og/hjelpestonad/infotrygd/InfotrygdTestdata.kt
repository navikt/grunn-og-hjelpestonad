package no.nav.grunn.og.hjelpestonad.infotrygd

import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.InfotrygdEvaluering
import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.InfotrygdGhResponse
import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.InfotrygdStonadResponse
import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.InfotrygdVedtakResponse
import java.time.LocalDate
import java.time.LocalDateTime

/** Testdata for svar fra Infotrygd. Registreringstidspunktet følger vedtakId med mindre det oppgis. */
object InfotrygdTestdata {
    fun stønad(
        vararg vedtak: InfotrygdVedtakResponse,
        stønadId: Long = 1L,
        datoOpphor: LocalDate? = null,
    ) = InfotrygdStonadResponse(
        stonadId = stønadId,
        personnr = "12345678901",
        kodeRutine = "GS",
        datoStart = LocalDate.parse("2020-01-01"),
        datoOpphor = datoOpphor,
        vedtak = vedtak.toList(),
    )

    fun vedtak(
        vedtakId: Long,
        fom: String = "2020-01-01",
        tom: String? = null,
        tidspunktReg: LocalDateTime = LocalDateTime.parse("2020-01-01T12:00:00").plusDays(vedtakId),
        evaluering: InfotrygdEvaluering? = null,
        gh: List<InfotrygdGhResponse> = emptyList(),
    ) = InfotrygdVedtakResponse(
        vedtakId = vedtakId,
        tknr = "0301",
        saksblokk = "A",
        saksnr = 1L,
        typeSak = "S",
        kodeResultat = "I",
        datoInnvFom = LocalDate.parse(fom),
        datoInnvTom = tom?.let(LocalDate::parse),
        datoMottattSak = LocalDate.parse("2019-12-15"),
        kodeVedtaksnivaa = "L",
        typeBeregning = "M",
        tidspunktReg = tidspunktReg,
        brukerid = "Z123456",
        endringer = listOf("F"),
        delytelser = emptyList(),
        gh = gh,
        evaluering = evaluering,
    )

    fun evaluering(diagnose1: String = "E10.9") =
        InfotrygdEvaluering(
            månedNotertIInfotrygd = "2020-01",
            diagnose1 = diagnose1,
            motstridendeFelt = emptySet(),
        )

    fun gh(trygdetid: Long = 40L) =
        InfotrygdGhResponse(
            tidspunktReg = LocalDateTime.parse("2020-01-01T12:00:00"),
            trygdetid = trygdetid,
            brukerid = "Z123456",
            satsGs = 1L,
        )
}
