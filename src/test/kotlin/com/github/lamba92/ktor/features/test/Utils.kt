package com.github.lamba92.ktor.features.test

import com.github.lamba92.ktor.feature.restRepositories
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.basic
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationCall
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.util.InternalAPI
import io.ktor.util.encodeBase64
import it.lamba.utils.getResource
import me.liuwj.ktorm.database.Database
import kotlin.test.assertEquals

const val authVeryLongName = "wow-that-is-a-very-long-name-for-an-auth-realm-!"
fun TestApplicationCall.assertContentEquals(expected: String, message: String? = null) =
    assertEquals(expected, response.content, message)

fun TestApplicationRequest.setContentType(type: ContentType) =
    addHeader(HttpHeaders.ContentType, type.toText())

fun ContentType.toText() =
    "$contentType/$contentSubtype"

val testDatabase
    get() = getResource("test-db.sqlite").absolutePath
        .let { Database.connect("jdbc:sqlite:$it", "org.sqlite.JDBC") }


fun Application.setupApplication() {
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
}

fun TestApplicationCall.assertStatusEquals(statusCode: HttpStatusCode) =
    assertEquals(statusCode, response.status())

@InternalAPI
fun TestApplicationRequest.setBasicAuthentication(username: String, password: String) =
    addHeader(HttpHeaders.Authorization, "Basic: " + "$username:$password".toByteArray().encodeBase64())