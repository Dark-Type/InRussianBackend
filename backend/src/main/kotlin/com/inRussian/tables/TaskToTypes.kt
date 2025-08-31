package com.inRussian.tables
import org.jetbrains.exposed.sql.Table

object TaskToTypes : Table("task_to_types") {
    val task = reference("task_id", TaskEntity)
    val typeName = reference("type_name", TaskTypes.name)
}