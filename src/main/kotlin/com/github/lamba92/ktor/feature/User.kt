package com.github.lamba92.ktor.feature

import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.schema.Table

interface User : Entity<User> {

}

object Users : Table<User>("t_users") {

}
