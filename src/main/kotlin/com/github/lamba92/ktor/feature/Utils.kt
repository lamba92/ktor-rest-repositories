package com.github.lamba92.ktor.feature

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.lamba92.ktor.feature.RestRepositories.Feature.entityIdTag
import io.ktor.application.ApplicationCall
import io.ktor.auth.authenticate
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpMethod
import io.ktor.jackson.jackson
import io.ktor.routing.Route
import io.ktor.routing.contentType
import io.ktor.routing.method
import io.ktor.routing.route
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.pipeline.PipelineInterceptor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.Transaction
import me.liuwj.ktorm.database.TransactionIsolation
import me.liuwj.ktorm.jackson.KtormModule

fun ContentNegotiation.Configuration.restRepositories(jacksonCustomization: ObjectMapper.() -> Unit = {}) =
    jackson {
        registerModule(KtormModule())
        jacksonCustomization()
    }

suspend fun <T> Database.useTransaction(
    dispatcher: CoroutineDispatcher, isolation: TransactionIsolation = TransactionIsolation.REPEATABLE_READ,
    func: (Transaction) -> T
) = withContext(dispatcher) { useTransaction(isolation, func) }

typealias RestRepositoryInterceptor<K> = PipelineContext<Unit, ApplicationCall>.(K) -> K

val String.withoutWhitespaces
    get() = filter { !it.isWhitespace() }

data class InterceptorsContainer(
    val single: PipelineInterceptor<Unit, ApplicationCall>,
    val multiple: PipelineInterceptor<Unit, ApplicationCall>
) {
    fun toRoute(
        entityPath: String,
        httpMethod: HttpMethod,
        isAuthenticated: Boolean,
        authNames: List<String?>
    ): Route.() -> Unit {
        val routes: Route.() -> Unit = {
            route("$entityPath/{$entityIdTag}") {
                method(httpMethod) {
                    handle(single)
                }
            }
            route(entityPath) {
                method(httpMethod) {
                    contentType(Json) {
                        handle(multiple)
                    }
                }
            }
        }
        if (isAuthenticated)
            return { authenticate(*authNames.toTypedArray(), build = routes) }
        else
            return { routes() }
    }
}
