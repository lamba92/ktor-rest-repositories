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
import it.lamba.utils.getResource
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.TransactionIsolation.SERIALIZABLE

fun Application.testModule() {

    val db = getResource("test-db.sqlite").absolutePath
        .let { Database.connect("jdbc:sqlite:$it", "org.sqlite.JDBC") }

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
            entityPath = "strings"
            addEndpoint(Get) {
                restRepositoryInterceptor = { entity ->
                    assert(entity.value1 == call.principal<UserIdPrincipal>()!!.name) { "value1 != userId" }
                    entity
                }
            }
        }
        registerEntity<DoubleIdEntity, Double>(DoubleIdEntities, db, SERIALIZABLE) {
            addEndpoints(Post, Delete) {
                isAuthenticated = true
                restRepositoryInterceptor = { entity ->
                    assert(entity.value1 == call.principal<UserIdPrincipal>()!!.name) { "value1 != userId" }
                    entity
                }
            }
            addEndpoint(Get)
        }
        registerEntity<IntIdEntity, Int>(IntIdEntities, db, SERIALIZABLE) {
            addEndpoints(Post, Delete) {
                isAuthenticated = true
                authNames = listOf(authVeryLongName)
                restRepositoryInterceptor = { entity ->
                    assert(entity.value1 == call.principal<UserIdPrincipal>()!!.name) { "value1 != userId" }
                    entity
                }
            }
            addEndpoint(Get)
        }
    }
}

fun Application.httpPutErrorTestModule() {
    val db = getResource("test-db.sqlite").absolutePath
        .let { Database.connect("jdbc:sqlite:$it", "org.sqlite.JDBC") }

    install(ContentNegotiation) {
        restRepositories()
    }

    install(RestRepositories) {
        registerEntity<StringIdEntity, String>(StringIdEntities, db, SERIALIZABLE) {
            addEndpoint(Put)
        }
    }

}