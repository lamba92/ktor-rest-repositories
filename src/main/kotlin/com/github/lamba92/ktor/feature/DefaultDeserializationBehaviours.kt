package com.github.lamba92.ktor.feature

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.http.HttpMethod.Companion.Delete
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpMethod.Companion.Put
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.util.pipeline.PipelineInterceptor
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.schema.Table
import kotlin.reflect.KClass

object DefaultDeserializationBehaviours {
    inline operator fun <reified T : Entity<T>> invoke(
        httpMethod: HttpMethod,
        entity: KClass<out Entity<*>>?,
        table: Table<T>
    ): PipelineInterceptor<Unit, ApplicationCall> = {
        when (httpMethod) {
            Get -> {
                call.respond(entity)
            }
            Post, Put -> {
                table.updateColumnsByEntity(call.receive())
            }
            Delete -> {
                withContext(IO) {
                    table.updateColumnsByEntity(call.receive(), false).delete()
                }
            }
            else -> throw NotImplementedError("HTTP method ${httpMethod.value} has not yet been implemented for entity of type ${entity.entityClass.simpleName}")
        }
    }
}