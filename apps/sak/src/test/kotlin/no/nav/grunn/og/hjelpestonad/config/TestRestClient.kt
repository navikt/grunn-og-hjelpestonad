package no.nav.grunn.og.hjelpestonad.config

import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

fun testRestClientBuilder(
    connectTimeout: Duration = Duration.ofSeconds(5),
    readTimeout: Duration = Duration.ofSeconds(10),
): RestClient.Builder {
    val httpClient =
        HttpClient
            .newBuilder()
            .connectTimeout(connectTimeout)
            .version(HttpClient.Version.HTTP_1_1)
            .build()
    val requestFactory =
        JdkClientHttpRequestFactory(httpClient).apply {
            setReadTimeout(readTimeout)
        }

    return RestClient.builder().requestFactory(requestFactory)
}
