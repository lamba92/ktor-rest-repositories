package com.github.lamba92.ktor.features.test

import io.ktor.application.Application
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Put
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.ktor.util.InternalAPI
import io.ktor.util.encodeBase64
import kotlin.test.Test
import kotlin.test.assertEquals

@InternalAPI
class Test {

    @Test
    fun `string entities test`() = withTestApplication(Application::testModule) {

        with(handleRequest(Get, "repositories/strings/a")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertContentEquals("{\"id\":a,\"value1\":\"b\",\"value2\":2}")
            assertEquals("{\"id\":a,\"value1\":\"b\",\"value2\":2}", response.content)
        }
    }

    @Test
    fun `integer entities test`() = withTestApplication(Application::testModule) {
        with(handleRequest(Get, "repositories/intidentities/1")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals("{\"id\":1,\"value1\":\"ciao\",\"value2\":3}", response.content)
        }
        with(handleRequest {
            method = Put
            uri = "repositories/intidentities"
            addHeader("Authorization", "Basic ${"ciao:rossi".toByteArray().encodeBase64()}")
            setBody(
                """{
                    |    "value1": "mario"
                    |    "value2": 4
                    |}""".trimMargin()
            )
        }) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals("{\"id\":2,\"value1\":\"mario\",\"value2\":4}", response.content)
        }
    }

    @Test
    fun `HTTP Put error reproduction`() = withTestApplication(Application::httpPutErrorTestModule) {

    }
}