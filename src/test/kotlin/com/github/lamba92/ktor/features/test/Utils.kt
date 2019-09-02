package com.github.lamba92.ktor.features.test

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.testing.TestApplicationCall
import io.ktor.server.testing.TestApplicationRequest
import kotlin.test.assertEquals

const val authVeryLongName = "wow-that-is-a-very-long-name-for-an-auth-realm-!"
fun TestApplicationCall.assertContentEquals(expected: String, message: String? = null) =
    assertEquals(expected, response.content, message)

fun TestApplicationRequest.setContentType(type: ContentType) =
    addHeader(HttpHeaders.ContentType, type.toText())

fun ContentType.toText() =
    "$contentType/$contentSubtype"
