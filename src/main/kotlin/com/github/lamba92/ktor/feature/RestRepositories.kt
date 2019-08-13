package com.github.lamba92.ktor.feature

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationFeature
import io.ktor.application.log
import io.ktor.auth.authenticate
import io.ktor.http.HttpMethod
import io.ktor.routing.Routing
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineInterceptor
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.TransactionIsolation
import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.schema.Table
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

class RestRepositories private constructor(val configuration: Configuration) {

    companion object Feature : ApplicationFeature<Application, Configuration, RestRepositories> {

        override val key = AttributeKey<RestRepositories>("RestRepositories")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): RestRepositories {
            val conf = Configuration().apply(configure)
            assert(conf.cleansedPath.isNotBlank()) { "Repository path is blank or empty" }
            val feature = RestRepositories(conf)

            pipeline.routing {
                installRoutes(feature)
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
                    val finalEntityPath = entityData.entityPath.filter { !it.isWhitespace() }
                    assert(finalEntityPath.isNotBlank()) { "${table.entityClass?.simpleName} path is blank or empty" }
                    val path = "${feature.configuration.cleansedPath}/${entityData.entityPath}/{entityId}"
                    logBuilder.appendln(
                        "     - ${httpMethod.value.padEnd(7)} | Authentication realm: ${(if (behaviour.isAuthenticated) behaviour.authName
                            ?: "Default" else "None").padEnd(longestNamedAuthRealm)} | $path"
                    )
                    if (behaviour.isAuthenticated)
                        route(path) {
                            authenticate(behaviour.authName) {
                                //                                method(httpMethod) { handle { behaviour.singleItemAction!! }  }
                            }
                        }
                    else
                        route(path) {
                            //                            method(httpMethod) { handle(behaviour.singleItemAction!!) }
                        }
                }
                application.log.info(logBuilder.toString())
            }
        }

        private val Configuration.EntitySetup<*>.longestAuthName
            get() = configuredMethods.values
                .map { it.authName?.length ?: 7 }
                .maxBy { it } ?: 7

        private fun Routing.installRoutes2(feature: RestRepositories) {
            feature.configuration.entitiesConfigurationMap2.forEach { (entityPath, entityData) ->
                entityData
            }
        }

    }

    data class Configuration(
        val entitiesConfigurationMap: MutableMap<Table<out Entity<*>>, EntitySetup<out Entity<*>>> = mutableMapOf(),
        val entitiesConfigurationMap2: MutableMap<String, EntitySetup<out Entity<*>>> = mutableMapOf(),
        var path: String = "repositories"
    ) {

        val cleansedPath
            get() = path.filter { !it.isWhitespace() }

        inline fun <reified T : Entity<T>, reified K> registerEntity(
            table: Table<T>,
            database: Database,
            defaultBehaviourIsolation: TransactionIsolation = TransactionIsolation.REPEATABLE_READ,
            httpMethodConf: EntitySetup<T>.() -> Unit
        ) {
            val eSetup = EntitySetup<T>(table::class.simpleName!!.toLowerCase(), table)
                .apply(httpMethodConf)
                .apply {
                    configuredMethods.forEach { (httpMethod, behaviour) ->
                        if (behaviour.singleItemAction == null)
                            behaviour.singleItemAction =
                                DefaultBehaviours<T, K>(
                                    DefaultBehaviours.ItemType.SINGLE,
                                    table,
                                    httpMethod,
                                    database,
                                    defaultBehaviourIsolation
                                )
                    }
                }
            entitiesConfigurationMap[table] = eSetup
            entitiesConfigurationMap2[eSetup.entityPath] = eSetup
        }

        class EntitySetup<T : Entity<T>>(var entityPath: String, internal val table: Table<T>) {

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
                var singleItemAction: PipelineInterceptor<Unit, ApplicationCall>? = null,
                var multipleItemsAction: PipelineInterceptor<Unit, ApplicationCall>? = null
            )

        }

    }

}