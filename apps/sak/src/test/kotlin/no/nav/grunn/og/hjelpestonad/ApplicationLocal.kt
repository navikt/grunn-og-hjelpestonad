package no.nav.grunn.og.hjelpestonad

import org.springframework.boot.builder.SpringApplicationBuilder

class ApplicationLocalMock : ApplicationLocalSetup()

fun main(args: Array<String>) {
    SpringApplicationBuilder(ApplicationLocalMock::class.java)
        .profiles("local-mock")
        .run(*args)
}
