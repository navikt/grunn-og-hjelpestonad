package no.nav.grunn.og.hjelpestonad.vilkår.diagnose

import no.nav.helse.diagnosekoder.Diagnosekoder

object Icd10 {
    const val MAKS_ANTALL_TREFF = 50

    /** Kodene i navikt/diagnosekoder er uten punktum, for eksempel `E109` for E10.9. */
    fun normaliserKode(kode: String): String = kode.filterNot { it.isWhitespace() || it == '.' }.uppercase()

    private val koder: Map<String, Diagnosekode> =
        Diagnosekoder.icd10.mapValues { (_, it) -> Diagnosekode(it.code, it.text) }

    fun finn(kode: String): Diagnosekode? = koder[normaliserKode(kode)]

    fun søk(søketekst: String): List<Diagnosekode> {
        val ord = søketekst.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (ord.isEmpty()) return emptyList()
        val kode = normaliserKode(søketekst)

        return koder
            .values
            .mapNotNull { diagnosekode -> treffrangering(diagnosekode, kode, ord)?.let { it to diagnosekode } }
            .sortedBy { (rangering, _) -> rangering }
            .take(MAKS_ANTALL_TREFF)
            .map { (_, diagnosekode) -> diagnosekode }
    }

    private fun treffrangering(
        diagnosekode: Diagnosekode,
        kode: String,
        ord: List<String>,
    ): Int? {
        val tekstTreff = ord.all { diagnosekode.tekst.contains(it, ignoreCase = true) }
        return when {
            diagnosekode.kode == kode -> 0
            diagnosekode.kode.startsWith(kode) -> 1
            tekstTreff && diagnosekode.tekst.startsWith(ord.first(), ignoreCase = true) -> 2
            tekstTreff -> 3
            else -> null
        }
    }
}
