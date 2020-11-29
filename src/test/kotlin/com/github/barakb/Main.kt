package com.github.barakb

import com.github.barakb.http.HttpClient
import com.github.barakb.http.HttpHeader
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import org.apache.hc.core5.http.ContentType
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

// https://proandroiddev.com/writing-dsls-in-kotlin-part-2-cd9dcd0c4715
// https://kotlinlang.org/docs/reference/type-safe-builders.html


@ExperimentalTime
fun main(): Unit = runBlocking {
    HttpClient {
        gson { setPrettyPrinting() }
        defaultRequest {
            contentType = ContentType.APPLICATION_JSON
            url = "http://httpbin.org/"
            header("name", "value")
            param("v", "f")
        }
    }.use { client ->
        val headers = client.get<Headers> {
            path = "headers"
            connectTimeout = 15.seconds
            responseTimeout = 30.seconds
        }
        println("headers: $headers")
    }
}

data class Headers(@HttpHeader("host") var host: String?, val headers: JsonObject)