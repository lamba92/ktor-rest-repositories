package com.github.lamba92.ktor.feature

import io.ktor.application.*
import io.ktor.auth.authenticate
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpMethod
import io.ktor.jackson.jackson
import io.ktor.routing.Routing
import io.ktor.routing.method
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineInterceptor
import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.jackson.KtormModule
import me.liuwj.ktorm.schema.Table
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

class RestRepositories private constructor(val configuration: Configuration) {

    companion object Feature : ApplicationFeature<Application, Configuration, RestRepositories> {

        override val key = AttributeKey<RestRepositories>("RestRepositories")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): RestRepositories {

            val feature = RestRepositories(Configuration().apply(configure))

            with(pipeline) {
                install(ContentNegotiation) {
                    jackson {
                        registerModule(KtormModule())
                    }
                }
                routing {
                    installRoutes(feature)
                }
            }

            return feature
        }

        private fun Routing.installRoutes(feature: RestRepositories) {
            feature.configuration.entitiesConfigurationMap.forEach { (table, entityData) ->
                val logBuilder = StringBuilder()
                val longestNamedAuthRealm = entityData.configuredMethods.values
                    .map { it.authName?.length ?: 7 }
                    .maxBy { it } ?: 7
                logBuilder.appendln("Installing routes for ${table.entityClass?.simpleName}: ")
                entityData.configuredMethods.forEach { (httpMethod, behaviour) ->
                    val path = "${feature.configuration.path}/${entityData.entityPath}/{entityId}"
                    logBuilder.appendln(
                        "     - ${httpMethod.value.padEnd(7)} | Authentication realm: ${(if (behaviour.isAuthenticated) behaviour.authName
                            ?: "Default" else "None").padEnd(longestNamedAuthRealm)} | $path"
                    )
                    if (behaviour.isAuthenticated)
                        route(path) {
                            authenticate(behaviour.authName) {
                                method(httpMethod) { handle(behaviour.action!!) }
                            }
                        }
                    else
                        route(path) {
                            method(httpMethod) { handle(behaviour.action!!) }
                        }
                }
                application.log.info(logBuilder.toString())
            }
        }
    }

    data class Configuration(
        val entitiesConfigurationMap: MutableMap<Table<out Entity<*>>, EntitySetup<out Entity<*>>> = mutableMapOf(),
        var path: String = "repositories"
    ) {

        val entitiesConfiguration
            get() = entitiesConfigurationMap.values.toList()

        inline fun <reified T : Entity<T>> registerEntity(
            table: Table<T>,
            entityPath: String = table::class.simpleName!!.toLowerCase(),
            httpMethodConf: EntitySetup<T>.() -> Unit
        ) {
            entitiesConfigurationMap[table] = EntitySetup<T>(entityPath)
                .apply(httpMethodConf)
                .apply {
                    configuredMethods.forEach { (httpMethod, behaviour) ->
                        if (behaviour.action == null)
                            behaviour.action = DefaultBehaviour(table, httpMethod)
                    }
                }
        }

        class EntitySetup<T : Entity<T>>(var entityPath: String) {

            val configuredMethods = mutableMapOf<HttpMethod, Behaviour<T>>()

            fun addMethod(httpMethod: HttpMethod, behaviourConfiguration: Behaviour<T>.() -> Unit = {}) {
                configuredMethods[httpMethod] = Behaviour<T>()
                    .apply(behaviourConfiguration)
            }

            fun addMethods(vararg httpMethods: HttpMethod, behaviourConfiguration: Behaviour<T>.() -> Unit = {}) =
                httpMethods.forEach { addMethod(it, behaviourConfiguration) }

            data class Behaviour<T : Entity<T>>(
                var isAuthenticated: Boolean = false,
                var authName: String? = null,
                var action: PipelineInterceptor<Unit, ApplicationCall>? = null
            )

        }

    }

}