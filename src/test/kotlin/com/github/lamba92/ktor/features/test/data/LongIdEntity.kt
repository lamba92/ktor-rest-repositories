package com.github.lamba92.ktor.features.test.data

import me.liuwj.ktorm.entity.Entity

interface LongIdEntity : Entity<LongIdEntity> {
    val id: Long
    var value1: String
    var value2: Int
}