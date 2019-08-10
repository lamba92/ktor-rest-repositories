package com.github.lamba92.ktor.features.test.data

import me.liuwj.ktorm.entity.Entity

interface DoubleIdEntity : Entity<DoubleIdEntity> {
    val id: Double
    var value1: String
    var value2: Int
}