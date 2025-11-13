package com.example.starter.helpers

import io.vertx.core.Vertx
import io.vertx.core.http.HttpHeaders
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField

@Suppress("NOTHING_TO_INLINE")
object PeriodicResolver {
    private val FORMATTER = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC)

    @Volatile
    var current: CharSequence = next()
        private set

    fun init(vertx: Vertx) = vertx.setPeriodic(1_000L) {
        current = next()
    }

    private inline fun next(): CharSequence {
        val now = Instant.now()
        val truncated = now.with(ChronoField.NANO_OF_SECOND, 0)
        return HttpHeaders.createOptimized(FORMATTER.format(truncated))
    }
}
