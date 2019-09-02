package com.github.lamba92.ktor.feature

import com.github.lamba92.ktor.feature.EndpointMultiplicity.MULTIPLE
import com.github.lamba92.ktor.feature.EndpointMultiplicity.SINGLE
import com.github.lamba92.ktor.feature.RestRepositories.Feature.entityIdTag
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.http.HttpMethod.Companion.Delete
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpMethod.Companion.Put
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.pipeline.PipelineInterceptor
import kotlinx.coroutines.Dispatchers.IO
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.TransactionIsolation
import me.liuwj.ktorm.dsl.insert
import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.entity.findById
import me.liuwj.ktorm.schema.NestedBinding
import me.liuwj.ktorm.schema.Table
import java.util.*


inline fun <reified K> String.coerce(): Any = when (K::class) {
    String::class -> this
    Int::class -> toInt()
    Long::class -> toLong()
    Float::class -> toFloat()
    Double::class -> toDouble()
    Date::class -> Date(toLong())
    else -> throw error("Unable to coerce type")
}

inline fun <reified T : Entity<T>, reified K> httpGetDefaultSingleItemBehaviour(
    table: Table<T>,
    database: Database,
    isolation: TransactionIsolation,
    crossinline customAction: RestRepositoryInterceptor<T> = { it }
): PipelineInterceptor<Unit, ApplicationCall> = {
    database.useTransaction(IO, isolation) {
        table.findById(call.parameters[entityIdTag]!!.coerce<K>())!!
    }
        .let { customAction(it) }
        .let { call.respond(it) }
}

inline fun <reified T : Entity<T>, reified K> httpGetDefaultMultipleItemBehaviour(
    table: Table<T>,
    database: Database,
    isolation: TransactionIsolation,
    crossinline customAction: RestRepositoryInterceptor<T> = { it }
): PipelineInterceptor<Unit, ApplicationCall> = {
    val ids = call.receive<List<Any>>()
        .map { it.toString().coerce<K>() }
    database.useTransaction(IO, isolation) {
        ids.map { table.findById(it)!! }
    }
        .map { customAction(it) }
        .let { call.respond(it) }
}

inline fun <reified T : Entity<T>, reified K> httpPostDefaultSingleItemBehaviour(
    table: Table<T>,
    database: Database,
    isolation: TransactionIsolation,
    crossinline customAction: RestRepositoryInterceptor<T> = { it }
): PipelineInterceptor<Unit, ApplicationCall> = {
    val entityId = call.parameters[entityIdTag]!!
    val entityReceived = customAction(checkEntityIdAndRetrieveIt(table, entityId).first)
    database.useTransaction(IO, isolation) {
        table.updateColumnsByEntity(entityReceived)
    }.let { call.respond(it) }
}

inline fun <reified T : Entity<T>, reified K> httpPostDefaultMultipleItemBehaviour(
    table: Table<T>,
    database: Database,
    isolation: TransactionIsolation,
    crossinline customAction: RestRepositoryInterceptor<T> = { it }
): PipelineInterceptor<Unit, ApplicationCall> = {
    call.receive<List<Any>>()
        .map { it.toString() }
        .map { checkEntityIdAndRetrieveIt(table, it) }
        .map { customAction(it.first) }
        .map { entityReceived ->
            database.useTransaction(IO, isolation) {
                table.updateColumnsByEntity(entityReceived)
            }
        }
        .let { call.respond(it) }
}

inline fun <reified T : Entity<T>, reified K> httpPutDefaultSingleItemBehaviour(
    table: Table<T>,
    database: Database,
    isolation: TransactionIsolation,
    crossinline customAction: RestRepositoryInterceptor<T> = { it }
): PipelineInterceptor<Unit, ApplicationCall> = {
    val entityReceived = call.receive<T>()
    val checkedEntity = customAction(entityReceived)
    database.useTransaction(IO, isolation) {
        table.insert {
            checkedEntity.properties.forEach { (name, value) ->
                it[name] to value
            }
        }
    }
        .let { call.respond(entityReceived) }
}

inline fun <reified T : Entity<T>, reified K> httpPutDefaultMultipleItemBehaviour(
    table: Table<T>,
    database: Database,
    isolation: TransactionIsolation,
    crossinline customAction: RestRepositoryInterceptor<T> = { it }
): PipelineInterceptor<Unit, ApplicationCall> = {
    val entitiesReceived = call.receive<List<T>>()
        .map { customAction(it) }
    database.useTransaction(IO, isolation) {
        entitiesReceived.forEach { entityReceived ->
            table.insert {
                entityReceived.properties.forEach { (name, value) ->
                    it[name] to value
                }
            }
        }
    }.let { call.respond(entitiesReceived) }
}

inline fun <reified T : Entity<T>, reified K> httpDeleteDefaultSingleItemBehaviour(
    table: Table<T>,
    database: Database,
    isolation: TransactionIsolation,
    crossinline customAction: RestRepositoryInterceptor<T> = { it }
): PipelineInterceptor<Unit, ApplicationCall> = {
    database.useTransaction(IO, isolation) {
        table.findById(call.parameters[entityIdTag]!!.coerce<K>())!!
    }
        .let { customAction(it) }
        .let { entity ->
            database.useTransaction(IO, isolation) { entity.delete() }
            call.respond(HttpStatusCode.OK)
        }
}

inline fun <reified T : Entity<T>, reified K> httpDeleteDefaultMultipleItemBehaviour(
    table: Table<T>,
    database: Database,
    isolation: TransactionIsolation,
    crossinline customAction: RestRepositoryInterceptor<T> = { it }
): PipelineInterceptor<Unit, ApplicationCall> = {
    call.receive<List<Any>>()
        .map { it.toString().coerce<K>() }
        .let { ids ->
            database.useTransaction(IO, isolation) {
                ids.map { table.findById(it)!! }
            }
        }
        .map { customAction(it) }
        .map { entity ->
            database.useTransaction(IO, isolation) { entity.delete() }
        }
    call.respond(HttpStatusCode.OK)
}

inline fun <reified T : Entity<T>, reified K> getDefaultBehaviour(
    endpointMultiplicity: EndpointMultiplicity,
    table: Table<T>,
    httpMethod: HttpMethod,
    database: Database,
    isolation: TransactionIsolation,
    crossinline customAction: RestRepositoryInterceptor<T> = { it }
): PipelineInterceptor<Unit, ApplicationCall> = when (endpointMultiplicity) {
    SINGLE -> when (httpMethod) {
        Get -> httpGetDefaultSingleItemBehaviour<T, K>(table, database, isolation, customAction)
        Post -> httpPostDefaultSingleItemBehaviour<T, K>(table, database, isolation, customAction)
        Put -> httpPutDefaultSingleItemBehaviour<T, K>(table, database, isolation, customAction)
        Delete -> httpDeleteDefaultSingleItemBehaviour<T, K>(table, database, isolation, customAction)
        else -> error("Defaults handle only GET, POST, PUT and DELETE")
    }
    MULTIPLE -> when (httpMethod) {
        Get -> httpGetDefaultMultipleItemBehaviour<T, K>(table, database, isolation, customAction)
        Post -> httpPostDefaultMultipleItemBehaviour<T, K>(table, database, isolation, customAction)
        Put -> httpPutDefaultMultipleItemBehaviour<T, K>(table, database, isolation, customAction)
        Delete -> httpDeleteDefaultMultipleItemBehaviour<T, K>(table, database, isolation, customAction)
        else -> error("Defaults handle only GET, POST, PUT and DELETE")
    }
}

inline fun <reified T : Entity<T>, reified K> getDefaultBehaviour(
    table: Table<T>,
    httpMethod: HttpMethod,
    database: Database,
    isolation: TransactionIsolation,
    crossinline customAction: RestRepositoryInterceptor<T>
) = InterceptorsContainer(
    getDefaultBehaviour<T, K>(
        SINGLE,
        table,
        httpMethod,
        database,
        isolation,
        customAction
    ),
    getDefaultBehaviour<T, K>(
        MULTIPLE,
        table,
        httpMethod,
        database,
        isolation,
        customAction
    )
)

enum class EndpointMultiplicity {
    SINGLE, MULTIPLE
}

suspend inline fun <reified T : Entity<T>> PipelineContext<Unit, ApplicationCall>.checkEntityIdAndRetrieveIt(
    table: Table<T>,
    entityId: String
): Pair<T, Any> {
    val entityReceived = call.receive<T>()
    table.primaryKey ?: error("Table ${table.tableName} doesn't have a primary key.")
    val primaryKeyName = (table.primaryKey!!.binding as NestedBinding).properties[0].name
    val primaryKeyValue = entityReceived[primaryKeyName]
        ?: error("The value of the primary key is absent.")
    if (primaryKeyValue.toString() != entityId)
        error("ID of deserialized entity does not match the ID in the URL")
    return entityReceived to primaryKeyValue
}

fun <E : Entity<E>> Table<E>.updateColumnsByEntity(entity: E): E {
    val primaryKey = primaryKey ?: error("Table $tableName doesn't have a primary key.")

    val primaryKeyName = (primaryKey.binding as NestedBinding).properties[0].name
    val primaryKeyValue = entity[primaryKeyName] ?: error("The value of the primary key is absent.")

    val entityFromDb = findById(primaryKeyValue) ?: error("Entity not found for id: $primaryKeyValue")
    for ((name, value) in entity.properties) {
        if (name != primaryKeyName && value != null) {
            entityFromDb[name] = value
        }
    }

    entityFromDb.flushChanges()
    return entityFromDb
}



