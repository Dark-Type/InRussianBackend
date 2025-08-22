package com.inRussian.tables

import org.jetbrains.exposed.sql.Table

object TaskToTypes : Table("task_to_types") {
    val task = reference("task_id", Tasks)
    val typeName = varchar("type_name", 50).references(TaskTypes.name)
    override val primaryKey = PrimaryKey(task, typeName, name = "pk_task_to_type")
}