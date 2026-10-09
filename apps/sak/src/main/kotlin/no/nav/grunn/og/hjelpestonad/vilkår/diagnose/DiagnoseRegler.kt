package no.nav.grunn.og.hjelpestonad.vilkår.diagnose

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput

object DiagnoseRegler {
    fun valider(request: VilkårDiagnoseRequest) {
        if (request.kode.isBlank()) {
            throw UgyldigInput("Diagnosekode kan ikke være tom")
        }
        if (Icd10.finn(request.kode) == null) {
            throw UgyldigInput("Diagnosekoden finnes ikke i ICD-10")
        }
        // Skadedatoen avgjør om § 6-9 lemper medlemskapskravet, jf. ADR-0004.
        if (request.erYrkesskade && request.fraOgMedDato == null) {
            throw UgyldigInput("En yrkesskade må ha en fra og med-dato")
        }
    }

    fun perioderMedSammeDiagnose(
        lagredePerioder: List<VilkårDiagnose>,
        diagnosekode: Diagnosekode,
    ): List<VilkårDiagnose> = lagredePerioder.filter { it.kode == diagnosekode.kode }
}
