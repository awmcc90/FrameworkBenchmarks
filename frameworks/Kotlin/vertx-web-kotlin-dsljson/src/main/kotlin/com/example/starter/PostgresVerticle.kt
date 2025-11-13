package com.example.starter

import com.example.starter.db.FortuneRepository
import com.example.starter.db.WorldRepository
import com.example.starter.handlers.FortuneHandler
import com.example.starter.handlers.WorldHandler
import com.example.starter.helpers.JsonResource
import com.example.starter.utils.isConnectionReset
import io.vertx.core.http.HttpServerOptions
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgConnection
import java.util.function.Predicate
import org.apache.logging.log4j.kotlin.Logging

class PostgresVerticle : CoroutineVerticle() {
    override suspend fun start() {
        val conn = PgConnection.connect(vertx, PG_CONNECT_OPTIONS).await()

        val fortuneRepository = FortuneRepository.init(conn)
        val worldRepository = WorldRepository.init(conn)

        val fortuneHandler = FortuneHandler(fortuneRepository.coAwait())
        val worldHandler = WorldHandler(worldRepository.coAwait())

        val server = vertx
            .createHttpServer(HTTP_SERVER_OPTIONS)
            .requestHandler {
                when (it.path()) {
                    FORTUNES_PATH -> fortuneHandler.templateFortunes(it)
                    DB_PATH -> worldHandler.readRandomWorld(it)
                    QUERIES_PATH -> worldHandler.readRandomWorlds(it)
                    UPDATES_PATH -> worldHandler.updateRandomWorlds(it)
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
        private const val FORTUNES_PATH = "/fortunes"
        private const val DB_PATH = "/db"
        private const val QUERIES_PATH = "/queries"
        private const val UPDATES_PATH = "/updates"
        private const val HTTP_SERVER_OPTIONS_RESOURCE = "vertx/http-server-options.json"
        private const val PG_CONNECT_OPTIONS_RESOURCE = "vertx/pg-connect-options.json"

        private val HTTP_SERVER_OPTIONS: HttpServerOptions by lazy {
            val json = JsonResource.of(HTTP_SERVER_OPTIONS_RESOURCE)
            HttpServerOptions(json)
        }

        private val PG_CONNECT_OPTIONS: PgConnectOptions by lazy {
            val json = JsonResource.of(PG_CONNECT_OPTIONS_RESOURCE)
            PgConnectOptions(json).apply {
                host = System.getProperty("tfb.pgHostOverride") ?: host
                preparedStatementCacheSqlFilter = Predicate {
                    true
                }
            }
        }
    }
}
