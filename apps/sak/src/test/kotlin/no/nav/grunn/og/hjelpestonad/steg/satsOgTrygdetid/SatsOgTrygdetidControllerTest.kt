package no.nav.grunn.og.hjelpestonad.steg.satsOgTrygdetid

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.grunn.og.hjelpestonad.ApplicationLocalSetup
import no.nav.grunn.og.hjelpestonad.oppdrag.OppdragSimuleringService
import no.nav.grunn.og.hjelpestonad.tilgangskontroll.ManglerTilgangTilPersonFeil
import no.nav.grunn.og.hjelpestonad.tilgangskontroll.TilgangService
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.TilkjentYtelseUtleder
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.UtledningAvTilkjentYtelseFeil
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.Utledningsfeil
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.Valideringsnøkkel
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
            UtledningAvTilkjentYtelseFeil(
                listOf(Utledningsfeil(Valideringsnøkkel.Trygdetid, TilkjentYtelseUtleder.TRYGDETID_MANGLER)),
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
            UtledningAvTilkjentYtelseFeil(
                listOf(Utledningsfeil(Valideringsnøkkel.Trygdetid, TilkjentYtelseUtleder.TRYGDETID_MANGLER)),
            )
        mockMvc.post(url).andExpect {
            status { isBadRequest() }
            jsonPath("$.feil[0].nøkkel") { value("trygdetid") }
            jsonPath("$.feil[0].melding") { value(TilkjentYtelseUtleder.TRYGDETID_MANGLER) }
        }
    }
}
