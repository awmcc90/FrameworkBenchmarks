package com.example.starter

import com.example.starter.helpers.PeriodicResolver
import com.example.starter.utils.block
import io.vertx.core.Vertx
import io.vertx.core.impl.cpu.CpuCoreSensor
import io.vertx.kotlin.core.deploymentOptionsOf
import io.vertx.kotlin.core.vertxOptionsOf
import kotlin.time.Duration.Companion.seconds
import org.apache.logging.log4j.kotlin.logger

private const val EVENT_LOOP_POOL_SIZE_PROPERTY = "vertx.eventLoopPoolSize"
private const val TFB_HAS_DB_PROPERTY = "tfb.hasDB"
private const val SERVER_NAME = "Vert.x-Web Benchmark"
private val LOGGER = logger("App")

fun main() {
    val eventLoopPoolSize = System.getProperty(EVENT_LOOP_POOL_SIZE_PROPERTY)?.toInt()
        ?: CpuCoreSensor.availableProcessors()

    val vertx = Vertx.vertx(
        vertxOptionsOf(
            eventLoopPoolSize = eventLoopPoolSize,
            preferNativeTransport = true,
            disableTCCL = true,
        )
    )

    if (!vertx.isNativeTransportEnabled) {
        throw IllegalStateException(
            "Native transport not enabled; missing required dependencies",
            vertx.unavailableNativeTransportCause(),
        )
    }

    vertx.exceptionHandler {
        LOGGER.error("Vertx unexpected exception", it)
    }

    Runtime.getRuntime().addShutdownHook(
        Thread {
            vertx.close().block(5.seconds)
        }
    )

    PeriodicResolver.init(vertx)

    val hasDb = System.getProperty(TFB_HAS_DB_PROPERTY).toBoolean()
    val supplier = {
        when {
            hasDb -> PostgresVerticle()
            else -> BasicVerticle()
        }
    }

    vertx
        .deployVerticle(
            supplier,
            deploymentOptionsOf(
                instances = if (hasDb) eventLoopPoolSize else 1,
            )
        )
        .onSuccess {
            LOGGER.info("$SERVER_NAME started.")
        }
        .onFailure {
            LOGGER.error("Something went wrong while deploying.", it)
        }
}