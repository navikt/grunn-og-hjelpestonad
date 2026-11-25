package no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import io.mockk.verifyOrder
import io.swagger.v3.core.converter.ModelConverters
import no.nav.grunn.og.hjelpestonad.ApplicationLocalSetup
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingSteg
import no.nav.grunn.og.hjelpestonad.oppdrag.OppdragSimuleringService
import no.nav.grunn.og.hjelpestonad.tilgangskontroll.ManglerTilgangTilPersonFeil
import no.nav.grunn.og.hjelpestonad.tilgangskontroll.TilgangService
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

@WebMvcTest(InngangsvilkårStegController::class)
@ContextConfiguration(classes = [ApplicationLocalSetup::class])
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("integrasjonstest")
class InngangsvilkårStegControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean(relaxed = true)
    private lateinit var inngangsvilkårStegService: InngangsvilkårStegService

    @MockkBean(relaxed = true)
    private lateinit var oppdragSimuleringService: OppdragSimuleringService

    @MockkBean(relaxed = true)
    private lateinit var tilgangService: TilgangService

    private val behandlingId = UUID.randomUUID()
    private val url = "/api/behandling/$behandlingId/inngangsvilkar/fullfor"

    @Test
    fun `OpenAPI angir nøyaktig tillatte feilkoder for InngangsvilkårBlokkertResponse`() {
        val responsskjema = ModelConverters.getInstance().read(InngangsvilkårBlokkertResponse::class.java)
        val feilkodeskjema = responsskjema.getValue("InngangsvilkårBlokkertResponse").properties.getValue("feilkode")

        assertThat(feilkodeskjema.enum.map { it.toString() }).containsExactly("FALSK_IDENTITET")
    }

    @Test
    fun `falsk identitet returnerer HTTP 422 med egen feilkode og stopper simulering`() {
        val feilmelding = "Personen er registrert med falsk identitet i Folkeregisteret."
        every { inngangsvilkårStegService.fullførSteg(behandlingId) } throws
            InngangsvilkårBlokkertFeil(
                behandlingId = behandlingId,
                feilkode = InngangsvilkårFeilkode.FALSK_IDENTITET,
                melding = feilmelding,
            )

        mockMvc.post(url).andExpect {
            status { isEqualTo(422) }
            jsonPath("$.feilkode") { value("FALSK_IDENTITET") }
            jsonPath("$.melding") { value(feilmelding) }
            jsonPath("$.status") { value(422) }
        }

        verify(exactly = 0) { oppdragSimuleringService.simulerEtterNyTilkjentYtelse(any()) }
    }

    @Test
    fun `manglende persontilgang returnerer 403 før inngangsvilkår-steget sjekkes`() {
        every { tilgangService.validerTilgangTilBehandling(behandlingId) } throws ManglerTilgangTilPersonFeil(avvisningskode = null)

        mockMvc.post(url).andExpect { status { isForbidden() } }

        verify(exactly = 0) { inngangsvilkårStegService.fullførSteg(any()) }
    }

    @Test
    fun `fullfører etter tilgangskontroll og returnerer neste steg`() {
        every { inngangsvilkårStegService.fullførSteg(behandlingId) } returns
            InngangsvilkårResultat(
                perioder = emptyList(),
                nesteSteg = BehandlingSteg.SATS_OG_TRYGDETID,
            )

        mockMvc.post(url).andExpect {
            status { isOk() }
            jsonPath("$.nesteSteg") { value("SATS_OG_TRYGDETID") }
        }

        verifyOrder {
            tilgangService.validerTilgangTilBehandling(behandlingId)
            inngangsvilkårStegService.fullførSteg(behandlingId)
        }
    }
}
