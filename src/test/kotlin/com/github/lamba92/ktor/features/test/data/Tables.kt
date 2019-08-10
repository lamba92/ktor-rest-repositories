package com.github.lamba92.ktor.features.test.data

import me.liuwj.ktorm.schema.*

object DoubleIdEntities : Table<DoubleIdEntity>("doubleIdEntities") {
    val id by double("id").primaryKey().bindTo { it.id }
    val value1 by varchar("value1").bindTo { it.value1 }
    val value2 by int("value2").bindTo { it.value2 }
}

object LongIdEntities : Table<LongIdEntity>("longIdEntities") {
    val id by long("id").primaryKey().bindTo { it.id }
    val value1 by varchar("value1").bindTo { it.value1 }
    val value2 by int("value2").bindTo { it.value2 }
}

object StringIdEntities : Table<StringIdEntity>("stringIdEntities") {
    val id by varchar("id").primaryKey().bindTo { it.id }
    val value1 by varchar("value1").bindTo { it.value1 }
    val value2 by int("value2").bindTo { it.value2 }
}