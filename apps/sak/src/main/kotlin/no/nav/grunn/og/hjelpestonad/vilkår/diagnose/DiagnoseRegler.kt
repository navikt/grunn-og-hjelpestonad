package no.nav.grunn.og.hjelpestonad.vilkår.diagnose

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput

object DiagnoseRegler {
    const val YRKESSKADE_FORDELT_PÅ_FLERE_PERIODER =
        "En diagnose med yrkesskade kan bare ha én oppfylt periode. " +
            "Endre den eksisterende perioden i stedet for å legge til en ny eller dele den opp."

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

    fun validerMaksEnYrkesskadePeriode(perioder: List<VilkårDiagnose>) {
        perioder.filter { it.erVilkårOppfylt() }.groupBy { it.tidslinjenøkkel() }.values.forEach { oppfylte ->
            if (oppfylte.size > 1 && oppfylte.any { it.erYrkesskade }) {
                throw UgyldigInput(YRKESSKADE_FORDELT_PÅ_FLERE_PERIODER)
            }
        }
    }
}
