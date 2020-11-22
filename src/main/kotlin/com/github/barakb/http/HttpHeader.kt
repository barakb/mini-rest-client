package com.github.barakb.http

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class HttpHeader(val name: String)