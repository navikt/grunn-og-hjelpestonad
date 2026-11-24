package no.nav.grunn.og.hjelpestonad.oppdrag.simulering

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import no.nav.grunn.og.hjelpestonad.oppdrag.DetaljertSimuleringResultat
import no.nav.grunn.og.hjelpestonad.oppdrag.FagOmrådeKode
import no.nav.grunn.og.hjelpestonad.oppdrag.SimuleringMottaker
import no.nav.grunn.og.hjelpestonad.oppdrag.SimulertPostering
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test

private val MANUELL_FAGOMRÅDEKODE = FagOmrådeKode.GRUNNSTØNAD_INFOTRYGD_MANUELT
private val SIMULERING_HENTET_DATO: LocalDate = LocalDate.of(2026, 11, 19)

/**
 * Ren test uten Spring, jf. ADR-0008. Tilfellene fra «ytelse på 10000 korrigert til 2000» og nedover er oversatt fra
 * `SimuleringUtilTest.kt` i familie-ba-sak, med samme forventede beløp. Testene med manuelle posteringer sender inn
 * kodene selv, slik at de ikke avhenger av [SimuleringOppsummerer.MANUELLE_FAGOMRÅDEKODER].
 */
class SimuleringOppsummererTest {
    @Test
    fun `simulering fra dev med debet fra februar til desember 2026 gir etterbetaling for månedene som har forfalt`() {
        val simuleringsposteringerFraDev =
            (2..11).map { måned -> postering(måned = "2026-%02d".format(måned), beløp = 752, forfallsdato = "2026-11-19") } +
                postering(måned = "2026-12", beløp = 752, forfallsdato = "2026-12-10")

        val simuleringsoppsummering = oppsummer(simuleringsposteringerFraDev)

        assertThat(simuleringsoppsummering.fom).isEqualTo(LocalDate.of(2026, 2, 1))
        assertThat(simuleringsoppsummering.tomSisteUtbetaling).isEqualTo(LocalDate.of(2026, 11, 30))
        assertBeløp(simuleringsoppsummering.etterbetaling, 7_520)
        assertBeløp(simuleringsoppsummering.feilutbetaling, 0)
        assertThat(simuleringsoppsummering.nesteUtbetaling).isEqualTo(
            NesteUtbetaling(
                fom = LocalDate.of(2026, 12, 1),
                tom = LocalDate.of(2026, 12, 31),
                forfallsdato = LocalDate.of(2026, 12, 10),
                beløp = BigDecimal.valueOf(752),
            ),
        )
        assertThat(simuleringsoppsummering.ukjenteFagområdekoder).isEmpty()

        assertThat(simuleringsoppsummering.perioder).hasSize(2)
        val februarTilNovember = simuleringsoppsummering.perioder[0]
        val desember = simuleringsoppsummering.perioder[1]
        assertThat(februarTilNovember.fom).isEqualTo(LocalDate.of(2026, 2, 1))
        assertThat(februarTilNovember.tom).isEqualTo(LocalDate.of(2026, 11, 30))
        assertThat(februarTilNovember.antallMåneder).isEqualTo(10)
        assertThat(februarTilNovember.erNesteUtbetaling).isFalse()
        assertSimuleringsbeløp(februarTilNovember.beløpPerMåned, nyttBeløp = 752, tidligereUtbetalt = 0, resultat = 752, etterbetaling = 752)
        assertSimuleringsbeløp(februarTilNovember.sumForPerioden, nyttBeløp = 7_520, tidligereUtbetalt = 0, resultat = 7_520, etterbetaling = 7_520)

        assertThat(desember.antallMåneder).isEqualTo(1)
        assertThat(desember.erNesteUtbetaling).isTrue()
        assertSimuleringsbeløp(desember.beløpPerMåned, nyttBeløp = 752, tidligereUtbetalt = 0, resultat = 752, etterbetaling = 0)
    }

    @Test
    fun `nytt beløp, tidligere utbetalt og resultat uten feilutbetaling`() {
        val simuleringsmåned =
            enMåned(
                postering(beløp = 100),
                postering(beløp = 100),
                postering(beløp = -99),
                postering(beløp = -99),
            )

        assertSimuleringsbeløp(simuleringsmåned.beløp, nyttBeløp = 200, tidligereUtbetalt = 198, resultat = 2, etterbetaling = 2)
    }

    @Test
    fun `nytt beløp, tidligere utbetalt og resultat med feilutbetaling`() {
        val simuleringsmåned =
            enMåned(
                postering(beløp = 100),
                postering(beløp = 100),
                postering(beløp = -99),
                postering(beløp = -99),
                postering(beløp = 98, posteringstype = "FEILUTBETALING"),
                postering(beløp = 98, posteringstype = "FEILUTBETALING"),
            )

        assertSimuleringsbeløp(simuleringsmåned.beløp, nyttBeløp = 4, tidligereUtbetalt = 198, resultat = -196, feilutbetaling = 196, etterbetaling = 0)
    }

    @Test
    fun `nytt beløp, tidligere utbetalt og resultat når feilutbetalingen blir mindre`() {
        val simuleringsmåned =
            enMåned(
                postering(beløp = 100),
                postering(beløp = 100),
                postering(beløp = -99),
                postering(beløp = -99),
                postering(beløp = 98, posteringstype = "FEILUTBETALING"),
                postering(beløp = -99, posteringstype = "FEILUTBETALING"),
            )

        assertSimuleringsbeløp(simuleringsmåned.beløp, nyttBeløp = 200, tidligereUtbetalt = 197, resultat = 3, feilutbetaling = 98, etterbetaling = 0)
    }

    @Test
    fun `negativ feilutbetaling gir etterbetaling for resten av ytelsen og ingen feilutbetaling`() {
        val simuleringsoppsummering =
            oppsummer(
                listOf(
                    postering(beløp = -500, posteringstype = "FEILUTBETALING"),
                    postering(beløp = -2_000),
                    postering(beløp = 3_000),
                    postering(beløp = -500),
                ),
            )

        assertBeløp(simuleringsoppsummering.etterbetaling, 500)
        assertBeløp(simuleringsoppsummering.feilutbetaling, 0)
    }

    @Test
    fun `positiv feilutbetaling gir ingen etterbetaling og summen som feilutbetaling`() {
        val simuleringsoppsummering =
            oppsummer(
                listOf(
                    postering(beløp = 500, posteringstype = "FEILUTBETALING"),
                    postering(beløp = -2_000),
                    postering(beløp = 3_000),
                    postering(beløp = -500),
                ),
            )

        assertBeløp(simuleringsoppsummering.etterbetaling, 0)
        assertBeløp(simuleringsoppsummering.feilutbetaling, 500)
    }

    @Test
    fun `bare måneder som har forfalt, gir etterbetaling`() {
        val simuleringsoppsummering =
            oppsummer(
                listOf(
                    postering(måned = "2026-10", beløp = 200, forfallsdato = "2026-11-18"),
                    postering(måned = "2026-11", beløp = 100, forfallsdato = "2026-11-20"),
                ),
            )

        assertThat(simuleringsoppsummering.perioder.map { it.beløpPerMåned.etterbetaling.toInt() }).containsExactly(200, 0)
    }

    @Test
    fun `forfall samme dag som simuleringen gir etterbetaling`() {
        val simuleringsmåned = enMåned(postering(beløp = 752, forfallsdato = "2026-11-19"))

        assertBeløp(simuleringsmåned.beløp.etterbetaling, 752)
    }

    @Test
    fun `ytelse på 10000 korrigert til 2000`() {
        val simuleringsmåned =
            enMåned(
                postering(beløp = -10_000, betalingstype = "KREDIT"),
                postering(beløp = 2_000),
                postering(beløp = 8_000, posteringstype = "FEILUTBETALING"),
                postering(beløp = -8_000, posteringstype = "MOTP", betalingstype = "KREDIT"),
                postering(beløp = 8_000),
            )

        assertSimuleringsbeløp(simuleringsmåned.beløp, nyttBeløp = 2_000, tidligereUtbetalt = 10_000, resultat = -8_000, feilutbetaling = 8_000, etterbetaling = 0)
    }

    @Test
    fun `ytelse på 2000 korrigert til 3000`() {
        val simuleringsmåned =
            enMåned(
                postering(beløp = -2_000, betalingstype = "KREDIT"),
                postering(beløp = 3_000),
                postering(beløp = -1_000, posteringstype = "FEILUTBETALING", betalingstype = "KREDIT"),
                postering(beløp = 1_000, posteringstype = "MOTP"),
                postering(beløp = -1_000, betalingstype = "KREDIT"),
            )

        assertSimuleringsbeløp(simuleringsmåned.beløp, nyttBeløp = 3_000, tidligereUtbetalt = 2_000, resultat = 1_000, etterbetaling = 0)
    }

    @Test
    fun `ytelse på 3000 korrigert til 12000`() {
        val simuleringsmåned =
            enMåned(
                postering(beløp = -3_000, betalingstype = "KREDIT"),
                postering(beløp = 12_000),
                postering(beløp = -7_000, posteringstype = "FEILUTBETALING", betalingstype = "KREDIT"),
                postering(beløp = 7_000, posteringstype = "MOTP"),
                postering(beløp = -7_000, betalingstype = "KREDIT"),
            )

        assertSimuleringsbeløp(simuleringsmåned.beløp, nyttBeløp = 12_000, tidligereUtbetalt = 3_000, resultat = 9_000, etterbetaling = 2_000)
    }

    @Test
    fun `manuelt trekk der deler er trukket`() {
        val simuleringsmåned =
            enMåned(
                postering(beløp = 305),
                postering(beløp = -198, posteringstype = "JUSTERING"),
                postering(beløp = 305, posteringstype = "JUSTERING"),
                postering(beløp = -305),
                postering(beløp = -107, posteringstype = "JUSTERING", fagområdekode = MANUELL_FAGOMRÅDEKODE.name),
                postering(beløp = 165, fagområdekode = MANUELL_FAGOMRÅDEKODE.name),
                manuelleFagområdekoder = setOf(MANUELL_FAGOMRÅDEKODE),
            )

        assertSimuleringsbeløp(simuleringsmåned.beløp, nyttBeløp = 305, tidligereUtbetalt = 140, resultat = 165, etterbetaling = 165, manuellPostering = 165)
    }

    @Test
    fun `manuelt trekk der alt er trukket`() {
        val simuleringsmåned =
            enMåned(
                postering(beløp = 305),
                postering(beløp = -153, posteringstype = "JUSTERING"),
                postering(beløp = 306, posteringstype = "JUSTERING"),
                postering(beløp = -305),
                postering(beløp = -153, posteringstype = "JUSTERING", fagområdekode = MANUELL_FAGOMRÅDEKODE.name),
                postering(beløp = 305, fagområdekode = MANUELL_FAGOMRÅDEKODE.name),
                manuelleFagområdekoder = setOf(MANUELL_FAGOMRÅDEKODE),
            )

        assertSimuleringsbeløp(simuleringsmåned.beløp, nyttBeløp = 305, tidligereUtbetalt = 0, resultat = 305, etterbetaling = 305, manuellPostering = 305)
    }

    @Test
    fun `feilutbetalinger som ikke er reelle, gir riktig resultat`() {
        val simuleringsmåned =
            enMåned(
                postering(beløp = 658),
                postering(beløp = -657),
                postering(beløp = -50, fagområdekode = MANUELL_FAGOMRÅDEKODE.name),
                postering(beløp = 46),
                postering(beløp = 46, posteringstype = "FEILUTBETALING"),
                postering(beløp = -46, posteringstype = "MOTP"),
                postering(beløp = 3, fagområdekode = MANUELL_FAGOMRÅDEKODE.name),
                postering(beløp = 3, posteringstype = "FEILUTBETALING", fagområdekode = MANUELL_FAGOMRÅDEKODE.name),
                postering(beløp = -3, posteringstype = "MOTP", fagområdekode = MANUELL_FAGOMRÅDEKODE.name),
                manuelleFagområdekoder = setOf(MANUELL_FAGOMRÅDEKODE),
            )

        assertSimuleringsbeløp(
            simuleringsmåned.beløp,
            nyttBeløp = 658,
            tidligereUtbetalt = 707,
            resultat = -49,
            feilutbetaling = 49,
            etterbetaling = 0,
            manuellPostering = -50,
        )
    }

    /*
     * De neste testene er førstegangsbehandlingen og revurderingene fra ba-sak i november 2021:
     * 2021   Feb    Mar    Apr    Mai    Jun    Jul    Aug    Sep    Okt    Nov
     * 18/11  17153  17153  17153  18195  18195  18195  18195  18195  18195
     * 22/11  17153  17153  17153  17257  17257  17257  17257  18195  18195
     * 23/11  17341  17341  17341  18382  18382  18382  18382  18382  18382  18382
     */

    @Test
    fun `førstegangsbehandling 18 november 2021`() {
        val simuleringsoppsummering =
            oppsummer(
                månedlige("2021-02", 3, 17_153) + månedlige("2021-05", 6, 18_195),
                simuleringHentetDato = LocalDate.of(2021, 11, 18),
            )

        assertBeløp(simuleringsoppsummering.feilutbetaling, 0)
        assertBeløp(simuleringsoppsummering.etterbetaling, 160_629)
        assertThat(simuleringsoppsummering.nesteUtbetaling).isNull()
    }

    @Test
    fun `revurdering 22 november 2021 gir feilutbetaling`() {
        val simuleringsoppsummering =
            oppsummer(
                månedlige("2021-02", 3, -17_153) + månedlige("2021-05", 6, -18_195) +
                    månedlige("2021-02", 3, 17_153) + månedlige("2021-05", 4, 17_257) + månedlige("2021-09", 2, 18_195) +
                    månedlige("2021-05", 4, 938, posteringstype = "FEILUTBETALING") +
                    månedlige("2021-05", 4, -938, posteringstype = "MOTP") +
                    månedlige("2021-05", 4, 938),
                simuleringHentetDato = LocalDate.of(2021, 11, 22),
            )

        assertBeløp(simuleringsoppsummering.feilutbetaling, 3_752)
        assertBeløp(simuleringsoppsummering.etterbetaling, 0)
    }

    @Test
    fun `revurdering 23 november 2021 gir etterbetaling for månedene før neste utbetaling`() {
        val simuleringsoppsummering =
            oppsummer(
                månedlige("2021-02", 3, -17_153) + månedlige("2021-05", 4, -17_257) + månedlige("2021-09", 2, -18_195) +
                    månedlige("2021-02", 3, 17_341) + månedlige("2021-05", 7, 18_382) +
                    månedlige("2021-05", 4, -938) +
                    månedlige("2021-05", 4, -938, posteringstype = "FEILUTBETALING") +
                    månedlige("2021-05", 4, 938, posteringstype = "MOTP"),
                simuleringHentetDato = LocalDate.of(2021, 11, 23),
            )

        assertBeløp(simuleringsoppsummering.feilutbetaling, 0)
        // ba-sak får 20 068 fordi testen der simulerer «i dag», så november 2021 også har forfalt.
        assertBeløp(simuleringsoppsummering.etterbetaling, 1_686)
        val nesteUtbetaling = requireNotNull(simuleringsoppsummering.nesteUtbetaling)
        assertThat(nesteUtbetaling.fom).isEqualTo(LocalDate.of(2021, 11, 1))
        assertBeløp(nesteUtbetaling.beløp, 18_382)
        val maiTilAugust = simuleringsoppsummering.perioder.single { it.fom == LocalDate.of(2021, 5, 1) }
        assertThat(maiTilAugust.antallMåneder).isEqualTo(4)
        assertSimuleringsbeløp(maiTilAugust.beløpPerMåned, nyttBeløp = 18_382, tidligereUtbetalt = 17_257, resultat = 1_125, etterbetaling = 187)
    }

    @Test
    fun `måned med feilutbetaling blir ikke neste utbetaling`() {
        val simuleringsoppsummering =
            oppsummer(
                listOf(
                    postering(måned = "2026-12", beløp = 100, posteringstype = "FEILUTBETALING", forfallsdato = "2026-12-10"),
                    postering(måned = "2027-01", beløp = 752, forfallsdato = "2027-01-10"),
                ),
            )

        assertThat(simuleringsoppsummering.nesteUtbetaling?.fom).isEqualTo(LocalDate.of(2027, 1, 1))
        assertBeløp(simuleringsoppsummering.feilutbetaling, 100)
    }

    @Test
    fun `måned med bare andre posteringstyper tas ikke med`() {
        val simuleringsoppsummering =
            oppsummer(
                listOf(
                    postering(måned = "2026-12", beløp = -200, posteringstype = "FORSKUDSSKATT", forfallsdato = "2026-12-10"),
                    postering(måned = "2027-01", beløp = 752, forfallsdato = "2027-01-10"),
                ),
            )

        assertThat(simuleringsoppsummering.perioder.map { it.fom }).containsExactly(LocalDate.of(2027, 1, 1))
        assertThat(simuleringsoppsummering.fom).isEqualTo(LocalDate.of(2027, 1, 1))
        assertThat(simuleringsoppsummering.nesteUtbetaling?.fom).isEqualTo(LocalDate.of(2027, 1, 1))
    }

    @Test
    fun `hull i simuleringen er ikke en periode i oppsummeringen`() {
        val simuleringsoppsummering =
            oppsummer(
                listOf(
                    postering(måned = "2026-01", beløp = 752, forfallsdato = "2026-01-20"),
                    postering(måned = "2026-03", beløp = 752, forfallsdato = "2026-03-20"),
                ),
            )

        assertThat(simuleringsoppsummering.nesteUtbetaling).isNull()
        assertThat(simuleringsoppsummering.perioder.map { it.fom }).containsExactly(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1))
        assertThat(simuleringsoppsummering.perioder.map { it.tom }).containsExactly(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 3, 31))
    }

    @Test
    fun `måneder etter neste utbetaling er ikke med i perioder`() {
        val simuleringsoppsummering =
            oppsummer(
                listOf(
                    postering(måned = "2026-10", beløp = 752, forfallsdato = "2026-10-20"),
                    postering(måned = "2026-11", beløp = 752, forfallsdato = "2026-11-20"),
                    postering(måned = "2026-12", beløp = 752, forfallsdato = "2026-12-10"),
                    postering(måned = "2027-01", beløp = 752, forfallsdato = "2027-01-10"),
                ),
            )

        assertThat(simuleringsoppsummering.nesteUtbetaling?.fom).isEqualTo(LocalDate.of(2026, 11, 1))
        assertThat(simuleringsoppsummering.perioder.map { it.fom }).containsExactly(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1))
        assertThat(simuleringsoppsummering.perioder.last().erNesteUtbetaling).isTrue()
        assertBeløp(simuleringsoppsummering.etterbetaling, 752)
    }

    @Test
    fun `andre posteringstyper i en måned med ytelse gir ikke beløp`() {
        val simuleringsoppsummering =
            oppsummer(
                listOf(
                    postering(beløp = 752),
                    postering(beløp = -752, posteringstype = "MOTP"),
                ),
            )

        assertSimuleringsbeløp(simuleringsoppsummering.perioder.single().beløpPerMåned, nyttBeløp = 752, tidligereUtbetalt = 0, resultat = 752, etterbetaling = 752)
    }

    @Test
    fun `posteringer i samme måned med ulik til og med-dato gir UgyldigInput`() {
        val simuleringsposteringer =
            listOf(
                simuleringspostering(fom = LocalDate.of(2026, 1, 1), tom = LocalDate.of(2026, 1, 31)),
                simuleringspostering(fom = LocalDate.of(2026, 1, 1), tom = LocalDate.of(2026, 1, 15)),
            )

        assertThatThrownBy { SimuleringOppsummerer.lagMåneder(simuleringsposteringer, SIMULERING_HENTET_DATO, emptySet()) }
            .isInstanceOf(UgyldigInput::class.java)
            .hasMessage("Posteringene i en måned i simuleringen fra Oppdrag har ulik til og med-dato.")
    }

    @Test
    fun `posteringer i samme måned med ulik forfallsdato gir UgyldigInput`() {
        val simuleringsposteringer =
            listOf(
                simuleringspostering(fom = LocalDate.of(2026, 1, 1), tom = LocalDate.of(2026, 1, 31)),
                simuleringspostering(fom = LocalDate.of(2026, 1, 1), tom = LocalDate.of(2026, 1, 31))
                    .copy(forfallsdato = LocalDate.of(2026, 11, 19)),
            )

        assertThatThrownBy { SimuleringOppsummerer.lagMåneder(simuleringsposteringer, SIMULERING_HENTET_DATO, emptySet()) }
            .isInstanceOf(UgyldigInput::class.java)
            .hasMessage("Posteringene i en måned i simuleringen fra Oppdrag har ulik forfallsdato.")
    }

    @Test
    fun `posteringer som ikke gir beløp, kan ha andre datoer og er ikke med i måneden`() {
        val simuleringsposteringer =
            listOf(
                simuleringspostering(fom = LocalDate.of(2026, 1, 1), tom = LocalDate.of(2026, 1, 31)),
                simuleringspostering(fom = LocalDate.of(2026, 1, 1), tom = LocalDate.of(2026, 1, 15))
                    .copy(posteringstype = Posteringstype.MOTP, forfallsdato = LocalDate.of(2026, 11, 19)),
            )

        val simuleringsmåned = SimuleringOppsummerer.lagMåneder(simuleringsposteringer, SIMULERING_HENTET_DATO, emptySet()).single()

        assertThat(simuleringsmåned.tom).isEqualTo(LocalDate.of(2026, 1, 31))
        assertThat(simuleringsmåned.posteringer).containsExactly(simuleringsposteringer.first())
    }

    @Test
    fun `fagområdekoder vi ikke kjenner, blir med i oppsummeringen sortert og uten duplikater`() {
        val simuleringsoppsummering =
            oppsummer(
                listOf(
                    postering(beløp = 752),
                    postering(beløp = 10, fagområdekode = FagOmrådeKode.GRUNNSTØNAD_INFOTRYGD_MANUELT.name),
                    postering(beløp = 20, fagområdekode = "BARNETRYGD"),
                    postering(beløp = 30, fagområdekode = FagOmrådeKode.GRUNNSTØNAD_INFOTRYGD_MANUELT.name),
                ),
            )

        assertThat(simuleringsoppsummering.ukjenteFagområdekoder)
            .containsExactly("BARNETRYGD", FagOmrådeKode.GRUNNSTØNAD_INFOTRYGD_MANUELT.name)
    }

    @Test
    fun `fagområdekode som bare finnes på MOTP er ukjent, også i en måned der MOTP ikke gir beløp`() {
        val simuleringsoppsummering =
            oppsummer(
                listOf(
                    postering(måned = "2026-11", beløp = 752),
                    postering(måned = "2026-11", beløp = -200, posteringstype = "MOTP", fagområdekode = "BARNETRYGD"),
                    postering(måned = "2026-12", beløp = -100, posteringstype = "MOTP", fagområdekode = "KONTANTSTØTTE"),
                ),
            )

        assertThat(simuleringsoppsummering.ukjenteFagområdekoder)
            .containsExactly("BARNETRYGD", "KONTANTSTØTTE")
    }

    @Test
    fun `manuelle fagområdekoder er ikke ukjente`() {
        val simuleringsoppsummering =
            oppsummer(
                listOf(postering(beløp = 10, fagområdekode = MANUELL_FAGOMRÅDEKODE.name)),
                manuelleFagområdekoder = setOf(MANUELL_FAGOMRÅDEKODE),
            )

        assertThat(simuleringsoppsummering.ukjenteFagområdekoder).isEmpty()
    }

    @Test
    fun `hjelpestønad på IT29 er ikke en ukjent fagområdekode`() {
        val simuleringsoppsummering =
            oppsummer(listOf(postering(beløp = 752, fagområdekode = FagOmrådeKode.HJELPESTØNAD_INFOTRYGD.name)))

        assertThat(simuleringsoppsummering.ukjenteFagområdekoder).isEmpty()
        assertBeløp(
            simuleringsoppsummering.perioder
                .single()
                .beløpPerMåned.nyttBeløp,
            752,
        )
    }

    @Test
    fun `tom simulering gir ingen perioder og null i alle summer`() {
        val simuleringsoppsummering = oppsummer(emptyList())

        assertThat(simuleringsoppsummering.perioder).isEmpty()
        assertThat(simuleringsoppsummering.fom).isNull()
        assertThat(simuleringsoppsummering.nesteUtbetaling).isNull()
        assertBeløp(simuleringsoppsummering.etterbetaling, 0)
        assertBeløp(simuleringsoppsummering.feilutbetaling, 0)
    }

    @Test
    fun `postering uten beløp gir UgyldigInput uten personopplysninger i meldingen`() {
        val simuleringUtenBeløp =
            DetaljertSimuleringResultat(
                simuleringMottaker =
                    listOf(
                        SimuleringMottaker(
                            simulertPostering = listOf(simulertPostering(postering(beløp = 1)).copy(beløp = null)),
                            mottakerNummer = "12345678910",
                            mottakerType = "BRUKER",
                        ),
                    ),
            )

        assertThatThrownBy { SimuleringOppsummerer.oppsummer(simuleringUtenBeløp, SIMULERING_HENTET_DATO) }
            .isInstanceOf(UgyldigInput::class.java)
            .hasMessage("En postering i simuleringen fra Oppdrag mangler beløp.")
    }

    @Test
    fun `ukjent posteringType gir UgyldigInput`() {
        val simuleringMedUkjentPosteringType =
            DetaljertSimuleringResultat(
                simuleringMottaker =
                    listOf(
                        SimuleringMottaker(
                            simulertPostering = listOf(simulertPostering(postering(beløp = 1)).copy(posteringType = "NY_TYPE")),
                            mottakerNummer = "12345678910",
                            mottakerType = "BRUKER",
                        ),
                    ),
            )

        assertThatThrownBy { SimuleringOppsummerer.oppsummer(simuleringMedUkjentPosteringType, SIMULERING_HENTET_DATO) }
            .isInstanceOf(UgyldigInput::class.java)
            .hasMessage("En postering i simuleringen fra Oppdrag har ukjent posteringType: NY_TYPE.")
    }

    @Test
    fun `ukjent betalingType gir UgyldigInput`() {
        val simuleringMedUkjentBetalingType =
            DetaljertSimuleringResultat(
                simuleringMottaker =
                    listOf(
                        SimuleringMottaker(
                            simulertPostering = listOf(simulertPostering(postering(beløp = 1)).copy(betalingType = "NY_TYPE")),
                            mottakerNummer = "12345678910",
                            mottakerType = "BRUKER",
                        ),
                    ),
            )

        assertThatThrownBy { SimuleringOppsummerer.oppsummer(simuleringMedUkjentBetalingType, SIMULERING_HENTET_DATO) }
            .isInstanceOf(UgyldigInput::class.java)
            .hasMessage("En postering i simuleringen fra Oppdrag har ukjent betalingType: NY_TYPE.")
    }

    @Test
    fun `ukjent fagOmrådeKode havner i ukjenteFagområdekoder`() {
        val simuleringsoppsummering = SimuleringOppsummerer.oppsummer(simuleringMedFagOmrådeKode("NY_YTELSE"), SIMULERING_HENTET_DATO)

        assertThat(simuleringsoppsummering.ukjenteFagområdekoder).containsExactly("NY_YTELSE")
    }

    @Test
    fun `manglende eller tom fagOmrådeKode gir UgyldigInput`() {
        listOf(null, "   ").forEach { fagOmrådeKode ->
            assertThatThrownBy { SimuleringOppsummerer.oppsummer(simuleringMedFagOmrådeKode(fagOmrådeKode), SIMULERING_HENTET_DATO) }
                .isInstanceOf(UgyldigInput::class.java)
                .hasMessage("En postering i simuleringen fra Oppdrag mangler fagOmrådeKode.")
        }
    }

    @Test
    fun `ukjent mottakerType gir UgyldigInput`() {
        val simuleringMedUkjentMottakerType =
            DetaljertSimuleringResultat(
                simuleringMottaker =
                    listOf(
                        SimuleringMottaker(
                            simulertPostering = listOf(simulertPostering(postering(beløp = 1))),
                            mottakerNummer = "12345678910",
                            mottakerType = "NY_TYPE",
                        ),
                    ),
            )

        assertThatThrownBy { SimuleringOppsummerer.oppsummer(simuleringMedUkjentMottakerType, SIMULERING_HENTET_DATO) }
            .isInstanceOf(UgyldigInput::class.java)
            .hasMessage("En postering i simuleringen fra Oppdrag har ukjent mottakerType: NY_TYPE.")
    }

    private data class TestPostering(
        val måned: YearMonth,
        val beløp: Int,
        val posteringstype: String,
        val fagområdekode: String,
        val forfallsdato: LocalDate,
        val betalingstype: String,
    )

    private fun postering(
        måned: String = "2026-01",
        beløp: Int,
        posteringstype: String = "YTELSE",
        fagområdekode: String = FagOmrådeKode.GRUNNSTØNAD_INFOTRYGD.name,
        forfallsdato: String? = null,
        betalingstype: String = if (beløp >= 0) "DEBIT" else "KREDIT",
    ): TestPostering {
        val yearMonth = YearMonth.parse(måned)
        return TestPostering(
            måned = yearMonth,
            beløp = beløp,
            posteringstype = posteringstype,
            fagområdekode = fagområdekode,
            forfallsdato = forfallsdato?.let(LocalDate::parse) ?: yearMonth.atDay(20),
            betalingstype = betalingstype,
        )
    }

    /** Som `mockVedtakSimuleringPosteringer` i ba-sak: én postering per måned, med forfall siste dag i måneden. */
    private fun månedlige(
        fraOgMedMåned: String,
        antallMåneder: Int,
        beløp: Int,
        posteringstype: String = "YTELSE",
    ): List<TestPostering> =
        (0 until antallMåneder).map { indeks ->
            val måned = YearMonth.parse(fraOgMedMåned).plusMonths(indeks.toLong())
            postering(måned = måned.toString(), beløp = beløp, posteringstype = posteringstype, forfallsdato = måned.atEndOfMonth().toString())
        }

    private fun simulertPostering(testPostering: TestPostering) =
        SimulertPostering(
            fagOmrådeKode = testPostering.fagområdekode,
            erFeilkonto = false,
            fom = testPostering.måned.atDay(1),
            tom = testPostering.måned.atEndOfMonth(),
            betalingType = testPostering.betalingstype,
            beløp = BigDecimal.valueOf(testPostering.beløp.toLong()),
            posteringType = testPostering.posteringstype,
            forfallsdato = testPostering.forfallsdato,
            utenInntrekk = false,
            klassekode = "GHGSORMG",
        )

    private fun oppsummer(
        testPosteringer: List<TestPostering>,
        simuleringHentetDato: LocalDate = SIMULERING_HENTET_DATO,
        manuelleFagområdekoder: Set<FagOmrådeKode> = emptySet(),
    ): Simuleringsoppsummering =
        SimuleringOppsummerer.oppsummer(
            DetaljertSimuleringResultat(
                simuleringMottaker =
                    listOf(
                        SimuleringMottaker(
                            simulertPostering = testPosteringer.map(::simulertPostering),
                            mottakerNummer = "12345678910",
                            mottakerType = "BRUKER",
                        ),
                    ),
            ),
            simuleringHentetDato,
            manuelleFagområdekoder,
        )

    private fun simuleringMedFagOmrådeKode(fagOmrådeKode: String?) =
        DetaljertSimuleringResultat(
            simuleringMottaker =
                listOf(
                    SimuleringMottaker(
                        simulertPostering = listOf(simulertPostering(postering(beløp = 1)).copy(fagOmrådeKode = fagOmrådeKode)),
                        mottakerNummer = "12345678910",
                        mottakerType = "BRUKER",
                    ),
                ),
        )

    private fun simuleringspostering(
        fom: LocalDate,
        tom: LocalDate,
    ) = Simuleringspostering(
        fagområdekode = FagOmrådeKode.GRUNNSTØNAD_INFOTRYGD.name,
        mottakertype = Mottakertype.BRUKER,
        fom = fom,
        tom = tom,
        forfallsdato = fom.plusDays(19),
        posteringstype = Posteringstype.YTELSE,
        betalingstype = Betalingstype.DEBIT,
        klassekode = "GHGSORMG",
        beløp = BigDecimal.valueOf(752),
        erFeilkonto = false,
        utenInntrekk = false,
    )

    private fun enMåned(
        vararg testPosteringer: TestPostering,
        manuelleFagområdekoder: Set<FagOmrådeKode> = emptySet(),
    ): Simuleringsmåned {
        val simuleringsposteringer =
            testPosteringer.map { testPostering ->
                Simuleringspostering(
                    fagområdekode = testPostering.fagområdekode,
                    mottakertype = Mottakertype.BRUKER,
                    fom = testPostering.måned.atDay(1),
                    tom = testPostering.måned.atEndOfMonth(),
                    forfallsdato = testPostering.forfallsdato,
                    posteringstype = Posteringstype.valueOf(testPostering.posteringstype),
                    betalingstype = Betalingstype.valueOf(testPostering.betalingstype),
                    klassekode = "GHGSORMG",
                    beløp = BigDecimal.valueOf(testPostering.beløp.toLong()),
                    erFeilkonto = false,
                    utenInntrekk = false,
                )
            }
        return SimuleringOppsummerer.lagMåneder(simuleringsposteringer, SIMULERING_HENTET_DATO, manuelleFagområdekoder).single()
    }

    private fun assertBeløp(
        faktiskBeløp: BigDecimal,
        forventetBeløp: Int,
    ) = assertThat(faktiskBeløp).isEqualByComparingTo(BigDecimal.valueOf(forventetBeløp.toLong()))

    private fun assertSimuleringsbeløp(
        simuleringsbeløp: Simuleringsbeløp,
        nyttBeløp: Int,
        tidligereUtbetalt: Int,
        resultat: Int,
        feilutbetaling: Int = 0,
        etterbetaling: Int,
        manuellPostering: Int = 0,
    ) {
        assertBeløp(simuleringsbeløp.nyttBeløp, nyttBeløp)
        assertBeløp(simuleringsbeløp.tidligereUtbetalt, tidligereUtbetalt)
        assertBeløp(simuleringsbeløp.resultat, resultat)
        assertBeløp(simuleringsbeløp.feilutbetaling, feilutbetaling)
        assertBeløp(simuleringsbeløp.etterbetaling, etterbetaling)
        assertBeløp(simuleringsbeløp.manuellPostering, manuellPostering)
    }
}
