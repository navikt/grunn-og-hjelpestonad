package no.nav.grunn.og.hjelpestonad.oppdrag.utbetalingsgenerator

import no.nav.grunn.og.hjelpestonad.oppdrag.utbetalingsgenerator.domain.AndelData
import java.time.YearMonth

private sealed interface BeståendeAndelResultat

private object NyAndelSkriverOver : BeståendeAndelResultat

private class Opphørsdato(
    val opphør: YearMonth,
) : BeståendeAndelResultat

private class AvkortAndel(
    val andel: AndelData,
    val opphør: YearMonth? = null,
) : BeståendeAndelResultat

internal data class BeståendeAndeler(
    val andeler: List<AndelData>,
    val opphørFra: YearMonth? = null,
)

internal object BeståendeAndelerBeregner {
    fun finnBeståendeAndeler(
        forrigeAndeler: List<AndelData>,
        nyeAndeler: List<AndelData>,
        opphørsdato: YearMonth?,
    ): BeståendeAndeler {
        if (opphørsdato != null) {
            return BeståendeAndeler(emptyList(), opphørsdato)
        }

        val indexPåFørsteEndring = finnIndexPåFørsteEndring(forrigeAndeler, nyeAndeler)
        val forrigeAndelerMedOppdatertId = oppdaterBeståendeAndelerMedId(forrigeAndeler, nyeAndeler)
        return finnBeståendeAndeler(forrigeAndelerMedOppdatertId, nyeAndeler, indexPåFørsteEndring)
    }

    private fun finnBeståendeAndeler(
        forrigeAndeler: List<AndelData>,
        nyeAndeler: List<AndelData>,
        indexPåFørsteEndring: Int?,
    ): BeståendeAndeler =
        if (indexPåFørsteEndring != null) {
            finnBeståendeAndelerNårDetFinnesEndring(
                forrigeAndeler = forrigeAndeler,
                nyeAndeler = nyeAndeler,
                indexPåFørsteEndring = indexPåFørsteEndring,
            )
        } else {
            BeståendeAndeler(forrigeAndeler, null)
        }

    private fun finnBeståendeAndelerNårDetFinnesEndring(
        forrigeAndeler: List<AndelData>,
        nyeAndeler: List<AndelData>,
        indexPåFørsteEndring: Int,
    ): BeståendeAndeler {
        val opphørsdato = finnBeståendeAndelOgOpphør(indexPåFørsteEndring, forrigeAndeler, nyeAndeler)
        return when (opphørsdato) {
            is Opphørsdato -> {
                BeståendeAndeler(forrigeAndeler.subList(0, indexPåFørsteEndring), opphørsdato.opphør)
            }

            is NyAndelSkriverOver -> {
                BeståendeAndeler(forrigeAndeler.subList(0, maxOf(0, indexPåFørsteEndring)))
            }

            is AvkortAndel -> {
                val avkortetAndeler = forrigeAndeler.subList(0, maxOf(0, indexPåFørsteEndring))
                BeståendeAndeler(avkortetAndeler + opphørsdato.andel, opphørsdato.opphør)
            }
        }
    }

    private fun finnBeståendeAndelOgOpphør(
        index: Int,
        forrigeAndeler: List<AndelData>,
        nyeAndeler: List<AndelData>,
    ): BeståendeAndelResultat {
        val forrige = forrigeAndeler[index]
        val ny = if (nyeAndeler.size > index) nyeAndeler[index] else null
        val nyNeste = if (nyeAndeler.size > index + 1) nyeAndeler[index + 1] else null

        return finnBeståendeAndelOgOpphør(ny, forrige, nyNeste)
    }

    private fun finnBeståendeAndelOgOpphør(
        ny: AndelData?,
        forrige: AndelData,
        nyNeste: AndelData?,
    ): BeståendeAndelResultat {
        if (ny == null || forrige.fom < ny.fom) {
            return Opphørsdato(forrige.fom)
        }
        if (forrige.fom > ny.fom || forrige.beløp != ny.beløp) {
            if (ny.beløp == 0) {
                return Opphørsdato(ny.fom)
            }
            return NyAndelSkriverOver
        }
        if (ny.tom != null && (forrige.tom == null || forrige.tom > ny.tom)) {
            val opphørsdato =
                if (nyNeste == null || nyNeste.fom != ny.tom.plusMonths(1) || nyNeste.beløp == 0) {
                    ny.tom.plusMonths(1)
                } else {
                    null
                }
            return AvkortAndel(forrige.copy(tom = ny.tom), opphørsdato)
        }
        return NyAndelSkriverOver
    }

    private fun oppdaterBeståendeAndelerMedId(
        forrigeAndeler: List<AndelData>,
        nyeAndeler: List<AndelData>,
    ) = forrigeAndeler.mapIndexed { forrigeIndex, andelData ->
        if (nyeAndeler.size > forrigeIndex) {
            andelData.copy(id = nyeAndeler[forrigeIndex].id)
        } else {
            andelData
        }
    }

    private fun finnIndexPåFørsteEndring(
        forrige: List<AndelData>,
        nye: List<AndelData>,
    ): Int? {
        forrige.forEachIndexed { index, andelData ->
            if (nye.size > index) {
                val nyAndelForIndex = nye[index]
                if (!andelData.erLik(nyAndelForIndex)) {
                    return index
                }
            } else {
                return index
            }
        }
        return null
    }

    private fun AndelData.erLik(other: AndelData): Boolean =
        this.fom == other.fom &&
            this.tom == other.tom &&
            this.beløp == other.beløp
}
