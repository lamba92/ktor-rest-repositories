package com.github.lamba92.ktor.feature

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpMethod.Companion.Put
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

object DefaultBehaviours {

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
            else -> error("Default methods handles only GET, POST and PUT")
        }
    }

    suspend inline fun <reified T : Entity<T>> PipelineContext<Unit, ApplicationCall>.checkEntityIdAndRetrieveIt(
        table: Table<T>,
        entityId: String
    ): Pair<T, String> {
        val entityReceived = call.receive<T>()
        table.primaryKey ?: error("Table ${table.tableName} doesn't have a primary key.")
        val primaryKeyName = (table.primaryKey.binding as NestedBinding).properties[0].name
        val primaryKeyValue = entityReceived[primaryKeyName]?.toString()
            ?: error("The value of the primary key is absent.")
        if (primaryKeyValue != entityId) error("ID of deserialized entity does not match the ID in the URL")
        return entityReceived to primaryKeyValue
    }
}