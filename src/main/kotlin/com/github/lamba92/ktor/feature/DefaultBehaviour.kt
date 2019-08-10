package com.github.lamba92.ktor.feature

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
import kotlinx.coroutines.withContext
import me.liuwj.ktorm.dsl.insert
import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.entity.findById
import me.liuwj.ktorm.schema.NestedBinding
import me.liuwj.ktorm.schema.Table

object DefaultBehaviour {

    inline operator fun <reified T : Entity<T>> invoke(
        table: Table<T>,
        httpMethod: HttpMethod
    ): PipelineInterceptor<Unit, ApplicationCall> = {
        val entityId = call.parameters["entityId"]!!
        when (httpMethod) {
            Get -> {
                call.respond(withContext(IO) { table.findById(entityId)!! })
            }
            Post -> {
                val (entityReceived, _) = checkEntityIdAndRetrieveIt(table, entityId)
                call.respond(table.updateColumnsByEntity(entityReceived))
            }
            Put -> {
                val (entityReceived, primaryKeyValue) = checkEntityIdAndRetrieveIt(table, entityId)
                withContext(IO) {
                    table.insert {
                        entityReceived.properties.forEach { (name, value) ->
                            it[name] to value
                        }
                    }
                }
                call.respond(withContext(IO) { table.findById(primaryKeyValue)!! })
            }
            Delete -> {
                val (entityReceived, _) = checkEntityIdAndRetrieveIt(table, entityId)
                withContext(IO) {
                    entityReceived.delete()
                }
                call.respond(HttpStatusCode.OK)
            }
            else -> error("Defaults handle only GET, POST, PUT and DELETE")
        }
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

    suspend fun <E : Entity<E>> Table<E>.updateColumnsByEntity(entity: E, updateValues: Boolean = true): E =
        withContext(IO) {
            val primaryKey = primaryKey ?: error("Table $tableName doesn't have a primary key.")

            val primaryKeyName = (primaryKey.binding as NestedBinding).properties[0].name
            val primaryKeyValue = entity[primaryKeyName] ?: error("The value of the primary key is absent.")

            val entityFromDb = findById(primaryKeyValue) ?: error("Entity not found for id: $primaryKeyValue")
            if (updateValues) {
                for ((name, value) in entity.properties) {
                    if (name != primaryKeyName && value != null) {
                        entityFromDb[name] = value
                    }
                }

                entityFromDb.flushChanges()
            }
            entityFromDb
        }
}