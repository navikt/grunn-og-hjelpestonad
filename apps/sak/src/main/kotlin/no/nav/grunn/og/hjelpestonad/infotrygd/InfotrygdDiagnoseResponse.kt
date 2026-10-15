package no.nav.grunn.og.hjelpestonad.infotrygd

import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.DiagnoserResponse
import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.InfotrygdDiagnose
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.Icd10

data class InfotrygdDiagnoserResponse(
    val diagnoser: List<InfotrygdDiagnoseResponse>,
)

data class InfotrygdDiagnoseResponse(
    val stønadstype: InfotrygdDiagnose.Stønadstype,
    val månedNotert: String,
    val diagnosekode: String,
    val icd10Tekst: String?,
)

fun DiagnoserResponse.tilInfotrygdDiagnoserResponse(): InfotrygdDiagnoserResponse =
    InfotrygdDiagnoserResponse(
        diagnoser =
            diagnoser.map { diagnose ->
                val kode = diagnose.diagnosekode.trim()
                InfotrygdDiagnoseResponse(
                    stønadstype = diagnose.stønadstype,
                    månedNotert = diagnose.månedDiagnoseNotertIInfotrygd,
                    diagnosekode = kode,
                    icd10Tekst = Icd10.finn(kode)?.tekst,
                )
            },
    )
