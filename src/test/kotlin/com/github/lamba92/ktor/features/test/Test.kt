package com.github.lamba92.ktor.features.test

import com.github.lamba92.ktor.feature.RestRepositories
import com.github.lamba92.ktor.features.test.data.DoubleIdEntities
import com.github.lamba92.ktor.features.test.data.IntIdEntities
import com.github.lamba92.ktor.features.test.data.StringIdEntities
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.basic
import io.ktor.http.HttpMethod.Companion.Delete
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpMethod.Companion.Put
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import it.lamba.utils.getResource
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.TransactionIsolation
import kotlin.test.Test
import kotlin.test.assertEquals

const val authVeryLongName = "wow-that-is-a-very-long-name-for-an-auth-realm-!"

class Test {

    @Test
    fun testServer() = withTestApplication(Application::testModule) {

        with(handleRequest(Get, "repositories/doubleidentities/2,4")) {
            assertEquals(HttpStatusCode.OK, response.status())
            println(response.content)
        }

    }
}

fun Application.testModule() {
    val dbPath = it.lamba.utils.getResource("test-db.sqlite").absolutePath
    val db = Database.connect("jdbc:sqlite:$dbPath", "org.sqlite.JDBC")
    install(Authentication) {
        basic(authVeryLongName) {
            validate { (username, _) ->
                UserIdPrincipal(username)
            }
        }
        basic {
            validate {
                UserIdPrincipal(it.name)
            }
        }
    }
    install(RestRepositories) {
        registerEntity(
            StringIdEntities,
            db,
            TransactionIsolation.SERIALIZABLE
        ) {
            addMethods(
                Get,
                Post,
                Put,
                Delete
            )
        }
        registerEntity(
            DoubleIdEntities,
            db,
            TransactionIsolation.SERIALIZABLE
        ) {
            addMethods(
                Post,
                Put,
                Delete
            ) {
                isAuthenticated = true
            }
            addMethod(Get)
        }
        registerEntity(
            IntIdEntities,
            db,
            TransactionIsolation.SERIALIZABLE
        ) {
            addMethods(
                Post,
                Put,
                Delete
            ) {
                isAuthenticated = true
                authName = authVeryLongName
            }
            addMethod(Get)
        }
    }
}