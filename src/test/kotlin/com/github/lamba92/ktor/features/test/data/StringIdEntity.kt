package com.github.lamba92.ktor.features.test.data

import me.liuwj.ktorm.entity.Entity

interface StringIdEntity : Entity<StringIdEntity> {
    val id: String
    var value1: String
    var value2: Int
}