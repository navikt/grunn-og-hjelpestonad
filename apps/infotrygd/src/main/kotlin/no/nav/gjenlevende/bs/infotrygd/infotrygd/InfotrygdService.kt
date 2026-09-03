package no.nav.gjenlevende.bs.infotrygd.infotrygd

import no.nav.gjenlevende.bs.infotrygd.infotrygd.dto.BarnInfo
import no.nav.gjenlevende.bs.infotrygd.infotrygd.dto.PeriodeResponse
import no.nav.gjenlevende.bs.infotrygd.infotrygd.dto.StønadType
import no.nav.gjenlevende.bs.infotrygd.infotrygd.dto.VedtakPeriodeResponse
import no.nav.gjenlevende.bs.infotrygd.infotrygd.repository.InfotrygdRepository
import no.nav.gjenlevende.bs.infotrygd.infrastruktur.exception.ApiFeil
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class InfotrygdService(
    private val infotrygdRepository: InfotrygdRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun test() {
        val test = infotrygdRepository.test()
        logger.info("Antall treff i db: ${test.size} - skal være 7")
    }

    fun hentVedtakPerioder(personident: String): VedtakPeriodeResponse {
        if (!infotrygdRepository.personEksisterer(personident)) {
            logger.info("Person med ident ikke funnet i Infotrygd")
            throw ApiFeil("Person ikke funnet i Infotrygd", HttpStatus.NOT_FOUND)
        }

        val vedtakPerioder =
            infotrygdRepository
                .hentVedtakPerioderForPerson(personident)
                .filter { it.erGyldigPeriode() }

        if (vedtakPerioder.isEmpty()) {
            logger.info("Person funnet i Infotrygd, men ingen vedtak perioder funnet")
            return VedtakPeriodeResponse(personident = personident)
        }

        val vedtakIder = vedtakPerioder.map { it.vedtakId }

        val roller = infotrygdRepository.hentRollerForVedtak(vedtakIder)
        val rollerPerVedtak = roller.groupBy { it.vedtakId }

        val barnetilsynVedtak = vedtakPerioder.filter { it.stønadType == StønadType.BARNETILSYN }
        val skolepengerVedtak = vedtakPerioder.filter { it.stønadType == StønadType.SKOLEPENGER }

        val barnetilsynPerioder =
            barnetilsynVedtak.map { vedtak ->
                val barnForVedtak = rollerPerVedtak[vedtak.vedtakId].orEmpty()
                val vedtakTom = vedtak.beregnTomDato()

                PeriodeResponse(
                    stønadType = StønadType.BARNETILSYN,
                    fom = vedtak.datoFom,
                    tom = vedtakTom,
                    vedtakId = vedtak.vedtakId,
                    stønadId = vedtak.stønadId,
                    barn =
                        barnForVedtak.map { barn ->
                            BarnInfo(
                                personLøpenummer = barn.personLøpenummer,
                                fom = barn.fom,
                                tom = barn.tom,
                            )
                        },
                )
            }

        val skolepengerPerioder =
            skolepengerVedtak.map { vedtak ->
                val barnForVedtak = rollerPerVedtak[vedtak.vedtakId].orEmpty()
                val vedtakTom = vedtak.beregnTomDato()

                PeriodeResponse(
                    stønadType = StønadType.SKOLEPENGER,
                    fom = vedtak.datoFom,
                    tom = vedtakTom,
                    vedtakId = vedtak.vedtakId,
                    stønadId = vedtak.stønadId,
                    barn =
                        barnForVedtak.map { barn ->
                            BarnInfo(
                                personLøpenummer = barn.personLøpenummer,
                                fom = barn.fom,
                                tom = barn.tom,
                            )
                        },
                )
            }

        return VedtakPeriodeResponse(
            personident = personident,
            barnetilsyn = barnetilsynPerioder,
            skolepenger = skolepengerPerioder,
        )
    }
}
