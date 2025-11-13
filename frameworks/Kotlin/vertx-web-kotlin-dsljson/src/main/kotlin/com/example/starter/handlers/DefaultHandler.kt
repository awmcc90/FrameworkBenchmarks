package com.example.starter.handlers

import io.vertx.core.Future
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.internal.buffer.BufferInternal

class DefaultHandler : AbstractHandler() {
    fun plaintext(req: HttpServerRequest): Future<Void> = req
        .plaintext()
        .end(MESSAGE_BUFFER)

    private companion object {
        private val MESSAGE_BUFFER = BufferInternal.buffer("Hello, World!", "UTF-8")
    }
}
