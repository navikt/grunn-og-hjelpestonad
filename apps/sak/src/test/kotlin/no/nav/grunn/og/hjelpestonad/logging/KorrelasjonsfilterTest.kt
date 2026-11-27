package no.nav.grunn.og.hjelpestonad.logging

import efterlatte.prosessering.Sporing
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.util.UUID

class KorrelasjonsfilterTest {
    private val korrelasjonsfilter = Korrelasjonsfilter()
    private var opprinneligMdc: Map<String, String>? = null

    @BeforeEach
    fun klargjorMdc() {
        opprinneligMdc = MDC.getCopyOfContextMap()
        MDC.clear()
    }

    @AfterEach
    fun gjenopprettMdc() {
        val opprinneligKontekst = opprinneligMdc
        if (opprinneligKontekst == null) {
            MDC.clear()
        } else {
            MDC.setContextMap(opprinneligKontekst)
        }
    }

    @Test
    fun `oppretter samme korrelasjons-ID for task-produsenten og eksisterende loggklienter`() {
        filtrer {
            val traceId = requireNotNull(Sporing.gjeldendeTraceId())
            assertThat(UUID.fromString(traceId).toString()).isEqualTo(traceId)
            assertThat(MDC.get("callId")).isEqualTo(traceId)
        }

        assertThat(MDC.get("trace_id")).isNull()
        assertThat(MDC.get("callId")).isNull()
    }

    @Test
    fun `gjenbruker eksisterende trace-ID uten å overskrive annen kontekst`() {
        MDC.put("trace_id", "0123456789abcdef0123456789abcdef")
        MDC.put("annenKontekst", "beholdes")

        filtrer {
            assertThat(Sporing.gjeldendeTraceId()).isEqualTo("0123456789abcdef0123456789abcdef")
            assertThat(MDC.get("callId")).isEqualTo(Sporing.gjeldendeTraceId())
            assertThat(MDC.get("annenKontekst")).isEqualTo("beholdes")
        }

        assertThat(MDC.get("trace_id")).isEqualTo("0123456789abcdef0123456789abcdef")
        assertThat(MDC.get("callId")).isNull()
        assertThat(MDC.get("annenKontekst")).isEqualTo("beholdes")
    }

    @Test
    fun `gjenbruker callId når trace-ID mangler`() {
        MDC.put("callId", "eksisterende-call-id")

        filtrer {
            assertThat(Sporing.gjeldendeTraceId()).isEqualTo("eksisterende-call-id")
            assertThat(MDC.get("callId")).isEqualTo("eksisterende-call-id")
        }

        assertThat(MDC.get("trace_id")).isNull()
        assertThat(MDC.get("callId")).isEqualTo("eksisterende-call-id")
    }

    @Test
    fun `beholder både trace-ID og callId når begge finnes`() {
        MDC.put("trace_id", "eksisterende-trace-id")
        MDC.put("callId", "eksisterende-call-id")

        filtrer {
            assertThat(Sporing.gjeldendeTraceId()).isEqualTo("eksisterende-trace-id")
            assertThat(MDC.get("callId")).isEqualTo("eksisterende-call-id")
        }

        assertThat(MDC.get("trace_id")).isEqualTo("eksisterende-trace-id")
        assertThat(MDC.get("callId")).isEqualTo("eksisterende-call-id")
    }

    @Test
    fun `tomme MDC-verdier gir en ny korrelasjons-ID og gjenopprettes etterpå`() {
        MDC.put("trace_id", "")
        MDC.put("callId", " ")

        filtrer {
            assertThat(Sporing.gjeldendeTraceId()).isNotBlank()
            assertThat(MDC.get("callId")).isEqualTo(Sporing.gjeldendeTraceId())
        }

        assertThat(MDC.get("trace_id")).isEmpty()
        assertThat(MDC.get("callId")).isEqualTo(" ")
    }

    @Test
    fun `gjenoppretter konteksten og videreformidler feil fra filterkjeden`() {
        MDC.put("annenKontekst", "beholdes")
        val opprinneligException = IllegalStateException("Feil i filterkjeden")

        val kastetException =
            assertThrows<IllegalStateException> {
                filtrer { throw opprinneligException }
            }

        assertThat(kastetException).isSameAs(opprinneligException)
        assertThat(MDC.getCopyOfContextMap()).containsExactlyEntriesOf(mapOf("annenKontekst" to "beholdes"))
    }

    @Test
    fun `separate forespørsler får forskjellige korrelasjons-ID-er`() {
        val korrelasjonsIder = mutableListOf<String>()
        repeat(2) {
            filtrer { korrelasjonsIder.add(requireNotNull(Sporing.gjeldendeTraceId())) }
        }

        assertThat(korrelasjonsIder).hasSize(2).doesNotHaveDuplicates()
    }

    private fun filtrer(filterkjede: () -> Unit) {
        korrelasjonsfilter.doFilter(MockHttpServletRequest(), MockHttpServletResponse()) { _, _ ->
            filterkjede()
        }
    }
}
