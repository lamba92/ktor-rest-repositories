package com.github.lamba92.ktor.feature

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationFeature
import io.ktor.auth.authenticate
import io.ktor.http.HttpMethod
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.routing.Routing
import io.ktor.routing.method
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineInterceptor
import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.schema.Table
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

class RestRepositories private constructor(val configuration: Configuration) {

    companion object Feature : ApplicationFeature<Application, Configuration, RestRepositories> {

        override val key = AttributeKey<RestRepositories>("RestRepositories")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): RestRepositories {

            val feature = RestRepositories(Configuration().apply(configure))

            pipeline.routing {
                installRoutes(feature)
            }

            return feature
        }

        private fun Routing.installRoutes(feature: RestRepositories) {
            route(feature.configuration.path) {
                feature.configuration.entitiesConfiguration.forEach { entityData ->
                    route("${entityData.entityPath}/{entityId}") {
                        entityData.configuredMethods.forEach { (httpMethod, behaviour) ->
                            if (behaviour.isAuthenticated)
                                authenticate(behaviour.authName) {
                                    method(httpMethod) { handle(behaviour.action!!) }
                                }
                            else
                                method(httpMethod) { handle(behaviour.action!!) }
                        }
                    }
                }
            }
        }
    }

    data class Configuration(
        val entitiesConfigurationMap: MutableMap<Table<out Entity<*>>, EntityHttpMethods<out Entity<*>>> = mutableMapOf(),
        var path: String = "repositories"
    ) {

        val entitiesConfiguration
            get() = entitiesConfigurationMap.values.toList()

        inline fun <reified T : Entity<T>> registerEntity(
            table: Table<T>,
            entityPath: String = table::class.simpleName!!.toLowerCase(),
            httpMethodConf: EntityHttpMethods<T>.() -> Unit
        ) {
            entitiesConfigurationMap[table] = EntityHttpMethods<T>(entityPath)
                .apply(httpMethodConf)
                .apply {
                    configuredMethods.forEach { (httpMethod, behaviour) ->
                        if (behaviour.action == null)
                            behaviour.action = DefaultBehaviours(table, httpMethod)
                    }
                }
        }

        class EntityHttpMethods<T : Entity<T>>(var entityPath: String) {

            val configuredMethods = mutableMapOf<HttpMethod, Behaviour<T>>()

            fun addMethod(httpMethod: HttpMethod, behaviourConfiguration: Behaviour<T>.() -> Unit) {
                configuredMethods[httpMethod] = Behaviour<T>()
                    .apply(behaviourConfiguration)
            }

            data class Behaviour<T : Entity<T>>(
                var isAuthenticated: Boolean = false,
                var authName: String? = null,
                var action: PipelineInterceptor<Unit, ApplicationCall>? = null
            )

        }

    }

}

fun main() {
    RestRepositories.Configuration().apply {
        path = "m_repos"

        registerEntity(Users, "m_users") {
            addMethod(Get) {
                isAuthenticated = true

            }
        }

    }
}