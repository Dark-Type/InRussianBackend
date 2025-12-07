package com.inRussian.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table


object UserLanguageSkills : Table("user_language_skills") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val language = varchar("language", 50)
    val understands = bool("understands").default(false)
    val speaks = bool("speaks").default(false)
    val reads = bool("reads").default(false)
    val writes = bool("writes").default(false)

    override val primaryKey = PrimaryKey(userId, language, name = "pk_user_language_skills")
}