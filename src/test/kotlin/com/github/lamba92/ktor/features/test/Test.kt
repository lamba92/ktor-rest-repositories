package com.github.lamba92.ktor.features.test

import io.ktor.application.Application
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.ktor.util.InternalAPI
import kotlin.test.Test
import kotlin.test.assertEquals

@InternalAPI
class Test {

    @Test
    fun `string entities test`() = withTestApplication(Application::testModule) {

        with(handleRequest(Get, "repositories/strings/a")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertContentEquals("{\"id\":\"a\",\"value1\":\"b\",\"value2\":2}")
        }
        with(handleRequest(Get, "repositories/strings") {
            setContentType(ContentType.Application.Json)
            setBody("[\"a\"]")
        }) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertContentEquals("[{\"id\":\"a\",\"value1\":\"b\",\"value2\":2}]")
        }
    }

    @Test
    fun `integer entities test`() = withTestApplication(Application::testModule) {
        with(handleRequest(Get, "repositories/intidentities/1")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals("{\"id\":1,\"value1\":\"ciao\",\"value2\":3}", response.content)
        }
        with(handleRequest(Get, "repositories/intidentities") {
            setContentType(ContentType.Application.Json)
            setBody("[1]")
        }) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertContentEquals("[{\"id\":1,\"value1\":\"ciao\",\"value2\":3}]")
        }
    }

    @Test
    fun `HTTP Put error reproduction`() = withTestApplication(Application::httpPutErrorTestModule) {

    }
}


