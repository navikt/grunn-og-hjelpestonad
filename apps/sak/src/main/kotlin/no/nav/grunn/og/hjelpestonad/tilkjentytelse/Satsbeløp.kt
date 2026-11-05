package no.nav.grunn.og.hjelpestonad.tilkjentytelse

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import java.time.LocalDate

/** Månedsbeløpet i hele kroner Stortinget har fastsatt for en sats (1–6) i en periode. `gyldigTilOgMed` er `null` for gjeldende beløp. */
data class Satsbeløp(
    val sats: Int,
    val månedsbeløp: Int,
    val gyldigFraOgMed: LocalDate,
    val gyldigTilOgMed: LocalDate?,
)

/**
 * Satsbeløpene for grunnstønad etter § 6-3 andre ledd, fra
 * [rundskrivet til § 6-3](https://lovdata.no/nav/rundskriv/v1-06-03).
 */
object Satsbeløpene {
    val alle: List<Satsbeløp> =
        listOf(
            // 1.1.1997
            Satsbeløp(sats = 1, månedsbeløp = 480, gyldigFraOgMed = LocalDate.of(1997, 1, 1), gyldigTilOgMed = LocalDate.of(1997, 12, 31)),
            Satsbeløp(sats = 2, månedsbeløp = 732, gyldigFraOgMed = LocalDate.of(1997, 1, 1), gyldigTilOgMed = LocalDate.of(1997, 12, 31)),
            Satsbeløp(sats = 3, månedsbeløp = 961, gyldigFraOgMed = LocalDate.of(1997, 1, 1), gyldigTilOgMed = LocalDate.of(1997, 12, 31)),
            Satsbeløp(sats = 4, månedsbeløp = 1416, gyldigFraOgMed = LocalDate.of(1997, 1, 1), gyldigTilOgMed = LocalDate.of(1997, 12, 31)),
            Satsbeløp(sats = 5, månedsbeløp = 1920, gyldigFraOgMed = LocalDate.of(1997, 1, 1), gyldigTilOgMed = LocalDate.of(1997, 12, 31)),
            Satsbeløp(sats = 6, månedsbeløp = 2400, gyldigFraOgMed = LocalDate.of(1997, 1, 1), gyldigTilOgMed = LocalDate.of(1997, 12, 31)),
            // 1.1.1998
            Satsbeløp(sats = 1, månedsbeløp = 491, gyldigFraOgMed = LocalDate.of(1998, 1, 1), gyldigTilOgMed = LocalDate.of(1998, 12, 31)),
            Satsbeløp(sats = 2, månedsbeløp = 748, gyldigFraOgMed = LocalDate.of(1998, 1, 1), gyldigTilOgMed = LocalDate.of(1998, 12, 31)),
            Satsbeløp(sats = 3, månedsbeløp = 983, gyldigFraOgMed = LocalDate.of(1998, 1, 1), gyldigTilOgMed = LocalDate.of(1998, 12, 31)),
            Satsbeløp(sats = 4, månedsbeløp = 1448, gyldigFraOgMed = LocalDate.of(1998, 1, 1), gyldigTilOgMed = LocalDate.of(1998, 12, 31)),
            Satsbeløp(sats = 5, månedsbeløp = 1963, gyldigFraOgMed = LocalDate.of(1998, 1, 1), gyldigTilOgMed = LocalDate.of(1998, 12, 31)),
            Satsbeløp(sats = 6, månedsbeløp = 2454, gyldigFraOgMed = LocalDate.of(1998, 1, 1), gyldigTilOgMed = LocalDate.of(1998, 12, 31)),
            // 1.1.1999
            Satsbeløp(sats = 1, månedsbeløp = 503, gyldigFraOgMed = LocalDate.of(1999, 1, 1), gyldigTilOgMed = LocalDate.of(1999, 12, 31)),
            Satsbeløp(sats = 2, månedsbeløp = 767, gyldigFraOgMed = LocalDate.of(1999, 1, 1), gyldigTilOgMed = LocalDate.of(1999, 12, 31)),
            Satsbeløp(sats = 3, månedsbeløp = 1008, gyldigFraOgMed = LocalDate.of(1999, 1, 1), gyldigTilOgMed = LocalDate.of(1999, 12, 31)),
            Satsbeløp(sats = 4, månedsbeløp = 1484, gyldigFraOgMed = LocalDate.of(1999, 1, 1), gyldigTilOgMed = LocalDate.of(1999, 12, 31)),
            Satsbeløp(sats = 5, månedsbeløp = 2012, gyldigFraOgMed = LocalDate.of(1999, 1, 1), gyldigTilOgMed = LocalDate.of(1999, 12, 31)),
            Satsbeløp(sats = 6, månedsbeløp = 2515, gyldigFraOgMed = LocalDate.of(1999, 1, 1), gyldigTilOgMed = LocalDate.of(1999, 12, 31)),
            // 1.1.2000
            Satsbeløp(sats = 1, månedsbeløp = 513, gyldigFraOgMed = LocalDate.of(2000, 1, 1), gyldigTilOgMed = LocalDate.of(2001, 12, 31)),
            Satsbeløp(sats = 2, månedsbeløp = 782, gyldigFraOgMed = LocalDate.of(2000, 1, 1), gyldigTilOgMed = LocalDate.of(2001, 12, 31)),
            Satsbeløp(sats = 3, månedsbeløp = 1028, gyldigFraOgMed = LocalDate.of(2000, 1, 1), gyldigTilOgMed = LocalDate.of(2001, 12, 31)),
            Satsbeløp(sats = 4, månedsbeløp = 1514, gyldigFraOgMed = LocalDate.of(2000, 1, 1), gyldigTilOgMed = LocalDate.of(2001, 12, 31)),
            Satsbeløp(sats = 5, månedsbeløp = 2052, gyldigFraOgMed = LocalDate.of(2000, 1, 1), gyldigTilOgMed = LocalDate.of(2001, 12, 31)),
            Satsbeløp(sats = 6, månedsbeløp = 2565, gyldigFraOgMed = LocalDate.of(2000, 1, 1), gyldigTilOgMed = LocalDate.of(2001, 12, 31)),
            // 1.1.2002
            Satsbeløp(sats = 1, månedsbeløp = 533, gyldigFraOgMed = LocalDate.of(2002, 1, 1), gyldigTilOgMed = LocalDate.of(2002, 12, 31)),
            Satsbeløp(sats = 2, månedsbeløp = 814, gyldigFraOgMed = LocalDate.of(2002, 1, 1), gyldigTilOgMed = LocalDate.of(2002, 12, 31)),
            Satsbeløp(sats = 3, månedsbeløp = 1070, gyldigFraOgMed = LocalDate.of(2002, 1, 1), gyldigTilOgMed = LocalDate.of(2002, 12, 31)),
            Satsbeløp(sats = 4, månedsbeløp = 1575, gyldigFraOgMed = LocalDate.of(2002, 1, 1), gyldigTilOgMed = LocalDate.of(2002, 12, 31)),
            Satsbeløp(sats = 5, månedsbeløp = 2135, gyldigFraOgMed = LocalDate.of(2002, 1, 1), gyldigTilOgMed = LocalDate.of(2002, 12, 31)),
            Satsbeløp(sats = 6, månedsbeløp = 2668, gyldigFraOgMed = LocalDate.of(2002, 1, 1), gyldigTilOgMed = LocalDate.of(2002, 12, 31)),
            // 1.1.2003. Uendret i 2004.
            Satsbeløp(sats = 1, månedsbeløp = 545, gyldigFraOgMed = LocalDate.of(2003, 1, 1), gyldigTilOgMed = LocalDate.of(2004, 12, 31)),
            Satsbeløp(sats = 2, månedsbeløp = 833, gyldigFraOgMed = LocalDate.of(2003, 1, 1), gyldigTilOgMed = LocalDate.of(2004, 12, 31)),
            Satsbeløp(sats = 3, månedsbeløp = 1095, gyldigFraOgMed = LocalDate.of(2003, 1, 1), gyldigTilOgMed = LocalDate.of(2004, 12, 31)),
            Satsbeløp(sats = 4, månedsbeløp = 1611, gyldigFraOgMed = LocalDate.of(2003, 1, 1), gyldigTilOgMed = LocalDate.of(2004, 12, 31)),
            Satsbeløp(sats = 5, månedsbeløp = 2184, gyldigFraOgMed = LocalDate.of(2003, 1, 1), gyldigTilOgMed = LocalDate.of(2004, 12, 31)),
            Satsbeløp(sats = 6, månedsbeløp = 2729, gyldigFraOgMed = LocalDate.of(2003, 1, 1), gyldigTilOgMed = LocalDate.of(2004, 12, 31)),
            // 1.1.2005
            Satsbeløp(sats = 1, månedsbeløp = 553, gyldigFraOgMed = LocalDate.of(2005, 1, 1), gyldigTilOgMed = LocalDate.of(2005, 12, 31)),
            Satsbeløp(sats = 2, månedsbeløp = 845, gyldigFraOgMed = LocalDate.of(2005, 1, 1), gyldigTilOgMed = LocalDate.of(2005, 12, 31)),
            Satsbeløp(sats = 3, månedsbeløp = 1111, gyldigFraOgMed = LocalDate.of(2005, 1, 1), gyldigTilOgMed = LocalDate.of(2005, 12, 31)),
            Satsbeløp(sats = 4, månedsbeløp = 1635, gyldigFraOgMed = LocalDate.of(2005, 1, 1), gyldigTilOgMed = LocalDate.of(2005, 12, 31)),
            Satsbeløp(sats = 5, månedsbeløp = 2217, gyldigFraOgMed = LocalDate.of(2005, 1, 1), gyldigTilOgMed = LocalDate.of(2005, 12, 31)),
            Satsbeløp(sats = 6, månedsbeløp = 2770, gyldigFraOgMed = LocalDate.of(2005, 1, 1), gyldigTilOgMed = LocalDate.of(2005, 12, 31)),
            // 1.1.2006
            Satsbeløp(sats = 1, månedsbeløp = 562, gyldigFraOgMed = LocalDate.of(2006, 1, 1), gyldigTilOgMed = LocalDate.of(2006, 12, 31)),
            Satsbeløp(sats = 2, månedsbeløp = 859, gyldigFraOgMed = LocalDate.of(2006, 1, 1), gyldigTilOgMed = LocalDate.of(2006, 12, 31)),
            Satsbeløp(sats = 3, månedsbeløp = 1130, gyldigFraOgMed = LocalDate.of(2006, 1, 1), gyldigTilOgMed = LocalDate.of(2006, 12, 31)),
            Satsbeløp(sats = 4, månedsbeløp = 1663, gyldigFraOgMed = LocalDate.of(2006, 1, 1), gyldigTilOgMed = LocalDate.of(2006, 12, 31)),
            Satsbeløp(sats = 5, månedsbeløp = 2255, gyldigFraOgMed = LocalDate.of(2006, 1, 1), gyldigTilOgMed = LocalDate.of(2006, 12, 31)),
            Satsbeløp(sats = 6, månedsbeløp = 2817, gyldigFraOgMed = LocalDate.of(2006, 1, 1), gyldigTilOgMed = LocalDate.of(2006, 12, 31)),
            // 1.1.2007
            Satsbeløp(sats = 1, månedsbeløp = 572, gyldigFraOgMed = LocalDate.of(2007, 1, 1), gyldigTilOgMed = LocalDate.of(2007, 12, 31)),
            Satsbeløp(sats = 2, månedsbeløp = 874, gyldigFraOgMed = LocalDate.of(2007, 1, 1), gyldigTilOgMed = LocalDate.of(2007, 12, 31)),
            Satsbeløp(sats = 3, månedsbeløp = 1149, gyldigFraOgMed = LocalDate.of(2007, 1, 1), gyldigTilOgMed = LocalDate.of(2007, 12, 31)),
            Satsbeløp(sats = 4, månedsbeløp = 1691, gyldigFraOgMed = LocalDate.of(2007, 1, 1), gyldigTilOgMed = LocalDate.of(2007, 12, 31)),
            Satsbeløp(sats = 5, månedsbeløp = 2293, gyldigFraOgMed = LocalDate.of(2007, 1, 1), gyldigTilOgMed = LocalDate.of(2007, 12, 31)),
            Satsbeløp(sats = 6, månedsbeløp = 2865, gyldigFraOgMed = LocalDate.of(2007, 1, 1), gyldigTilOgMed = LocalDate.of(2007, 12, 31)),
            // 1.1.2008
            Satsbeløp(sats = 1, månedsbeløp = 585, gyldigFraOgMed = LocalDate.of(2008, 1, 1), gyldigTilOgMed = LocalDate.of(2008, 12, 31)),
            Satsbeløp(sats = 2, månedsbeløp = 894, gyldigFraOgMed = LocalDate.of(2008, 1, 1), gyldigTilOgMed = LocalDate.of(2008, 12, 31)),
            Satsbeløp(sats = 3, månedsbeløp = 1175, gyldigFraOgMed = LocalDate.of(2008, 1, 1), gyldigTilOgMed = LocalDate.of(2008, 12, 31)),
            Satsbeløp(sats = 4, månedsbeløp = 1730, gyldigFraOgMed = LocalDate.of(2008, 1, 1), gyldigTilOgMed = LocalDate.of(2008, 12, 31)),
            Satsbeløp(sats = 5, månedsbeløp = 2346, gyldigFraOgMed = LocalDate.of(2008, 1, 1), gyldigTilOgMed = LocalDate.of(2008, 12, 31)),
            Satsbeløp(sats = 6, månedsbeløp = 2931, gyldigFraOgMed = LocalDate.of(2008, 1, 1), gyldigTilOgMed = LocalDate.of(2008, 12, 31)),
            // 1.1.2009
            Satsbeløp(sats = 1, månedsbeløp = 599, gyldigFraOgMed = LocalDate.of(2009, 1, 1), gyldigTilOgMed = LocalDate.of(2009, 12, 31)),
            Satsbeløp(sats = 2, månedsbeløp = 915, gyldigFraOgMed = LocalDate.of(2009, 1, 1), gyldigTilOgMed = LocalDate.of(2009, 12, 31)),
            Satsbeløp(sats = 3, månedsbeløp = 1202, gyldigFraOgMed = LocalDate.of(2009, 1, 1), gyldigTilOgMed = LocalDate.of(2009, 12, 31)),
            Satsbeløp(sats = 4, månedsbeløp = 1770, gyldigFraOgMed = LocalDate.of(2009, 1, 1), gyldigTilOgMed = LocalDate.of(2009, 12, 31)),
            Satsbeløp(sats = 5, månedsbeløp = 2400, gyldigFraOgMed = LocalDate.of(2009, 1, 1), gyldigTilOgMed = LocalDate.of(2009, 12, 31)),
            Satsbeløp(sats = 6, månedsbeløp = 2999, gyldigFraOgMed = LocalDate.of(2009, 1, 1), gyldigTilOgMed = LocalDate.of(2009, 12, 31)),
            // 1.1.2010
            Satsbeløp(sats = 1, månedsbeløp = 609, gyldigFraOgMed = LocalDate.of(2010, 1, 1), gyldigTilOgMed = LocalDate.of(2010, 12, 31)),
            Satsbeløp(sats = 2, månedsbeløp = 930, gyldigFraOgMed = LocalDate.of(2010, 1, 1), gyldigTilOgMed = LocalDate.of(2010, 12, 31)),
            Satsbeløp(sats = 3, månedsbeløp = 1221, gyldigFraOgMed = LocalDate.of(2010, 1, 1), gyldigTilOgMed = LocalDate.of(2010, 12, 31)),
            Satsbeløp(sats = 4, månedsbeløp = 1798, gyldigFraOgMed = LocalDate.of(2010, 1, 1), gyldigTilOgMed = LocalDate.of(2010, 12, 31)),
            Satsbeløp(sats = 5, månedsbeløp = 2439, gyldigFraOgMed = LocalDate.of(2010, 1, 1), gyldigTilOgMed = LocalDate.of(2010, 12, 31)),
            Satsbeløp(sats = 6, månedsbeløp = 3047, gyldigFraOgMed = LocalDate.of(2010, 1, 1), gyldigTilOgMed = LocalDate.of(2010, 12, 31)),
            // 1.1.2011
            Satsbeløp(sats = 1, månedsbeløp = 621, gyldigFraOgMed = LocalDate.of(2011, 1, 1), gyldigTilOgMed = LocalDate.of(2011, 12, 31)),
            Satsbeløp(sats = 2, månedsbeløp = 948, gyldigFraOgMed = LocalDate.of(2011, 1, 1), gyldigTilOgMed = LocalDate.of(2011, 12, 31)),
            Satsbeløp(sats = 3, månedsbeløp = 1245, gyldigFraOgMed = LocalDate.of(2011, 1, 1), gyldigTilOgMed = LocalDate.of(2011, 12, 31)),
            Satsbeløp(sats = 4, månedsbeløp = 1833, gyldigFraOgMed = LocalDate.of(2011, 1, 1), gyldigTilOgMed = LocalDate.of(2011, 12, 31)),
            Satsbeløp(sats = 5, månedsbeløp = 2486, gyldigFraOgMed = LocalDate.of(2011, 1, 1), gyldigTilOgMed = LocalDate.of(2011, 12, 31)),
            Satsbeløp(sats = 6, månedsbeløp = 3105, gyldigFraOgMed = LocalDate.of(2011, 1, 1), gyldigTilOgMed = LocalDate.of(2011, 12, 31)),
            // 1.1.2012. For sats 2 sier rundskrivet 968 kroner i måneden, men Infotrygd utbetalte 964 (11 568 / 12),
            // se ADR-0011.
            Satsbeløp(sats = 1, månedsbeløp = 631, gyldigFraOgMed = LocalDate.of(2012, 1, 1), gyldigTilOgMed = LocalDate.of(2012, 12, 31)),
            Satsbeløp(sats = 2, månedsbeløp = 964, gyldigFraOgMed = LocalDate.of(2012, 1, 1), gyldigTilOgMed = LocalDate.of(2012, 12, 31)),
            Satsbeløp(sats = 3, månedsbeløp = 1265, gyldigFraOgMed = LocalDate.of(2012, 1, 1), gyldigTilOgMed = LocalDate.of(2012, 12, 31)),
            Satsbeløp(sats = 4, månedsbeløp = 1863, gyldigFraOgMed = LocalDate.of(2012, 1, 1), gyldigTilOgMed = LocalDate.of(2012, 12, 31)),
            Satsbeløp(sats = 5, månedsbeløp = 2526, gyldigFraOgMed = LocalDate.of(2012, 1, 1), gyldigTilOgMed = LocalDate.of(2012, 12, 31)),
            Satsbeløp(sats = 6, månedsbeløp = 3155, gyldigFraOgMed = LocalDate.of(2012, 1, 1), gyldigTilOgMed = LocalDate.of(2012, 12, 31)),
            // 1.1.2013
            Satsbeløp(sats = 1, månedsbeløp = 643, gyldigFraOgMed = LocalDate.of(2013, 1, 1), gyldigTilOgMed = LocalDate.of(2013, 12, 31)),
            Satsbeløp(sats = 2, månedsbeløp = 983, gyldigFraOgMed = LocalDate.of(2013, 1, 1), gyldigTilOgMed = LocalDate.of(2013, 12, 31)),
            Satsbeløp(sats = 3, månedsbeløp = 1289, gyldigFraOgMed = LocalDate.of(2013, 1, 1), gyldigTilOgMed = LocalDate.of(2013, 12, 31)),
            Satsbeløp(sats = 4, månedsbeløp = 1899, gyldigFraOgMed = LocalDate.of(2013, 1, 1), gyldigTilOgMed = LocalDate.of(2013, 12, 31)),
            Satsbeløp(sats = 5, månedsbeløp = 2574, gyldigFraOgMed = LocalDate.of(2013, 1, 1), gyldigTilOgMed = LocalDate.of(2013, 12, 31)),
            Satsbeløp(sats = 6, månedsbeløp = 3215, gyldigFraOgMed = LocalDate.of(2013, 1, 1), gyldigTilOgMed = LocalDate.of(2013, 12, 31)),
            // 1.1.2014. Uendret i 2015.
            Satsbeløp(sats = 1, månedsbeløp = 653, gyldigFraOgMed = LocalDate.of(2014, 1, 1), gyldigTilOgMed = LocalDate.of(2015, 12, 31)),
            Satsbeløp(sats = 2, månedsbeløp = 998, gyldigFraOgMed = LocalDate.of(2014, 1, 1), gyldigTilOgMed = LocalDate.of(2015, 12, 31)),
            Satsbeløp(sats = 3, månedsbeløp = 1309, gyldigFraOgMed = LocalDate.of(2014, 1, 1), gyldigTilOgMed = LocalDate.of(2015, 12, 31)),
            Satsbeløp(sats = 4, månedsbeløp = 1928, gyldigFraOgMed = LocalDate.of(2014, 1, 1), gyldigTilOgMed = LocalDate.of(2015, 12, 31)),
            Satsbeløp(sats = 5, månedsbeløp = 2613, gyldigFraOgMed = LocalDate.of(2014, 1, 1), gyldigTilOgMed = LocalDate.of(2015, 12, 31)),
            Satsbeløp(sats = 6, månedsbeløp = 3264, gyldigFraOgMed = LocalDate.of(2014, 1, 1), gyldigTilOgMed = LocalDate.of(2015, 12, 31)),
            // 1.1.2016. Uendret i 2017.
            Satsbeløp(sats = 1, månedsbeløp = 670, gyldigFraOgMed = LocalDate.of(2016, 1, 1), gyldigTilOgMed = LocalDate.of(2017, 12, 31)),
            Satsbeløp(sats = 2, månedsbeløp = 1023, gyldigFraOgMed = LocalDate.of(2016, 1, 1), gyldigTilOgMed = LocalDate.of(2017, 12, 31)),
            Satsbeløp(sats = 3, månedsbeløp = 1342, gyldigFraOgMed = LocalDate.of(2016, 1, 1), gyldigTilOgMed = LocalDate.of(2017, 12, 31)),
            Satsbeløp(sats = 4, månedsbeløp = 1977, gyldigFraOgMed = LocalDate.of(2016, 1, 1), gyldigTilOgMed = LocalDate.of(2017, 12, 31)),
            Satsbeløp(sats = 5, månedsbeløp = 2679, gyldigFraOgMed = LocalDate.of(2016, 1, 1), gyldigTilOgMed = LocalDate.of(2017, 12, 31)),
            Satsbeløp(sats = 6, månedsbeløp = 3346, gyldigFraOgMed = LocalDate.of(2016, 1, 1), gyldigTilOgMed = LocalDate.of(2017, 12, 31)),
            // 1.1.2018
            Satsbeløp(sats = 1, månedsbeløp = 678, gyldigFraOgMed = LocalDate.of(2018, 1, 1), gyldigTilOgMed = LocalDate.of(2018, 12, 31)),
            Satsbeløp(sats = 2, månedsbeløp = 1035, gyldigFraOgMed = LocalDate.of(2018, 1, 1), gyldigTilOgMed = LocalDate.of(2018, 12, 31)),
            Satsbeløp(sats = 3, månedsbeløp = 1357, gyldigFraOgMed = LocalDate.of(2018, 1, 1), gyldigTilOgMed = LocalDate.of(2018, 12, 31)),
            Satsbeløp(sats = 4, månedsbeløp = 1999, gyldigFraOgMed = LocalDate.of(2018, 1, 1), gyldigTilOgMed = LocalDate.of(2018, 12, 31)),
            Satsbeløp(sats = 5, månedsbeløp = 2709, gyldigFraOgMed = LocalDate.of(2018, 1, 1), gyldigTilOgMed = LocalDate.of(2018, 12, 31)),
            Satsbeløp(sats = 6, månedsbeløp = 3383, gyldigFraOgMed = LocalDate.of(2018, 1, 1), gyldigTilOgMed = LocalDate.of(2018, 12, 31)),
            // 1.1.2019. Uendret i 2020, 2021 og 2022.
            Satsbeløp(sats = 1, månedsbeløp = 686, gyldigFraOgMed = LocalDate.of(2019, 1, 1), gyldigTilOgMed = LocalDate.of(2022, 12, 31)),
            Satsbeløp(sats = 2, månedsbeløp = 1047, gyldigFraOgMed = LocalDate.of(2019, 1, 1), gyldigTilOgMed = LocalDate.of(2022, 12, 31)),
            Satsbeløp(sats = 3, månedsbeløp = 1372, gyldigFraOgMed = LocalDate.of(2019, 1, 1), gyldigTilOgMed = LocalDate.of(2022, 12, 31)),
            Satsbeløp(sats = 4, månedsbeløp = 2021, gyldigFraOgMed = LocalDate.of(2019, 1, 1), gyldigTilOgMed = LocalDate.of(2022, 12, 31)),
            Satsbeløp(sats = 5, månedsbeløp = 2739, gyldigFraOgMed = LocalDate.of(2019, 1, 1), gyldigTilOgMed = LocalDate.of(2022, 12, 31)),
            Satsbeløp(sats = 6, månedsbeløp = 3421, gyldigFraOgMed = LocalDate.of(2019, 1, 1), gyldigTilOgMed = LocalDate.of(2022, 12, 31)),
            // 1.1.2023
            Satsbeløp(sats = 1, månedsbeløp = 705, gyldigFraOgMed = LocalDate.of(2023, 1, 1), gyldigTilOgMed = LocalDate.of(2023, 6, 30)),
            Satsbeløp(sats = 2, månedsbeløp = 1076, gyldigFraOgMed = LocalDate.of(2023, 1, 1), gyldigTilOgMed = LocalDate.of(2023, 6, 30)),
            Satsbeløp(sats = 3, månedsbeløp = 1410, gyldigFraOgMed = LocalDate.of(2023, 1, 1), gyldigTilOgMed = LocalDate.of(2023, 6, 30)),
            Satsbeløp(sats = 4, månedsbeløp = 2078, gyldigFraOgMed = LocalDate.of(2023, 1, 1), gyldigTilOgMed = LocalDate.of(2023, 6, 30)),
            Satsbeløp(sats = 5, månedsbeløp = 2816, gyldigFraOgMed = LocalDate.of(2023, 1, 1), gyldigTilOgMed = LocalDate.of(2023, 6, 30)),
            Satsbeløp(sats = 6, månedsbeløp = 3517, gyldigFraOgMed = LocalDate.of(2023, 1, 1), gyldigTilOgMed = LocalDate.of(2023, 6, 30)),
            // 1.7.2023
            Satsbeløp(sats = 1, månedsbeløp = 724, gyldigFraOgMed = LocalDate.of(2023, 7, 1), gyldigTilOgMed = LocalDate.of(2023, 12, 31)),
            Satsbeløp(sats = 2, månedsbeløp = 1104, gyldigFraOgMed = LocalDate.of(2023, 7, 1), gyldigTilOgMed = LocalDate.of(2023, 12, 31)),
            Satsbeløp(sats = 3, månedsbeløp = 1447, gyldigFraOgMed = LocalDate.of(2023, 7, 1), gyldigTilOgMed = LocalDate.of(2023, 12, 31)),
            Satsbeløp(sats = 4, månedsbeløp = 2131, gyldigFraOgMed = LocalDate.of(2023, 7, 1), gyldigTilOgMed = LocalDate.of(2023, 12, 31)),
            Satsbeløp(sats = 5, månedsbeløp = 2887, gyldigFraOgMed = LocalDate.of(2023, 7, 1), gyldigTilOgMed = LocalDate.of(2023, 12, 31)),
            Satsbeløp(sats = 6, månedsbeløp = 3606, gyldigFraOgMed = LocalDate.of(2023, 7, 1), gyldigTilOgMed = LocalDate.of(2023, 12, 31)),
            // 1.1.2024. Uendret i 2025 og 2026
            Satsbeløp(sats = 1, månedsbeløp = 752, gyldigFraOgMed = LocalDate.of(2024, 1, 1), gyldigTilOgMed = null),
            Satsbeløp(sats = 2, månedsbeløp = 1146, gyldigFraOgMed = LocalDate.of(2024, 1, 1), gyldigTilOgMed = null),
            Satsbeløp(sats = 3, månedsbeløp = 1502, gyldigFraOgMed = LocalDate.of(2024, 1, 1), gyldigTilOgMed = null),
            Satsbeløp(sats = 4, månedsbeløp = 2212, gyldigFraOgMed = LocalDate.of(2024, 1, 1), gyldigTilOgMed = null),
            Satsbeløp(sats = 5, månedsbeløp = 2997, gyldigFraOgMed = LocalDate.of(2024, 1, 1), gyldigTilOgMed = null),
            Satsbeløp(sats = 6, månedsbeløp = 3744, gyldigFraOgMed = LocalDate.of(2024, 1, 1), gyldigTilOgMed = null),
        )

    /** Kaster [UgyldigInput] hvis datoen er utenfor listen. Bruker aldri nærmeste beløp. */
    fun hent(
        sats: Int,
        dato: LocalDate,
    ): Int =
        alle
            .singleOrNull { it.sats == sats && it.gjelder(dato) }
            ?.månedsbeløp
            ?: throw UgyldigInput("Finner ikke satsbeløp for sats $sats den $dato.")

    private fun Satsbeløp.gjelder(dato: LocalDate) = !dato.isBefore(gyldigFraOgMed) && (gyldigTilOgMed == null || !dato.isAfter(gyldigTilOgMed))
}
