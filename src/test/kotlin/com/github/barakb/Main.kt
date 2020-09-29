package com.github.barakb

import com.github.barakb.http.HttpClient
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking

// https://proandroiddev.com/writing-dsls-in-kotlin-part-2-cd9dcd0c4715
// https://kotlinlang.org/docs/reference/type-safe-builders.html


fun main(): Unit = runBlocking {
    HttpClient {
        defaultRequest {
            url = "http://httpbin.org/"
            header("name", "value")
            param("v", "f")
        }
    }.use { client ->
        val headers = client.get<JsonObject> {
            path = "headers"
        }
        println("headers: $headers")
    }
}