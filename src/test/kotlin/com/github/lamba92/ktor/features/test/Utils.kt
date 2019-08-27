package com.github.lamba92.ktor.features.test

import io.ktor.server.testing.TestApplicationCall
import kotlin.test.assertEquals

const val authVeryLongName = "wow-that-is-a-very-long-name-for-an-auth-realm-!"
fun TestApplicationCall.assertContentEquals(expected: String, message: String? = null) =
    assertEquals(expected, response.content, message)