package com.example.starter

import com.example.starter.handlers.DefaultHandler
import com.example.starter.handlers.MessageHandler
import com.example.starter.helpers.JsonResource
import com.example.starter.utils.isConnectionReset
import io.vertx.core.http.HttpServerOptions
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import org.apache.logging.log4j.kotlin.Logging

class BasicVerticle : CoroutineVerticle() {
    override suspend fun start() {
        val defaultHandler = DefaultHandler()
        val messageHandler = MessageHandler()

        val server = vertx
            .createHttpServer(HTTP_SERVER_OPTIONS)
            .requestHandler {
                when (it.path()) {
                    PLAINTEXT_PATH -> defaultHandler.plaintext(it)
                    JSON_PATH -> messageHandler.readDefaultMessage(it)
                    else -> throw IllegalStateException("No handler for path: ${it.path()}")
                }
            }
            .exceptionHandler {
                if (!it.isConnectionReset()) {
                    logger.error("Exception in HttpServer", it)
                }
            }

        server.listen().coAwait()
        logger.info("HTTP server started on port 8080")
    }

    companion object : Logging {
        private const val PLAINTEXT_PATH = "/plaintext"
        private const val JSON_PATH = "/json"
        private const val HTTP_SERVER_OPTIONS_RESOURCE = "vertx/http-server-options.json"

        private val HTTP_SERVER_OPTIONS: HttpServerOptions by lazy {
            val json = JsonResource.of(HTTP_SERVER_OPTIONS_RESOURCE)
            HttpServerOptions(json)
        }
    }
}
