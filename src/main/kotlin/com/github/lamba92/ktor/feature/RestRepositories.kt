package com.github.lamba92.ktor.feature

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationFeature
import io.ktor.application.log
import io.ktor.http.HttpMethod
import io.ktor.http.HttpMethod.Companion.Put
import io.ktor.routing.Route
import io.ktor.routing.method
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineInterceptor
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.TransactionIsolation
import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.schema.Table
import org.slf4j.Logger
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

class RestRepositories private constructor(val configuration: Configuration) {

    companion object Feature : ApplicationFeature<Application, Configuration, RestRepositories> {

        const val entityIdTag = "entityIdTag"

        override val key = AttributeKey<RestRepositories>("RestRepositories")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): RestRepositories {
            val conf = Configuration(pipeline.log).apply(configure)
            assert(conf.repositoryPath.withoutWhitespaces.isNotBlank()) { "Repository path cannot be blank or empty" }
            val feature = RestRepositories(conf)

            conf.builtRoutes.forEach {
                pipeline.routing {
                    route(conf.repositoryPath, it)
                }
            }

            return feature
        }

    }

    class Configuration(val logger: Logger) {

        val entitiesConfigurationMap: MutableMap<Pair<String, HttpMethod>, Route.() -> Unit> = mutableMapOf()

        internal val builtRoutes
            get() = entitiesConfigurationMap.values.toList()

        var repositoryPath: String = "repositories"
            set(value) {
                assert(value.withoutWhitespaces.isNotBlank()) { "Repository path cannot be blank or empty" }
                field = value
            }

        val EntitySetup<out Entity<*>>.longestAuthName
            get() = configuredMethods.values
                .map { it.authName?.length ?: 7 }
                .maxBy { it } ?: 7

        inline fun <reified T : Entity<T>, reified K> registerEntity(
            table: Table<T>,
            database: Database,
            isolation: TransactionIsolation = TransactionIsolation.REPEATABLE_READ,
            httpMethodConf: EntitySetup<T>.() -> Unit
        ) {
            val logBuilder = StringBuilder()
            EntitySetup<T>(table::class.simpleName!!.toLowerCase())
                .apply(httpMethodConf)
                .apply {
                    assert(entityPath.withoutWhitespaces.isNotBlank()) { "${T::class.simpleName} path cannot be blank or empty" }
                    logBuilder.appendln("Building methods for entity ${T::class.simpleName}:")
                    configuredMethods.forEach { (httpMethod, behaviour) ->
                        if (httpMethod != Put) {
                            entitiesConfigurationMap[entityPath.withoutWhitespaces to httpMethod] =
                                DefaultBehaviours<T, K>(
                                    table,
                                    httpMethod,
                                    database,
                                    isolation,
                                    behaviour.restRepositoryInterceptor
                                ).toRoute(entityPath.withoutWhitespaces, httpMethod)
                            logBuilder.appendln(
                                "     - ${httpMethod.value.padEnd(7)} | Authentication realm: ${(if (behaviour.isAuthenticated) behaviour.authName
                                    ?: "Default" else "None").padEnd(longestAuthName)} | ${repositoryPath.withoutWhitespaces}/${entityPath.withoutWhitespaces}"
                            )
                        } else
                            logBuilder.appendln(
                                "     - ${httpMethod.value.padEnd(7)} | Authentication realm: ${(if (behaviour.isAuthenticated) behaviour.authName
                                    ?: "Default" else "None").padEnd(longestAuthName)} | ${repositoryPath.withoutWhitespaces}/${entityPath.withoutWhitespaces} | DISABLED DUE TO BUG"
                            )
                        logger.info(logBuilder.toString())
                    }
                }

        }

        data class InterceptorsContainer(
            val single: PipelineInterceptor<Unit, ApplicationCall>,
            val multiple: PipelineInterceptor<Unit, ApplicationCall>
        ) {
            fun toRoute(entityPath: String, httpMethod: HttpMethod): Route.() -> Unit = {
                route("$entityPath/$entityIdTag") {
                    method(httpMethod) { handle(single) }
                }
                route(entityPath) {
                    method(httpMethod) { handle(multiple) }
                }
            }
        }

        class EntitySetup<T : Entity<T>>(
            var entityPath: String
        ) {

            val configuredMethods = mutableMapOf<HttpMethod, Behaviour<T>>()

            fun addEndpoint(httpMethod: HttpMethod, behaviourConfiguration: Behaviour<T>.() -> Unit = {}) {
                configuredMethods[httpMethod] = Behaviour<T>()
                    .apply(behaviourConfiguration)
            }

            fun addEndpoints(vararg httpMethods: HttpMethod, behaviourConfiguration: Behaviour<T>.() -> Unit = {}) =
                httpMethods.forEach { addEndpoint(it, behaviourConfiguration) }

            data class Behaviour<T : Entity<T>>(
                var isAuthenticated: Boolean = false,
                var authName: String? = null,
                var restRepositoryInterceptor: RestRepositoryInterceptor<T> = { it }
            )

        }

    }

}