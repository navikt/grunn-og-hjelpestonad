package no.nav.grunn.og.hjelpestonad.steg.satsOgTrygdetid

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import io.mockk.verifyOrder
import io.swagger.v3.core.converter.ModelConverters
import no.nav.grunn.og.hjelpestonad.ApplicationLocalSetup
import no.nav.grunn.og.hjelpestonad.oppdrag.OppdragSimuleringService
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.Valideringsfeil
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.Valideringsnøkkel
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.VilkårValideringFeil
import no.nav.grunn.og.hjelpestonad.tilgangskontroll.ManglerTilgangTilPersonFeil
import no.nav.grunn.og.hjelpestonad.tilgangskontroll.TilgangService
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.TilkjentYtelseUtleder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.util.UUID

@WebMvcTest(SatsOgTrygdetidController::class)
@ContextConfiguration(classes = [ApplicationLocalSetup::class])
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("integrasjonstest")
class SatsOgTrygdetidControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean(relaxed = true)
    private lateinit var satsOgTrygdetidService: SatsOgTrygdetidService

    @MockkBean(relaxed = true)
    private lateinit var oppdragSimuleringService: OppdragSimuleringService

    @MockkBean(relaxed = true)
    private lateinit var tilgangService: TilgangService

    private val behandlingId = UUID.randomUUID()
    private val url = "/api/behandling/$behandlingId/sats-og-trygdetid/fullfor"

    @Test
    fun `OpenAPI angir nøyaktig de tillatte responsnøklene`() {
        val responsskjema = ModelConverters.getInstance().read(Valideringsfeil::class.java)
        val nøkkelskjema = responsskjema.getValue("Valideringsfeil").properties.getValue("nøkkel")

        assertThat(nøkkelskjema.enum.map { it.toString() }).containsExactly(
            "MEDLEM_I_TRYGDEN_ELLER_OMFATTET_AV_EØS_FORORDNINGEN",
            "DIAGNOSE",
            "NØDVENDIGE_EKSTRAUTGIFTER",
            "TRYGDETID",
        )
    }

    @Test
    fun `fullfører etter personsjekk og returnerer 204`() {
        mockMvc.post(url).andExpect { status { isNoContent() } }
        verifyOrder {
            tilgangService.validerTilgangTilBehandling(behandlingId)
            satsOgTrygdetidService.fullførSteg(behandlingId)
        }
    }

    @Test
    fun `simulerer etter at tilkjent ytelse er lagret`() {
        mockMvc.post(url).andExpect { status { isNoContent() } }
        verifyOrder {
            satsOgTrygdetidService.fullførSteg(behandlingId)
            oppdragSimuleringService.simulerEtterNyTilkjentYtelse(behandlingId)
        }
    }

    @Test
    fun `feil i fullførSteg gir ingen simulering`() {
        every { satsOgTrygdetidService.fullførSteg(behandlingId) } throws
            VilkårValideringFeil(
                listOf(Valideringsfeil(Valideringsnøkkel.TRYGDETID, TilkjentYtelseUtleder.TRYGDETID_MANGLER)),
            )
        mockMvc.post(url).andExpect { status { isBadRequest() } }
        verify(exactly = 0) { oppdragSimuleringService.simulerEtterNyTilkjentYtelse(any()) }
    }

    @Test
    fun `manglende persontilgang stopper før beregning`() {
        every { tilgangService.validerTilgangTilBehandling(behandlingId) } throws ManglerTilgangTilPersonFeil(avvisningskode = null)
        mockMvc.post(url).andExpect { status { isForbidden() } }
        verify(exactly = 0) { satsOgTrygdetidService.fullførSteg(any()) }
    }

    @Test
    fun `manglende trygdetid returneres som strukturert valideringsfeil`() {
        every { satsOgTrygdetidService.fullførSteg(behandlingId) } throws
            VilkårValideringFeil(listOf(Valideringsfeil(Valideringsnøkkel.TRYGDETID, TilkjentYtelseUtleder.TRYGDETID_MANGLER)))
        mockMvc.post(url).andExpect {
            status { isBadRequest() }
            jsonPath("$.feil.length()") { value(1) }
            jsonPath("$.feil[0].nøkkel") { value("TRYGDETID") }
            jsonPath("$.feil[0].melding") { value(TilkjentYtelseUtleder.TRYGDETID_MANGLER) }
        }
    }

    @Test
    fun `medlemskap og diagnose returneres som separate valideringsfeil`() {
        every { satsOgTrygdetidService.fullførSteg(behandlingId) } throws
            VilkårValideringFeil(
                listOf(
                    Valideringsfeil(Valideringsnøkkel.MEDLEM_I_TRYGDEN_ELLER_OMFATTET_AV_EØS_FORORDNINGEN, "Medlemskap mangler."),
                    Valideringsfeil(Valideringsnøkkel.DIAGNOSE, "Diagnose mangler."),
                ),
            )

        mockMvc.post(url).andExpect {
            status { isBadRequest() }
            jsonPath("$.feil.length()") { value(2) }
            jsonPath("$.feil[0].nøkkel") { value("MEDLEM_I_TRYGDEN_ELLER_OMFATTET_AV_EØS_FORORDNINGEN") }
            jsonPath("$.feil[0].melding") { value("Medlemskap mangler.") }
            jsonPath("$.feil[1].nøkkel") { value("DIAGNOSE") }
            jsonPath("$.feil[1].melding") { value("Diagnose mangler.") }
        }
    }

    @Test
    fun `manglende medlemskap og diagnose beholder hver sin valideringsnøkkel`() {
        every { satsOgTrygdetidService.fullførSteg(behandlingId) } throws
            VilkårValideringFeil(
                listOf(
                    Valideringsfeil(Valideringsnøkkel.MEDLEM_I_TRYGDEN_ELLER_OMFATTET_AV_EØS_FORORDNINGEN, "Du må vurdere medlemskap i folketrygden."),
                    Valideringsfeil(Valideringsnøkkel.DIAGNOSE, "Du må vurdere om det foreligger en varig sykdom, skade eller lyte."),
                ),
            )

        mockMvc.post(url).andExpect {
            status { isBadRequest() }
            jsonPath("$.feil.length()") { value(2) }
            jsonPath("$.feil[0].nøkkel") { value("MEDLEM_I_TRYGDEN_ELLER_OMFATTET_AV_EØS_FORORDNINGEN") }
            jsonPath("$.feil[0].melding") { value("Du må vurdere medlemskap i folketrygden.") }
            jsonPath("$.feil[1].nøkkel") { value("DIAGNOSE") }
            jsonPath("$.feil[1].melding") { value("Du må vurdere om det foreligger en varig sykdom, skade eller lyte.") }
            jsonPath("$.status") { value(400) }
            jsonPath("$.melding") { value("Valideringen er ikke komplett") }
        }
    }

    @Test
    fun `gjentatte feil samles per nøkkel og beholder alle meldingene`() {
        every { satsOgTrygdetidService.fullførSteg(behandlingId) } throws
            VilkårValideringFeil(
                listOf(
                    Valideringsfeil(Valideringsnøkkel.NØDVENDIGE_EKSTRAUTGIFTER, "Første melding."),
                    Valideringsfeil(Valideringsnøkkel.NØDVENDIGE_EKSTRAUTGIFTER, "Andre melding."),
                    Valideringsfeil(Valideringsnøkkel.TRYGDETID, TilkjentYtelseUtleder.TRYGDETID_MANGLER),
                ),
            )

        mockMvc.post(url).andExpect {
            status { isBadRequest() }
            jsonPath("$.feil.length()") { value(2) }
            jsonPath("$.feil[0].nøkkel") { value("NØDVENDIGE_EKSTRAUTGIFTER") }
            jsonPath("$.feil[0].melding") { value("Første melding. Andre melding.") }
            jsonPath("$.feil[1].nøkkel") { value("TRYGDETID") }
            jsonPath("$.feil[1].melding") { value(TilkjentYtelseUtleder.TRYGDETID_MANGLER) }
        }
    }
}
