package com.inRussian.tables

import org.jetbrains.exposed.sql.Table


object TaskTypes : Table("task_types") {
    val name = varchar("name", 50).uniqueIndex()
}