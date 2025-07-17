package com.inRussian.tables

import org.jetbrains.exposed.sql.Table

object StaffProfiles : Table("staff_profiles") {
    val userId = reference("user_id", Users)
    val name = varchar("name", 100)
    val surname = varchar("surname", 100)
    val patronymic = varchar("patronymic", 100).nullable()
    override val primaryKey = PrimaryKey(userId)
}