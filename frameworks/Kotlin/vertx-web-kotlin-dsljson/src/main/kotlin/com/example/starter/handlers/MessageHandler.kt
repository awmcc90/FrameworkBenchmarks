package com.example.starter.handlers

import com.example.starter.models.Message
import com.example.starter.utils.serialize
import io.vertx.core.Future
import io.vertx.core.http.HttpServerRequest

class MessageHandler : AbstractHandler() {
    fun readDefaultMessage(req: HttpServerRequest): Future<Void> = req
        .json()
        .end(DEFAULT_MESSAGE.serialize())

    private companion object {
        private val DEFAULT_MESSAGE = Message("Hello, World!")
    }
}
