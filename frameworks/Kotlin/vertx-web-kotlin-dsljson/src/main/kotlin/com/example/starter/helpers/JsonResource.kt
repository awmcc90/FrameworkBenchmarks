package com.example.starter.helpers

import io.vertx.core.json.JsonObject

object JsonResource {
    fun of(resource: String): JsonObject {
        val classLoader = JsonResource::class.java.classLoader
        classLoader.getResourceAsStream(resource)?.use { input ->
            val output = BufferOutputStream()
            output.write(input.readAllBytes())
            return output.toJsonObject()
        }
        throw IllegalStateException("$resource not found")
    }
}
