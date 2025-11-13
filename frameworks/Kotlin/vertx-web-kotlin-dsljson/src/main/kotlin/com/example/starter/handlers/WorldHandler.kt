package com.example.starter.handlers

import com.example.starter.db.WorldRepository
import com.example.starter.utils.serialize
import io.vertx.core.http.HttpServerRequest
import org.apache.logging.log4j.kotlin.Logging

class WorldHandler(private val repository: WorldRepository) : AbstractHandler() {
    fun readRandomWorld(req: HttpServerRequest) {
        repository
            .selectRandomWorld()
            .onSuccess {
                req.json().end(it.serialize())
            }
            .onFailure {
                logger.error(SOMETHING_WENT_WRONG, it)
                req.error()
            }
    }

    fun readRandomWorlds(req: HttpServerRequest) {
        repository
            .selectRandomWorlds(req.queries())
            .onSuccess {
                req.json().end(it.serialize())
            }
            .onFailure {
                logger.error(SOMETHING_WENT_WRONG, it)
                req.error()
            }
    }

    fun updateRandomWorlds(req: HttpServerRequest) {
        repository
            .updateRandomWorlds(req.queries())
            .onSuccess {
                req.json().end(it.serialize())
            }
            .onFailure {
                logger.error(SOMETHING_WENT_WRONG, it)
                req.error()
            }
    }

    private companion object : Logging {
        private const val QUERIES_PARAM_NAME = "queries"

        @Suppress("NOTHING_TO_INLINE")
        private inline fun HttpServerRequest.queries(): Int = getParam(QUERIES_PARAM_NAME)
            ?.toIntOrNull()
            ?.coerceIn(1, 500) ?: 1
    }
}