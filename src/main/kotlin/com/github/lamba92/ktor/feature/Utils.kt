package com.github.lamba92.ktor.feature

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.lamba92.ktor.feature.RestRepositories.Feature.entityIdTag
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpMethod
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpMethod.Companion.Put
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.response.respond
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
import java.util.*

fun ContentNegotiation.Configuration.restRepositories(jacksonCustomization: ObjectMapper.() -> Unit = {}) =
    jackson {
        registerModule(KtormModule())
        jacksonCustomization()
    }

suspend fun <T> Database.useTransaction(
    dispatcher: CoroutineDispatcher, isolation: TransactionIsolation = TransactionIsolation.REPEATABLE_READ,
    func: (Transaction) -> T
) = withContext(dispatcher) { useTransaction(isolation, func) }

typealias RestRepositoryInterceptor<K> = PipelineContext<Unit, ApplicationCall>.(K) -> K?

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
    ): Route.() -> Unit = if (isAuthenticated) {
        { authenticate(*authNames.toTypedArray(), build = buildRoutes(entityPath, httpMethod)) }
    } else {
        { buildRoutes(entityPath, httpMethod)() }
    }

    private fun buildRoutes(entityPath: String, httpMethod: HttpMethod): Route.() -> Unit = {
        route("$entityPath/{$entityIdTag}") {
            method(httpMethod) {
                if (httpMethod == Post || httpMethod == Put)
                    contentType(Json) { handle(single) }
                else
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

}

inline fun <reified K> String.coerce(): Any = when (K::class) {
    String::class -> this
    Int::class -> toInt()
    Long::class -> toLong()
    Float::class -> toFloat()
    Double::class -> toDouble()
    Date::class -> Date(toLong())
    else -> throw error("Unable to coerce type")
}

val PipelineContext<Unit, ApplicationCall>.entityIdParameter
    get() = call.parameters[entityIdTag]!!

inline fun <reified K> PipelineContext<Unit, ApplicationCall>.entityIdCoerced() =
    entityIdParameter.coerce<K>()

suspend inline fun <reified K> PipelineContext<Unit, ApplicationCall>.receiveEntityIds() =
    call.receive<List<Any>>().map { it.toString().coerce<K>() }

suspend inline fun <reified K> PipelineContext<Unit, ApplicationCall>.receiveEntityIds(action: (List<Any>) -> Unit) {
    val ids = receiveEntityIds<K>()
    if (ids.isEmpty())
        call.respond(HttpStatusCode.BadRequest)
    else
        action(ids)
}

suspend fun <T> ApplicationCall.respondIfNotEmpty(items: List<T>, code: HttpStatusCode = HttpStatusCode.Forbidden) =
    respond(if (items.isEmpty()) code else items)

suspend inline fun <reified K> PipelineContext<Unit, ApplicationCall>.receiveEntities() =
    call.receive<List<K>>()

suspend inline fun <reified K> PipelineContext<Unit, ApplicationCall>.receiveEntities(action: (List<K>) -> Unit) {
    val entitiesReceived = call.receive<List<K>>()
    if (entitiesReceived.isEmpty())
        call.respond(HttpStatusCode.BadRequest)
    else
        action(entitiesReceived)
}



