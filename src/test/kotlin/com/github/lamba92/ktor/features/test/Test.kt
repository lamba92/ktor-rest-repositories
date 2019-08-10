package com.github.lamba92.ktor.features.test

import com.github.lamba92.ktor.feature.RestRepositories
import io.ktor.application.install
import io.ktor.server.testing.withTestApplication

class Test {

    fun testServer() {
        withTestApplication {
            application.install(RestRepositories) {
            }
        }
    }


}