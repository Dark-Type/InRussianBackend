package com.inRussian.tables
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object TaskToTypes : Table("task_to_types") {
    val task = reference(
        name = "task_id",
        foreign = TaskEntity,
        onDelete = ReferenceOption.CASCADE
    )
    val typeName = reference("type_name", TaskTypes.name)
}