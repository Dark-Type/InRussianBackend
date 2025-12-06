package com.inRussian.repositories

import com.inRussian.config.dbQuery
import com.inRussian.models.users.UserLanguageSkill
import com.inRussian.tables.UserLanguageSkills
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.UUID


interface UserLanguageSkillsRepository {
    suspend fun findByUser(userId: String): List<UserLanguageSkill>
    suspend fun upsert(skill: UserLanguageSkill): UserLanguageSkill
    suspend fun delete(userId: String, language: String): Boolean
}

class ExposedUserLanguageSkillsRepository : UserLanguageSkillsRepository {
    override suspend fun findByUser(userId: String): List<UserLanguageSkill> = dbQuery {
        UserLanguageSkills.selectAll()
            .where { UserLanguageSkills.userId eq UUID.fromString(userId) }
            .map { it.toSkill() }
    }

    override suspend fun upsert(skill: UserLanguageSkill): UserLanguageSkill = dbQuery {
        val userUuid = UUID.fromString(skill.userId)
        val exists = UserLanguageSkills.selectAll().where {
            (UserLanguageSkills.userId eq userUuid) and (UserLanguageSkills.language eq skill.language)
        }.any()

        if (exists) {
            UserLanguageSkills.update({
                (UserLanguageSkills.userId eq userUuid) and (UserLanguageSkills.language eq skill.language)
            }) {
                it[understands] = skill.understands
                it[speaks] = skill.speaks
                it[reads] = skill.reads
                it[writes] = skill.writes
            }
        } else {
            UserLanguageSkills.insert {
                it[userId] = userUuid
                it[language] = skill.language
                it[understands] = skill.understands
                it[speaks] = skill.speaks
                it[reads] = skill.reads
                it[writes] = skill.writes
            }
        }
        skill
    }

    override suspend fun delete(userId: String, language: String): Boolean = dbQuery {
        UserLanguageSkills.deleteWhere {
            (UserLanguageSkills.userId eq UUID.fromString(userId)) and
                    (UserLanguageSkills.language eq language)
        } > 0
    }
}

private fun ResultRow.toSkill() = UserLanguageSkill(
    userId = this[UserLanguageSkills.userId].toString(),
    language = this[UserLanguageSkills.language],
    understands = this[UserLanguageSkills.understands],
    speaks = this[UserLanguageSkills.speaks],
    reads = this[UserLanguageSkills.reads],
    writes = this[UserLanguageSkills.writes]
)