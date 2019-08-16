package com.github.lamba92.ktor.features.test

import com.github.lamba92.ktor.feature.RestRepositories
import com.github.lamba92.ktor.feature.restRepositories
import com.github.lamba92.ktor.features.test.data.*
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.basic
import io.ktor.auth.principal
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpMethod.Companion.Delete
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpMethod.Companion.Put
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.ktor.util.InternalAPI
import io.ktor.util.encodeBase64
import it.lamba.utils.getResource
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.TransactionIsolation.SERIALIZABLE
import kotlin.test.Test
import kotlin.test.assertEquals

const val authVeryLongName = "wow-that-is-a-very-long-name-for-an-auth-realm-!"

class Test {

    @InternalAPI
    @Test
    fun testServer() = withTestApplication(Application::testModule) {

        with(handleRequest(Get, "repositories/doubleidentities/2.4")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals("{\"id\":2.4,\"value1\":\"LOL\",\"value2\":3}", response.content)
        }

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
}

fun Application.testModule() {
    val dbPath = getResource("test-db.sqlite").absolutePath
    val db = Database.connect("jdbc:sqlite:$dbPath", "org.sqlite.JDBC")
    install(Authentication) {
        basic(authVeryLongName) {
            validate { (username, _) ->
                UserIdPrincipal(username)
            }
        }
        basic {
            validate { (username, _) ->
                UserIdPrincipal(username)
            }
        }
    }
    install(ContentNegotiation) {
        restRepositories()
    }
    install(RestRepositories) {
        registerEntity<StringIdEntity, String>(StringIdEntities, db, SERIALIZABLE) {
            addEndpoint(Put)
        }
        registerEntity<DoubleIdEntity, Double>(DoubleIdEntities, db, SERIALIZABLE) {
            addEndpoints(Post, Put, Delete) {
                isAuthenticated = true
                restRepositoryInterceptor = { entity ->
                    assert(entity.value1 == call.principal<UserIdPrincipal>()!!.name) { "value1 != userId" }
                    entity
                }
            }
            addEndpoint(Get)
        }
        registerEntity<IntIdEntity, Int>(IntIdEntities, db, SERIALIZABLE) {
            addEndpoints(Post, Put, Delete) {
                isAuthenticated = true
                authName = authVeryLongName
                restRepositoryInterceptor = { entity ->
                    assert(entity.value1 == call.principal<UserIdPrincipal>()!!.name) { "value1 != userId" }
                    entity
                }
            }
            addEndpoint(Get)
        }
    }
}