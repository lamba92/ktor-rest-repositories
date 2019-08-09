package com.github.lamba92.ktor.feature

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.entity.findById
import me.liuwj.ktorm.schema.NestedBinding
import me.liuwj.ktorm.schema.Table

fun <A, B, C> Map<A, Map<B, C>>.flatten() = flatMap { (a, b) ->
    b.map { Triple(a, it.key, it.value) }
}

@JvmName("flatten3")
fun <A, B, C, D> List<Triple<A, B, Pair<C, D>>>.flatten() =
    map { Quadruple(it.first, it.second, it.third.first, it.third.second) }

@JvmName("flatten2")
fun <A, B, C, D> List<Triple<A, Pair<B, C>, D>>.flatten() =
    map { Quadruple(it.first, it.second.first, it.second.second, it.third) }

fun <A, B, C, D> List<Triple<Pair<A, B>, C, D>>.flatten() =
    map { Quadruple(it.first.first, it.first.second, it.second, it.third) }

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

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