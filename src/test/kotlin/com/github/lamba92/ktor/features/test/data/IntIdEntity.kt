package com.github.lamba92.ktor.features.test.data

import me.liuwj.ktorm.entity.Entity

interface IntIdEntity : Entity<IntIdEntity> {
    val id: Int
    var value1: String
    var value2: Int
}