package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnose
import no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter.Ekstrautgift
import no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter.EkstrautgifterRegler
import no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter.Hjemmelbokstav
import no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter.Sats
import no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter.VilkårEkstrautgifter
import no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter.VilkårEkstrautgifterRequest
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

private const val MS = "G35"
private const val CØLIAKI = "K900"

class EkstrautgifterReglerTest {
    private val behandlingId = UUID.randomUUID()

    /**
     * Requesten som valideres. Hver bokstav blir én ekstrautgift med alle [diagnoser]. Bruk [ekstrautgifter] for egne
     * diagnoser per bokstav. Standardverdiene er de samme som for [ekstrautgifter], som bygger lagret periode.
     */
    private fun request(
        vurdering: Vurdering = Vurdering.JA,
        sats: Sats? = Sats.SATS_2,
        bokstaver: Set<Hjemmelbokstav> = setOf(Hjemmelbokstav.B),
        diagnoser: Set<String> = setOf(MS),
        fraOgMedDato: LocalDate? = LocalDate.of(2025, 11, 1),
        tilOgMedDato: LocalDate? = null,
        ekstrautgifter: List<Ekstrautgift> = if (diagnoser.isEmpty()) emptyList() else bokstaver.map { Ekstrautgift(it, diagnoser) },
    ) = VilkårEkstrautgifterRequest(
        vurdering = vurdering,
        sats = sats,
        ekstrautgifter = ekstrautgifter,
        fraOgMedDato = fraOgMedDato,
        tilOgMedDato = tilOgMedDato,
    )

    private fun requestMed(vararg ekstrautgifter: Pair<Hjemmelbokstav, Set<String>>) = request(ekstrautgifter = ekstrautgifter.map { (bokstav, koder) -> Ekstrautgift(bokstav, koder) })

    /** Lagret periode slik den ligger i databasen. Brukes som `lagret`, med samme standardverdier som [request]. */
    private fun ekstrautgifter(
        vurdering: Vurdering = Vurdering.JA,
        sats: Sats? = Sats.SATS_2,
        bokstaver: Set<Hjemmelbokstav> = setOf(Hjemmelbokstav.B),
        diagnoser: Set<String> = setOf(MS),
        fraOgMedDato: LocalDate? = LocalDate.of(2025, 11, 1),
        tilOgMedDato: LocalDate? = null,
        ekstrautgifter: List<Ekstrautgift> = if (diagnoser.isEmpty()) emptyList() else bokstaver.map { Ekstrautgift(it, diagnoser) },
    ) = VilkårEkstrautgifter(
        behandlingId = behandlingId,
        vurdering = vurdering,
        sats = sats,
        fraOgMedDato = fraOgMedDato,
        tilOgMedDato = tilOgMedDato,
    ).medEkstrautgifter(ekstrautgifter)

    private fun diagnose(
        kode: String = MS,
        fraOgMedDato: LocalDate? = null,
        tilOgMedDato: LocalDate? = null,
        vurdering: Vurdering = Vurdering.JA,
    ) = VilkårDiagnose(
        behandlingId = behandlingId,
        kode = kode,
        tekst = "Tekst",
        vurdering = vurdering,
        fraOgMedDato = fraOgMedDato,
        tilOgMedDato = tilOgMedDato,
    )

    private val msHeleTiden = listOf(diagnose(MS))

    private fun valider(
        request: VilkårEkstrautgifterRequest,
        lagret: VilkårEkstrautgifter? = null,
        diagnoser: List<VilkårDiagnose> = msHeleTiden,
    ) = EkstrautgifterRegler.valider(request, lagret, diagnoser)

    @Test
    fun `godtar oppfylt periode med sats, hjemmelbokstav og diagnose som er oppfylt i hele perioden`() {
        assertThatCode { valider(request()) }.doesNotThrowAnyException()
    }

    @Test
    fun `avviser oppfylt periode uten sats`() {
        assertThatThrownBy { valider(request(sats = null)) }
            .isInstanceOf(UgyldigInput::class.java)
            .hasMessage(EkstrautgifterRegler.SATS_MANGLER)
    }

    @Test
    fun `avviser oppfylt periode uten ekstrautgifter`() {
        assertThatThrownBy { valider(request(bokstaver = emptySet())) }
            .isInstanceOf(UgyldigInput::class.java)
            .hasMessage(EkstrautgifterRegler.EKSTRAUTGIFT_MANGLER)
    }

    @Test
    fun `avviser en ekstrautgift uten diagnose i requesten`() {
        assertThatThrownBy {
            valider(requestMed(Hjemmelbokstav.B to setOf(MS), Hjemmelbokstav.F to emptySet()))
        }.isInstanceOf(UgyldigInput::class.java)
            .hasMessage(EkstrautgifterRegler.DIAGNOSE_MANGLER)
    }

    @Test
    fun `avviser to ekstrautgifter med samme hjemmelbokstav i requesten`() {
        assertThatThrownBy {
            valider(requestMed(Hjemmelbokstav.B to setOf(MS), Hjemmelbokstav.B to setOf(CØLIAKI)))
        }.isInstanceOf(UgyldigInput::class.java)
            .hasMessage(EkstrautgifterRegler.HJEMMELBOKSTAV_FLERE_GANGER)
    }

    @Test
    fun `godtar request der hver ekstrautgift har sine egne diagnoser`() {
        assertThatCode {
            valider(
                requestMed(Hjemmelbokstav.B to setOf(MS), Hjemmelbokstav.F to setOf(CØLIAKI, MS)),
                diagnoser = listOf(diagnose(MS), diagnose(CØLIAKI)),
            )
        }.doesNotThrowAnyException()
    }

    @Test
    fun `godtar ekstrautgifter med hver sin diagnose når begge diagnosene er oppfylt i hele perioden`() {
        val ekstrautgifter = listOf(Ekstrautgift(Hjemmelbokstav.B, setOf(MS)), Ekstrautgift(Hjemmelbokstav.F, setOf(CØLIAKI)))
        val diagnoser = listOf(diagnose(MS), diagnose(CØLIAKI))

        assertThatCode { valider(request(ekstrautgifter = ekstrautgifter), diagnoser = diagnoser) }.doesNotThrowAnyException()
    }

    @Test
    fun `avviser periode som ikke er oppfylt, men har sats`() {
        assertThatThrownBy { valider(request(vurdering = Vurdering.NEI, sats = Sats.SATS_1)) }
            .isInstanceOf(UgyldigInput::class.java)
            .hasMessage(EkstrautgifterRegler.SATS_UTEN_RETT)
    }

    @Test
    fun `godtar periode som ikke er oppfylt, uten sats og ekstrautgifter`() {
        assertThatCode {
            valider(request(vurdering = Vurdering.NEI, sats = null, bokstaver = emptySet(), diagnoser = emptySet()))
        }.doesNotThrowAnyException()
    }

    @Test
    fun `godtar periode som ikke er oppfylt, uten fra og med-dato`() {
        assertThatCode {
            valider(
                request(
                    vurdering = Vurdering.NEI,
                    sats = null,
                    bokstaver = emptySet(),
                    diagnoser = emptySet(),
                    fraOgMedDato = null,
                ),
            )
        }.doesNotThrowAnyException()
    }

    @Test
    fun `avviser oppfylt periode uten fra og med-dato`() {
        assertThatThrownBy { valider(request(fraOgMedDato = null)) }
            .isInstanceOf(UgyldigInput::class.java)
            .hasMessage(EkstrautgifterRegler.FRA_OG_MED_MANGLER)
    }

    @Test
    fun `avviser bokstav d og e i en ny periode`() {
        listOf(Hjemmelbokstav.D, Hjemmelbokstav.E).forEach { bokstav ->
            assertThatThrownBy { valider(request(bokstaver = setOf(Hjemmelbokstav.B, bokstav))) }
                .isInstanceOf(UgyldigInput::class.java)
                .hasMessage(EkstrautgifterRegler.BOKSTAV_GJELDER_IKKE_NYE_TILFELLER)
        }
    }

    @Test
    fun `godtar bokstav d når den lagrede perioden allerede har den`() {
        val lagret = ekstrautgifter(bokstaver = setOf(Hjemmelbokstav.D))

        assertThatCode {
            valider(request(sats = Sats.SATS_3, bokstaver = setOf(Hjemmelbokstav.D)), lagret = lagret)
        }.doesNotThrowAnyException()
    }

    @Test
    fun `avviser bokstav e når den lagrede perioden bare har d`() {
        val lagret = ekstrautgifter(bokstaver = setOf(Hjemmelbokstav.D))
        val endret = request(bokstaver = setOf(Hjemmelbokstav.D, Hjemmelbokstav.E)).copy(id = lagret.id)

        assertThatThrownBy { valider(endret, lagret = lagret) }
            .isInstanceOf(UgyldigInput::class.java)
            .hasMessage(EkstrautgifterRegler.BOKSTAV_GJELDER_IKKE_NYE_TILFELLER)
    }

    @Test
    fun `avviser diagnose som ikke er vurdert, uten å røpe koden`() {
        assertThatThrownBy { valider(request(diagnoser = setOf(CØLIAKI))) }
            .isInstanceOf(UgyldigInput::class.java)
            .hasMessage(EkstrautgifterRegler.DIAGNOSE_IKKE_OPPFYLT_I_HELE_PERIODEN)
            .hasMessageNotContaining(CØLIAKI)
    }

    @Test
    fun `avviser diagnose som er vurdert til nei`() {
        assertThatThrownBy { valider(request(), diagnoser = listOf(diagnose(MS, vurdering = Vurdering.NEI))) }
            .isInstanceOf(UgyldigInput::class.java)
            .hasMessage(EkstrautgifterRegler.DIAGNOSE_IKKE_OPPFYLT_I_HELE_PERIODEN)
    }

    @Test
    fun `avviser diagnose som er oppfylt i starten, men slutter før løpende ekstrautgifter`() {
        val diagnoser = listOf(diagnose(MS, tilOgMedDato = LocalDate.of(2026, 6, 30)))

        assertThatThrownBy { valider(request(tilOgMedDato = null), diagnoser = diagnoser) }
            .isInstanceOf(UgyldigInput::class.java)
            .hasMessage(EkstrautgifterRegler.DIAGNOSE_IKKE_OPPFYLT_I_HELE_PERIODEN)
    }

    @Test
    fun `godtar diagnose som slutter samme dag som ekstrautgiftene`() {
        val diagnoser = listOf(diagnose(MS, tilOgMedDato = LocalDate.of(2026, 6, 30)))

        assertThatCode {
            valider(request(tilOgMedDato = LocalDate.of(2026, 6, 30)), diagnoser = diagnoser)
        }.doesNotThrowAnyException()
    }

    @Test
    fun `avviser diagnose som blir oppfylt etter at ekstrautgiftene starter`() {
        val diagnoser = listOf(diagnose(MS, fraOgMedDato = LocalDate.of(2025, 11, 2)))

        assertThatThrownBy { valider(request(fraOgMedDato = LocalDate.of(2025, 11, 1)), diagnoser = diagnoser) }
            .isInstanceOf(UgyldigInput::class.java)
            .hasMessage(EkstrautgifterRegler.DIAGNOSE_IKKE_OPPFYLT_I_HELE_PERIODEN)
    }

    @Test
    fun `godtar diagnose som er oppfylt i to perioder som følger rett etter hverandre`() {
        val diagnoser =
            listOf(
                diagnose(MS, fraOgMedDato = LocalDate.of(2026, 1, 1)),
                diagnose(MS, fraOgMedDato = LocalDate.of(2025, 1, 1), tilOgMedDato = LocalDate.of(2025, 12, 31)),
            )

        assertThatCode { valider(request(), diagnoser = diagnoser) }.doesNotThrowAnyException()
    }

    @Test
    fun `avviser diagnose med hull mellom to oppfylte perioder`() {
        val diagnoser =
            listOf(
                diagnose(MS, fraOgMedDato = LocalDate.of(2025, 1, 1), tilOgMedDato = LocalDate.of(2025, 12, 31)),
                diagnose(MS, fraOgMedDato = LocalDate.of(2026, 1, 1), tilOgMedDato = LocalDate.of(2026, 1, 31), vurdering = Vurdering.NEI),
                diagnose(MS, fraOgMedDato = LocalDate.of(2026, 2, 1)),
            )

        assertThatThrownBy { valider(request(), diagnoser = diagnoser) }
            .isInstanceOf(UgyldigInput::class.java)
            .hasMessage(EkstrautgifterRegler.DIAGNOSE_IKKE_OPPFYLT_I_HELE_PERIODEN)
    }

    @Test
    fun `avviser når én av flere diagnoser ikke er oppfylt i hele perioden`() {
        val diagnoser = listOf(diagnose(MS), diagnose(CØLIAKI, tilOgMedDato = LocalDate.of(2025, 12, 31)))

        assertThatThrownBy { valider(request(diagnoser = setOf(MS, CØLIAKI)), diagnoser = diagnoser) }
            .isInstanceOf(UgyldigInput::class.java)
            .hasMessage(EkstrautgifterRegler.DIAGNOSE_IKKE_OPPFYLT_I_HELE_PERIODEN)
    }

    @Test
    fun `krever at valgte diagnoser er oppfylt også når vilkåret ikke er oppfylt`() {
        assertThatThrownBy {
            valider(request(vurdering = Vurdering.NEI, sats = null, diagnoser = setOf(CØLIAKI)))
        }.isInstanceOf(UgyldigInput::class.java)
            .hasMessage(EkstrautgifterRegler.DIAGNOSE_IKKE_OPPFYLT_I_HELE_PERIODEN)
    }

    @Test
    fun `godtar periode som ikke er oppfylt uten fra og med-dato når diagnosen er oppfylt uten startdato`() {
        val periode = request(vurdering = Vurdering.NEI, sats = null, fraOgMedDato = null, diagnoser = setOf(MS))

        assertThatCode { valider(periode, diagnoser = msHeleTiden) }.doesNotThrowAnyException()
    }

    @Test
    fun `avviser periode som ikke er oppfylt uten fra og med-dato når diagnosen har startdato`() {
        val periode = request(vurdering = Vurdering.NEI, sats = null, fraOgMedDato = null, diagnoser = setOf(MS))
        val diagnoser = listOf(diagnose(MS, fraOgMedDato = LocalDate.of(2025, 1, 1)))

        assertThatThrownBy { valider(periode, diagnoser = diagnoser) }
            .isInstanceOf(UgyldigInput::class.java)
            .hasMessage(EkstrautgifterRegler.DIAGNOSE_IKKE_OPPFYLT_I_HELE_PERIODEN)
    }
}
