package com.github.lamba92.ktor.feature

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpMethod.Companion.Put
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineInterceptor
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import me.liuwj.ktorm.dsl.Query
import me.liuwj.ktorm.dsl.QueryRowSet
import me.liuwj.ktorm.dsl.insert
import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.entity.createEntity
import me.liuwj.ktorm.entity.findById
import me.liuwj.ktorm.schema.NestedBinding
import me.liuwj.ktorm.schema.Table
import kotlin.reflect.KClass
import kotlin.text.get

class RestRepositories private constructor(val configuration: Configuration) {

    companion object Feature : ApplicationFeature<Application, Configuration, RestRepositories> {

        override val key = AttributeKey<RestRepositories>("RestRepositories")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): RestRepositories {

            val feature = RestRepositories(Configuration().apply(configure))

            pipeline.routing {
                route(feature.configuration.path) {
                    feature.configuration.entitiesConfigurationMap.forEach { (table, entityData) ->
                        route("${entityData.entityPath}/{entityId}") {
                            entityData.configuredMethods.forEach { (httpMethod, behaviour) ->

                            }
                        }
                    }
                }
            }

            return feature
        }
    }

    data class Configuration(
        val entitiesConfigurationMap: MutableMap<Table<out Entity<*>>, EntityHttpMethods<out Entity<*>>> = mutableMapOf(),
        var path: String = "repositories"
    ) {

        inline fun <reified T : Entity<T>> registerEntity(
            table: Table<T>,
            entityPath: String = T::class.simpleName!!,
            httpMethodConf: EntityHttpMethods<T>.() -> Unit
        ) {
            val data = EntityHttpMethods<T>(entityPath)
                .apply(httpMethodConf)
            entitiesConfigurationMap[table] = data
            val action: PipelineInterceptor<Unit, ApplicationCall> = {
                data.configuredMethods.forEach { (httpMethod, behaviour) ->
                    val entityId = call.parameters["entityId"]!!
                    when (httpMethod) {
                        Get -> {
                            call.respond(withContext(IO) { table.findById(entityId)!! })
                        }
                        Post -> {
                            val entityReceived = call.receive<T>()
                            table.primaryKey ?: error("Table ${table.tableName} doesn't have a primary key.")
                            val primaryKeyName = (table.primaryKey.binding as NestedBinding).properties[0].name
                            val primaryKeyValue = entityReceived[primaryKeyName]?.toString()
                                ?: error("The value of the primary key is absent.")
                            if (primaryKeyValue != entityId) error("ID of deserialized entity does not match the ID in the URL")
                            call.respond(table.updateColumnsByEntity(entityReceived))
                        }
                        Put -> {
                            val entityReceived = call.receive<T>()
                            table.primaryKey ?: error("Table ${table.tableName} doesn't have a primary key.")
                            val primaryKeyName = (table.primaryKey.binding as NestedBinding).properties[0].name
                            val primaryKeyValue = entityReceived[primaryKeyName]?.toString()
                                ?: error("The value of the primary key is absent.")
                            if (primaryKeyValue != entityId) error("ID of deserialized entity does not match the ID in the URL")
                            withContext(IO) {
                                table.insert {
                                    entityReceived.properties.forEach { (name, value) ->
                                        it[name] to value
                                    }
                                }
                            }
                            call.respond(withContext(IO) { table.findById(primaryKeyValue)!! })
                        }
                    }
                }
            }
        }

        class EntityHttpMethods<T : Entity<T>>(var entityPath: String) {

            val configuredMethods = mutableMapOf<HttpMethod, Behaviour<T>>()

            fun addMethod(httpMethod: HttpMethod, behaviourConfiguration: Behaviour<T>.() -> Unit) {
                configuredMethods[httpMethod] = Behaviour<T>().apply(behaviourConfiguration)
            }

            data class Behaviour<T : Entity<T>>(val isAuthenticated: Boolean = false, val authName: String? = null)

        }

    }

    data class HttpMethodAuth(val httpMethod: HttpMethod, val authName: String? = null)

}
