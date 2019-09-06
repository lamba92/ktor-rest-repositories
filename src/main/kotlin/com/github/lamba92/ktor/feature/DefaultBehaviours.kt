package com.github.lamba92.ktor.feature

import com.github.lamba92.ktor.feature.EndpointMultiplicity.MULTIPLE
import com.github.lamba92.ktor.feature.EndpointMultiplicity.SINGLE
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

inline fun <reified T : Entity<T>, reified K> httpGetDefaultSingleItemBehaviour(
    table: Table<T>,
    database: Database,
    isolation: TransactionIsolation,
    crossinline customAction: RestRepositoryInterceptor<T> = { it }
): PipelineInterceptor<Unit, ApplicationCall> = {
    database.useTransaction(IO, isolation) {
        table.findById(entityIdCoerced<K>())!!
    }
        .let { customAction(it) }
        .let { call.respond(it ?: HttpStatusCode.Forbidden) }
}

inline fun <reified T : Entity<T>, reified K> httpGetDefaultMultipleItemBehaviour(
    table: Table<T>,
    database: Database,
    isolation: TransactionIsolation,
    crossinline customAction: RestRepositoryInterceptor<T> = { it }
): PipelineInterceptor<Unit, ApplicationCall> = {
    receiveEntityIds<K> { ids ->
        database.useTransaction(IO, isolation) {
            ids.map { table.findById(it)!! }
        }
            .mapNotNull { customAction(it) }
            .let { call.respondIfNotEmpty(it) }
    }
}

inline fun <reified T : Entity<T>, reified K> httpPostDefaultSingleItemBehaviour(
    table: Table<T>,
    database: Database,
    isolation: TransactionIsolation,
    crossinline customAction: RestRepositoryInterceptor<T> = { it }
): PipelineInterceptor<Unit, ApplicationCall> = {
    val entityReceived = customAction(checkEntityId(table, entityIdParameter, call.receive()))
    if (entityReceived != null)
        database.useTransaction(IO, isolation) {
            table.updateColumnsByEntity(entityReceived)
        }
            .let { call.respond(it) }
    else
        call.respond(HttpStatusCode.Forbidden)
}

inline fun <reified T : Entity<T>, reified K> httpPostDefaultMultipleItemBehaviour(
    table: Table<T>,
    database: Database,
    isolation: TransactionIsolation,
    crossinline customAction: RestRepositoryInterceptor<T> = { it }
): PipelineInterceptor<Unit, ApplicationCall> = {
    receiveEntities<T> { entities ->
        entities.mapNotNull { customAction(it) }
            .map { entityReceived ->
                database.useTransaction(IO, isolation) {
                    table.updateColumnsByEntity(entityReceived)
                }
            }
            .let { call.respondIfNotEmpty(it) }
    }
}

inline fun <reified T : Entity<T>, reified K> httpPutDefaultSingleItemBehaviour(
    table: Table<T>,
    database: Database,
    isolation: TransactionIsolation,
    crossinline customAction: RestRepositoryInterceptor<T> = { it }
): PipelineInterceptor<Unit, ApplicationCall> = {
    customAction(checkEntityId(table, entityIdParameter, call.receive()))
        ?.let { entityReceived ->
            database.useTransaction(IO, isolation) {
                table.insert {
                    entityReceived.properties.forEach { (name, value) ->
                        it[name] to value
                    }
                }
            }
            call.respond(entityReceived)
        }
        ?: call.respond(HttpStatusCode.Forbidden)
}

inline fun <reified T : Entity<T>, reified K> httpPutDefaultMultipleItemBehaviour(
    table: Table<T>,
    database: Database,
    isolation: TransactionIsolation,
    crossinline customAction: RestRepositoryInterceptor<T> = { it }
): PipelineInterceptor<Unit, ApplicationCall> = {
    val entitiesReceived = receiveEntities<T>()
        .mapNotNull { customAction(it) }

    if (entitiesReceived.isNotEmpty()) {
        database.useTransaction(IO, isolation) {
            entitiesReceived.forEach { entityReceived ->
                table.insert {
                    entityReceived.properties.forEach { (name, value) ->
                        it[name] to value
                    }
                }
            }
        }
        call.respond(entitiesReceived)
    } else
        call.respond(HttpStatusCode.Forbidden)
}

inline fun <reified T : Entity<T>, reified K> httpDeleteDefaultSingleItemBehaviour(
    table: Table<T>,
    database: Database,
    isolation: TransactionIsolation,
    crossinline customAction: RestRepositoryInterceptor<T> = { it }
): PipelineInterceptor<Unit, ApplicationCall> = {
    database.useTransaction(IO, isolation) {
        table.findById(entityIdCoerced<K>())!!
    }
        .let { customAction(it) }
        ?.let { entity ->
            database.useTransaction(IO, isolation) { entity.delete() }
            call.respond(HttpStatusCode.OK)
        }
        ?: call.respond(HttpStatusCode.Forbidden)
}

inline fun <reified T : Entity<T>, reified K> httpDeleteDefaultMultipleItemBehaviour(
    table: Table<T>,
    database: Database,
    isolation: TransactionIsolation,
    crossinline customAction: RestRepositoryInterceptor<T> = { it }
): PipelineInterceptor<Unit, ApplicationCall> = {
    receiveEntities<T> { ids ->
        val entitiesFiltered = database.useTransaction(IO, isolation) {
            ids.map { table.findById(it)!! }
        }
            .mapNotNull { customAction(it) }

        if (entitiesFiltered.isEmpty())
            call.respond(HttpStatusCode.Forbidden)
        else {
            database.useTransaction(IO, isolation) {
                entitiesFiltered.forEach { it.delete() }
            }
            call.respond(HttpStatusCode.OK)
        }
    }
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
): InterceptorsContainer {
    val single: suspend (PipelineContext<Unit, ApplicationCall>, Unit) -> Unit = {
        getDefaultBehaviour<T, K>(
            SINGLE,
            table,
            httpMethod,
            database,
            isolation,
            customAction
        )
    }()
    val multiple: suspend (PipelineContext<Unit, ApplicationCall>, Unit) -> Unit = {
        getDefaultBehaviour<T, K>(
            MULTIPLE,
            table,
            httpMethod,
            database,
            isolation,
            customAction
        )
    }()
    return InterceptorsContainer(
        single,
        multiple
    )
}


enum class EndpointMultiplicity {
    SINGLE, MULTIPLE
}

inline fun <reified T : Entity<T>> checkEntityId(
    table: Table<T>,
    entityId: String,
    entityReceived: T
): T {
    table.primaryKey ?: error("Table ${table.tableName} doesn't have a primary key.")
    val primaryKeyName = (table.primaryKey!!.binding as NestedBinding).properties[0].name
    val primaryKeyValue = entityReceived[primaryKeyName]
        ?: error("The value of the primary key is absent.")
    if (primaryKeyValue.toString() != entityId)
        error("ID of deserialized entity does not match the ID in the URL")
    return entityReceived
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



