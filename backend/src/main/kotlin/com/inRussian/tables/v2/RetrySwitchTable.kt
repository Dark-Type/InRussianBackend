package com.inRussian.tables.v2

import org.jetbrains.exposed.sql.Table

object RetrySwitchTable : Table("retry_switch") {
    val id = integer("id").autoIncrement()
    val enabled = bool("enabled")
    override val primaryKey = PrimaryKey(id)
}