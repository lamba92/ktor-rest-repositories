package com.github.lamba92.ktor.feature

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.ApplicationCall
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.Transaction
import me.liuwj.ktorm.database.TransactionIsolation
import me.liuwj.ktorm.jackson.KtormModule

fun ContentNegotiation.Configuration.restRepositories(jacksonCustomization: ObjectMapper.() -> Unit = {}) =
    jackson {
        registerModule(KtormModule())
        jacksonCustomization()
    }

suspend fun <T> Database.useTransaction(
    dispatcher: CoroutineDispatcher, isolation: TransactionIsolation = TransactionIsolation.REPEATABLE_READ,
    func: (Transaction) -> T
) = withContext(dispatcher) { useTransaction(isolation, func) }

typealias RestRepositoryInterceptor<K> = PipelineContext<Unit, ApplicationCall>.(K) -> K

val String.withoutWhitespaces
    get() = filter { !it.isWhitespace() }
