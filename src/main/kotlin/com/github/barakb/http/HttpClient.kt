package com.github.barakb.http

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import mu.KotlinLogging
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest
import org.apache.hc.client5.http.async.methods.SimpleHttpRequests
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder
import org.apache.hc.client5.http.impl.async.HttpAsyncClients
import org.apache.hc.client5.http.protocol.HttpClientContext
import org.apache.hc.client5.http.protocol.HttpClientContext.REQUEST_CONFIG
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.Header
import org.apache.hc.core5.http.HttpResponse
import org.apache.hc.core5.io.CloseMode
import java.io.Closeable
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.full.findAnnotation
import kotlin.time.Duration
import kotlin.time.ExperimentalTime


private val logger = KotlinLogging.logger {}

@ExperimentalTime
class HttpClient(configBuilder: HttpConfigBuilder) : Closeable {
    val config: Config = configBuilder.create()

    constructor(init: HttpConfigBuilder.() -> Unit) : this(HttpConfigBuilder().apply(init))


    val httpClient: CloseableHttpAsyncClient = config.builder.build().apply { start() }


    @Suppress("unused")
    suspend inline fun <reified T> get(noinline init: RequestBuilder.() -> Unit): T {
        return execute<T>(RequestType.GET)(init)
    }

    @Suppress("unused")
    suspend inline fun <reified T> put(noinline init: RequestBuilder.() -> Unit): T {
        return execute<T>(RequestType.PUT)(init)
    }

    @Suppress("unused")
    suspend inline fun <reified T> post(noinline init: RequestBuilder.() -> Unit): T {
        return execute<T>(RequestType.POST)(init)
    }

    @Suppress("unused")
    suspend inline fun <reified T> head(noinline init: RequestBuilder.() -> Unit): T {
        return execute<T>(RequestType.HEAD)(init)
    }

    @Suppress("unused")
    suspend inline fun <reified T> delete(noinline init: RequestBuilder.() -> Unit): T {
        return execute<T>(RequestType.DELETE)(init)
    }

    @Suppress("unused")
    suspend inline fun <reified T> options(noinline init: RequestBuilder.() -> Unit): T {
        return execute<T>(RequestType.OPTIONS)(init)
    }

    @Suppress("unused")
    suspend inline fun <reified T> trace(noinline init: RequestBuilder.() -> Unit): T {
        return execute<T>(RequestType.TRACE)(init)
    }

    override fun close() {
        httpClient.close(CloseMode.GRACEFUL)
    }

    inline fun <reified T> execute(requestType: RequestType): suspend (init: RequestBuilder.() -> Unit) -> T = { init ->
        val request = config.defaultRequest.copy().apply(init).build(requestType)
        val (ctx, httpRequest) = prepareHttpUriRequest(request)
        val response = httpClient.execute(ctx, httpRequest)
        val res = extractResponse<T>(request.type, request.url, response)
        when (T::class) {
            List::class -> (res as List<*>?)?.forEach { populateHeaders(it, response) }
            else -> populateHeaders(res, response)
        }
        res
    }

    fun populateHeaders(res: Any?, response: SimpleHttpResponse) {
        if (res != null) {
            res::class.declaredMembers.filterIsInstance<KMutableProperty<*>>()
                .forEach { prop ->
                    val httpHeader = prop.findAnnotation<HttpHeader>()
                    if (httpHeader != null) {
                        val found: Header? = response.getFirstHeader(httpHeader.name)
                        found?.let {
                            prop.setter.call(res, found.value)
                        }
                    }
                }
        }
    }


    inline fun <reified T> extractResponse(
        requestType: RequestType,
        requestUrl: String,
        response: SimpleHttpResponse
    ): T {
        val typeInfo = typeInfo<T>()
        val status = response.code
        if (status == 404) {
            if (typeInfo.kotlinType?.isMarkedNullable == true) {
                return null as T
            } else {
                throw HttpResponseException(status, response.reasonPhrase, requestType, requestUrl)
            }
        }
        return when (T::class) {
            HttpResponse::class -> response as T
            String::class -> response.bodyText as T
            else ->
                try {
                    config.gson.fromJson(response.bodyText, typeInfo.reifiedType)
                } catch (jse: JsonSyntaxException) {
                    if (typeInfo.kotlinType?.isMarkedNullable == true) {
                        null as T
                    } else {
                        throw ResponseSyntaxException(
                            jse.message,
                            response.bodyText,
                            typeInfo.reifiedType,
                            requestType,
                            requestUrl
                        )
                    }
                }
        }
    }

    fun prepareHttpUriRequest(request: Request): Pair<HttpClientContext, SimpleHttpRequest> {
        val (ctx, httpRequest) = httpRequest(request)
        httpRequest.uri = URI.create(request.url)
        request.body?.let {
            if (it is ByteArray) {
                logger.debug("${httpRequest.method} ${httpRequest.uri}: body = binary")
                httpRequest.setBody(it, ContentType.APPLICATION_OCTET_STREAM)
            } else {
                val ct = request.contentType ?: ContentType.APPLICATION_JSON
                val content = if (ct == ContentType.APPLICATION_JSON) config.gson.toJson(it) else "$it"
                logger.debug("${httpRequest.method} ${httpRequest.uri}: body = $content")
                httpRequest.setBody(content.toByteArray(), ct)
            }
        }
        request.headers.forEach {
            httpRequest.setHeader(it.first, it.second)
        }
        logger.debug("sending $httpRequest")
        return (ctx to httpRequest)
    }

    private fun httpRequest(request: Request): Pair<HttpClientContext, SimpleHttpRequest> {
        val requestConfigBuilder = RequestConfig.custom()
        request.connectTimeout?.let {
            requestConfigBuilder.setConnectTimeout(it.toLongMilliseconds(), TimeUnit.MILLISECONDS)
        }
        request.responseTimeout?.let {
            requestConfigBuilder.setResponseTimeout(it.toLongMilliseconds(), TimeUnit.MILLISECONDS)
        }
        val ctx: HttpClientContext = HttpClientContext.create()
        ctx.setAttribute(REQUEST_CONFIG, requestConfigBuilder.build())
        return (ctx to when (request.type) {
            RequestType.GET -> SimpleHttpRequests.get(request.url)
            RequestType.PUT -> SimpleHttpRequests.put(request.url)
            RequestType.POST -> SimpleHttpRequests.post(request.url)
            RequestType.DELETE -> SimpleHttpRequests.delete(request.url)
            RequestType.HEAD -> SimpleHttpRequests.head(request.url)
            RequestType.TRACE -> SimpleHttpRequests.trace(request.url)
            RequestType.OPTIONS -> SimpleHttpRequests.options(request.url)
        })
    }
}


@DslMarker
annotation class DslHttpClient
class Config @ExperimentalTime constructor(
    val builder: HttpAsyncClientBuilder,
    val defaultRequest: RequestBuilder = RequestBuilder(),
    val gson: Gson = Gson()
)

@ExperimentalTime
@DslHttpClient
class HttpConfigBuilder {
    private var defaultRequest: RequestBuilder = RequestBuilder()
    private var gsonBuilder = GsonBuilder()
    private var httpAsyncClientBuilder = HttpAsyncClients.custom()


    @Suppress("unused")
    fun defaultRequest(init: RequestBuilder.() -> Unit) {
        defaultRequest.apply(init)
    }

    @Suppress("unused")
    fun gson(init: GsonBuilder.() -> Unit) {
        gsonBuilder = gsonBuilder.apply(init)
    }

    @Suppress("unused")
    fun client(init: HttpAsyncClientBuilder.() -> Unit) {
        httpAsyncClientBuilder.apply(init)
    }

    internal fun create(): Config {
        return Config(httpAsyncClientBuilder, defaultRequest, gsonBuilder.create())
    }
}

enum class RequestType {
    GET,
    PUT,
    POST,
    DELETE,
    HEAD,
    TRACE,
    OPTIONS
}

data class Request @ExperimentalTime constructor(
    val type: RequestType,
    val url: String,
    var body: Any? = null,
    val headers: Set<Pair<String, String>>,
    val contentType: ContentType?,
    val connectTimeout: Duration?,
    val responseTimeout: Duration?
)

@ExperimentalTime
@Suppress("MemberVisibilityCanBePrivate")
@DslHttpClient
class RequestBuilder @ExperimentalTime constructor(
    private val headers: MutableSet<Pair<String, String>> = mutableSetOf(),
    private val queries: MutableSet<Pair<String, String>> = mutableSetOf(),
    var url: String? = null,
    var body: Any? = null,
    var path: String = "",
    var contentType: ContentType? = null,
    var connectTimeout: Duration? = null,
    var responseTimeout: Duration? = null
) {

    fun copy(): RequestBuilder {
        return RequestBuilder(
            headers.toMutableSet(),
            queries.toMutableSet(),
            url,
            body,
            path,
            contentType,
            connectTimeout,
            responseTimeout
        )
    }

    fun build(type: RequestType): Request {
        val combined = url?.let { combineUrl(it, path) }
            ?: throw java.lang.IllegalArgumentException("Bad request, url is not set")
        val withQueries = appendQueries(combined, queries)
        return Request(type, withQueries, body, headers, contentType, connectTimeout, responseTimeout)
    }

    @Suppress("unused")
    fun header(name: String, value: String) {
        headers.add(name to value)
    }

    @Suppress("unused")
    fun param(name: String, value: Any?) {
        value?.let { queries.add(name to it.toString()) }
    }

    @Suppress("unused")
    fun param(name: String) {
        queries.add(name to "")
    }


    private fun appendQueries(url: String, queries: Set<Pair<String, String>>): String {
        return queries.foldIndexed(url, { i, acc, query ->
            val connector = if (i == 0) "?" else "&"
            if (query.second == "") {
                "$acc$connector${query.first}"
            } else {
                "$acc$connector${query.first}=${query.second}"
            }
        })
    }

    private fun combineUrl(url: String, path: String): String {
        if (path.isEmpty()) return url
        return if (url.endsWith("/") || path.endsWith("/")) {
            "$url$path"
        } else {
            "$url/$path"
        }
    }
}

