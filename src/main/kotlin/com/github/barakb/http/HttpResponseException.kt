
package com.github.barakb.http

import java.io.IOException

@Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")
class HttpResponseException(val statusCode: Int, val reasonPhrase: String?, val requestType: RequestType, val requestUrl: String)
    : IOException("status code: $statusCode, reason phrase: $reasonPhrase, request: type=$requestType, url=$requestUrl")
