package com.github.lamba92.ktor.feature

import io.ktor.application.Application
import io.ktor.application.ApplicationFeature
import io.ktor.http.HttpMethod
import io.ktor.routing.method
import io.ktor.routing.routing
import io.ktor.util.AttributeKey
import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.schema.Table

class RestRepositories private constructor(val configuration: Configuration) {

    companion object Feature : ApplicationFeature<Application, Configuration, RestRepositories> {

        override val key = AttributeKey<RestRepositories>("RestRepositories")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): RestRepositories {

            val feature = RestRepositories(Configuration().apply(configure))

            pipeline.routing {
                feature.configuration.entitiesConfigurationMap
                    .flatten()
                    .forEach { (table, httpMethod, behaviour) ->
                        method(httpMethod) { DefaultDeserializationBehaviours(httpMethod, table.entityClass, table) }
                    }
            }

            return feature
        }
    }

    data class Configuration(val entitiesConfigurationMap: MutableMap<Table<out Entity<*>>, Map<HttpMethod, EntityHttpMethods.Behaviour<out Entity<*>>>> = mutableMapOf()) {

        inline fun <reified T : Entity<T>> registerEntity(
            table: Table<T>,
            httpMethodConf: EntityHttpMethods<T>.() -> Unit
        ) {
            entitiesConfigurationMap[table] = EntityHttpMethods<T>().apply(httpMethodConf).configuredMethods
        }

        class EntityHttpMethods<T : Entity<T>> {

            val configuredMethods = mutableMapOf<HttpMethod, Behaviour<T>>()

            fun addMethod(httpMethod: HttpMethod, behaviourConfiguration: Behaviour<T>.() -> Unit) {
                configuredMethods[httpMethod] = Behaviour<T>().apply(behaviourConfiguration)
            }

            data class Behaviour<T : Entity<T>>(val isAuthenticated: Boolean = false, val authName: String? = null)

        }

    }

    data class HttpMethodAuth(val httpMethod: HttpMethod, val authName: String? = null)

}
