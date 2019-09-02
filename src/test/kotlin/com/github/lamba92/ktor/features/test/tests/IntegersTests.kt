package com.github.lamba92.ktor.features.test.tests

import com.github.lamba92.ktor.features.test.*
import com.github.lamba92.ktor.features.test.data.IntIdEntities
import io.ktor.application.Application
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod.Companion.Delete
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.ktor.util.InternalAPI
import org.junit.Test

@InternalAPI
class IntegersTests : RestRepositoriesTest {

    override val restRepoPath = "repositories"
    override val entitiesPath = IntIdEntities::class.simpleName!!.toLowerCase()

    @Test
    override fun `single item GET test`() = withTestApplication(Application::integerTestModule) {
        with(handleRequest(Get, "$urlPath/1")) {
            assertStatusEquals(HttpStatusCode.OK)
            assertContentEquals("{\"id\":1,\"value1\":\"ciao\",\"value2\":3}")
        }
    }

    @Test
    override fun `multiple item GET test`() = withTestApplication(Application::integerTestModule) {
        with(handleRequest(Get, urlPath) {
            setContentType(ContentType.Application.Json)
            setBody("[\"1\"]")
        }) {
            assertStatusEquals(HttpStatusCode.OK)
            assertContentEquals("[{\"id\":1,\"value1\":\"ciao\",\"value2\":3}]")
        }
    }

    @Test(expected = NotImplementedError::class)
    override fun `single item PUT test`() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    @Test(expected = NotImplementedError::class)
    override fun `multiple item PUT test`() {
        TODO("not implemented")
    }

    @Test
    override fun `single item POST test`() = withTestApplication(Application::integerTestModule) {
        with(handleRequest(Post, "$urlPath/1") {
            setContentType(ContentType.Application.Json)
            setBasicAuthentication("test", "test")
            setBody("{\"id\":1,\"value1\":\"ciao mamma\",\"value2\":30}")
        }) {
            assertStatusEquals(HttpStatusCode.OK)
            assertContentEquals("{\"id\":1,\"value1\":\"ciao mamma\",\"value2\":30}")
        }
    }

    @Test
    override fun `multiple item POST test`() = withTestApplication(Application::integerTestModule) {
        with(handleRequest(Post, urlPath) {
            setContentType(ContentType.Application.Json)
            setBasicAuthentication("test", "test")
            setBody("[{\"id\":1,\"value1\":\"ciao mamma 2\",\"value2\":300}]")
        }) {
            assertStatusEquals(HttpStatusCode.OK)
            assertContentEquals("[{\"id\":1,\"value1\":\"ciao mamma 2\",\"value2\":300}]")
        }
    }

    @Test
    override fun `single item DELETE test`() = withTestApplication(Application::integerTestModule) {
        with(handleRequest(Delete, "$urlPath/1") { setBasicAuthentication("test", "test") }) {
            assertStatusEquals(HttpStatusCode.OK)
        }
    }

    @Test
    override fun `multiple item DELETE test`() = withTestApplication(Application::integerTestModule) {
        with(handleRequest(Delete, urlPath) {
            setContentType(ContentType.Application.Json)
            setBasicAuthentication("test", "test")
            setBody("[1]")
        }) {
            assertStatusEquals(HttpStatusCode.OK)
        }
    }

}
