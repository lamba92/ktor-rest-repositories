package com.github.lamba92.ktor.features.test

import com.github.lamba92.ktor.feature.RestRepositories
import com.github.lamba92.ktor.features.test.data.DoubleIdEntities
import com.github.lamba92.ktor.features.test.data.LongIdEntities
import com.github.lamba92.ktor.features.test.data.StringIdEntities
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.basic
import io.ktor.http.HttpMethod.Companion.Delete
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpMethod.Companion.Put
import io.ktor.server.testing.withTestApplication
import kotlin.test.Test

class Test {

    val authVeryLongName = "wow-that-is-a-very-long-name-for-an-auth-realm-!"

    @Test
    fun testServer() {
        withTestApplication {
            with(application) {
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
                    registerEntity(StringIdEntities) {
                        addMethods(Get, Post, Put, Delete)
                    }
                    registerEntity(DoubleIdEntities) {
                        addMethods(Post, Put, Delete) {
                            isAuthenticated = true
                        }
                        addMethod(Get)
                    }
                    registerEntity(LongIdEntities) {
                        addMethods(Post, Put, Delete) {
                            isAuthenticated = true
                            authName = authVeryLongName
                        }
                        addMethod(Get)
                    }
                }
            }
        }
    }
}