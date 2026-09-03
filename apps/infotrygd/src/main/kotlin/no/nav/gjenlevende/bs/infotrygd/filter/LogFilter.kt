package no.nav.gjenlevende.bs.infotrygd.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.Ordered.HIGHEST_PRECEDENCE
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.GenericFilterBean
import java.util.UUID

@Component
@Order(HIGHEST_PRECEDENCE)
class LogFilter
    @JvmOverloads
    constructor(
        @Value("\${spring.application.name}") private val applicationName: String = "gjenlevende-bs-infotrygd",
    ) : GenericFilterBean() {
        private val log = LoggerFactory.getLogger(javaClass)
        private val consumerIdHeader = "Nav-Consumer-Id"
        private val callIdHeader = "Nav-Call-Id"

        private val dontLog =
            setOf(
                "/internal/health/liveness",
                "/internal/health/readiness",
                "/internal/prometheus",
                "/api/ping",
            )

        override fun doFilter(
            request: ServletRequest,
            response: ServletResponse,
            chain: FilterChain,
        ) {
            putValues(HttpServletRequest::class.java.cast(request))
            val req = request as HttpServletRequest
            val res = response as HttpServletResponse
            val t0 = System.nanoTime()
            try {
                chain.doFilter(request, response)
            } finally {
                if (!dontLog.contains(req.requestURI)) {
                    val millis = (System.nanoTime() - t0) / 1_000_000
                    log.info("[${millis}ms]\t${res.status} ${req.method} ${req.requestURI}")
                }
                MDC.clear()
            }
        }

        private fun putValues(request: HttpServletRequest) {
            try {
                toMDC(consumerIdHeader, request.getHeader(consumerIdHeader) ?: applicationName)
                toMDC(callIdHeader, request.getHeader(callIdHeader) ?: UUID.randomUUID().toString())
            } catch (e: Exception) {
                log.warn("Noe gikk galt ved setting av MDC-verdier for request {}, MDC-verdier er inkomplette", request.requestURI, e)
            }
        }

        override fun toString(): String = "${javaClass.simpleName} [applicationName=$applicationName]"

        fun toMDC(
            key: String,
            value: String,
        ) {
            MDC.put(key, value)
        }
    }
