package no.nav.grunn.og.hjelpestonad.infotrygd.util

import org.springframework.jdbc.core.simple.JdbcClient

/**
 * Ikke-nullbar variant av [JdbcClient.StatementSpec.query] med klasse. Spring 7 returnerer `MappedQuerySpec<T?>`,
 * så uten denne blir elementene i `list()` og `set()` nullbare i Kotlin.
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T : Any> JdbcClient.StatementSpec.queryAs(): JdbcClient.MappedQuerySpec<T> = query(T::class.java) as JdbcClient.MappedQuerySpec<T>
