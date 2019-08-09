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
    private val behaviours = mapOf
}