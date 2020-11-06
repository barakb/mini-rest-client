package com.github.barakb.http

import java.io.IOException

@Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")
class ResponseSyntaxException(val msg: String?, val bodyText: String?, val reifiedType: Type, val requestType: RequestType, requestUrl: String)
    : IOException("message: $msg, input:$bodyText, output: $reifiedType, request: request: type=$requestType, url=$requestUrl")
