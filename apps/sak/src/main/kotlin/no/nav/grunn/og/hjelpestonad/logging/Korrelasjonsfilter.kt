package no.nav.grunn.og.hjelpestonad.logging

import efterlatte.prosessering.Sporing
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class Korrelasjonsfilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val opprinneligCallId = MDC.get("callId")
        val korrelasjonsId =
            Sporing.gjeldendeTraceId()?.takeIf { it.isNotBlank() }
                ?: opprinneligCallId?.takeIf { it.isNotBlank() }
                ?: UUID.randomUUID().toString()

        // Biblioteket leser trace_id, mens auditloggeren og klientene bruker callId.
        Sporing.kjorMedTraceId(korrelasjonsId) {
            MDC.put("callId", opprinneligCallId?.takeIf { it.isNotBlank() } ?: korrelasjonsId)
            try {
                filterChain.doFilter(request, response)
            } finally {
                if (opprinneligCallId == null) {
                    MDC.remove("callId")
                } else {
                    MDC.put("callId", opprinneligCallId)
                }
            }
        }
    }
}
