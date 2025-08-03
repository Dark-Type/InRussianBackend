package com.inRussian.repositories

import com.inRussian.models.users.UserProfile
import com.inRussian.models.users.UserLanguageSkill
import com.inRussian.requests.users.UserLanguageSkillRequest
import com.inRussian.tables.UserProfiles
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import com.inRussian.tables.UserLanguageSkills
import java.util.*

interface UserProfileRepository {
    suspend fun findByUserId(userId: String): UserProfile?
    suspend fun create(profile: UserProfile): UserProfile
    suspend fun update(profile: UserProfile): UserProfile
    suspend fun deleteByUserId(userId: String): Boolean
    suspend fun addSkill(userId: String, skill: UserLanguageSkillRequest): Boolean
    suspend fun getSkills(userId: String): List<UserLanguageSkill>
    suspend fun updateSkill(skillId: String, userId: String, skill: UserLanguageSkillRequest): UserLanguageSkill?
    suspend fun deleteSkill(skillId: String, userId: String): Boolean
}

class ExposedUserProfileRepository : UserProfileRepository {

    private fun ResultRow.toUserProfile() = UserProfile(
        userId = this[UserProfiles.userId].toString(),
        surname = this[UserProfiles.surname],
        name = this[UserProfiles.name],
        patronymic = this[UserProfiles.patronymic],
        gender = this[UserProfiles.gender],
        dob = this[UserProfiles.dob].toString(),
        dor = this[UserProfiles.dor].toString(),
        citizenship = this[UserProfiles.citizenship],
        nationality = this[UserProfiles.nationality],
        countryOfResidence = this[UserProfiles.countryOfResidence],
        cityOfResidence = this[UserProfiles.cityOfResidence],
        countryDuringEducation = this[UserProfiles.countryDuringEducation],
        periodSpent = this[UserProfiles.periodSpent],
        kindOfActivity = this[UserProfiles.kindOfActivity],
        education = this[UserProfiles.education],
        purposeOfRegister = this[UserProfiles.purposeOfRegister]
    )

    private fun ResultRow.toUserLanguageSkill() = UserLanguageSkill(
        userId = this[UserLanguageSkills.userId].toString(),
        language = this[UserLanguageSkills.language],
        understands = this[UserLanguageSkills.understands],
        speaks = this[UserLanguageSkills.speaks],
        reads = this[UserLanguageSkills.reads],
        writes = this[UserLanguageSkills.writes]
    )


    override suspend fun findByUserId(userId: String): UserProfile? = transaction {
        UserProfiles.selectAll().where { UserProfiles.userId eq UUID.fromString(userId) }
            .map { it.toUserProfile() }
            .firstOrNull()
    }

    override suspend fun create(profile: UserProfile): UserProfile = transaction {
        UserProfiles.insert {
            it[userId] = UUID.fromString(profile.userId)
            it[surname] = profile.surname
            it[name] = profile.name
            it[patronymic] = profile.patronymic
            it[gender] = profile.gender
            it[dob] = java.time.LocalDate.parse(profile.dob)
            it[dor] = java.time.LocalDate.parse(profile.dor)
            it[citizenship] = profile.citizenship
            it[nationality] = profile.nationality
            it[countryOfResidence] = profile.countryOfResidence
            it[cityOfResidence] = profile.cityOfResidence
            it[countryDuringEducation] = profile.countryDuringEducation
            it[periodSpent] = profile.periodSpent
            it[kindOfActivity] = profile.kindOfActivity
            it[education] = profile.education
            it[purposeOfRegister] = profile.purposeOfRegister
        }
        profile
    }

    override suspend fun update(profile: UserProfile): UserProfile = transaction {
        UserProfiles.update({ UserProfiles.userId eq UUID.fromString(profile.userId) }) {
            it[surname] = profile.surname
            it[name] = profile.name
            it[patronymic] = profile.patronymic
            it[gender] = profile.gender
            it[dob] = java.time.LocalDate.parse(profile.dob)
            it[dor] = java.time.LocalDate.parse(profile.dor)
            it[citizenship] = profile.citizenship
            it[nationality] = profile.nationality
            it[countryOfResidence] = profile.countryOfResidence
            it[cityOfResidence] = profile.cityOfResidence
            it[countryDuringEducation] = profile.countryDuringEducation
            it[periodSpent] = profile.periodSpent
            it[kindOfActivity] = profile.kindOfActivity
            it[education] = profile.education
            it[purposeOfRegister] = profile.purposeOfRegister
        }
        profile
    }

    override suspend fun deleteByUserId(userId: String): Boolean = transaction {
        UserProfiles.deleteWhere { UserProfiles.userId eq UUID.fromString(userId) } > 0
    }

    override suspend fun addSkill(userId: String, skill: UserLanguageSkillRequest): Boolean = transaction {
        val result = UserLanguageSkills.insertIgnore {
            it[UserLanguageSkills.userId] = UUID.fromString(userId)
            it[language] = skill.language
            it[understands] = skill.understands
            it[speaks] = skill.speaks
            it[reads] = skill.reads
            it[writes] = skill.writes
        }
        result.resultedValues?.isNotEmpty() == true
    }

    override suspend fun getSkills(userId: String): List<UserLanguageSkill> = transaction {
        UserLanguageSkills.selectAll().where { UserLanguageSkills.userId eq UUID.fromString(userId) }
            .map { it.toUserLanguageSkill() }
    }

    override suspend fun updateSkill(
        skillId: String,
        userId: String,
        skill: UserLanguageSkillRequest
    ): UserLanguageSkill? = transaction {
        val updated = UserLanguageSkills.update({
            (UserLanguageSkills.userId eq UUID.fromString(userId)) and
                    (UserLanguageSkills.language eq skillId)
        }) {
            it[understands] = skill.understands
            it[speaks] = skill.speaks
            it[reads] = skill.reads
            it[writes] = skill.writes
        }

        if (updated > 0) {
            UserLanguageSkills.selectAll().where {
                (UserLanguageSkills.userId eq UUID.fromString(userId)) and
                        (UserLanguageSkills.language eq skillId)
            }.map { it.toUserLanguageSkill() }.firstOrNull()
        } else null
    }

    override suspend fun deleteSkill(skillId: String, userId: String): Boolean = transaction {
        UserLanguageSkills.deleteWhere {
            (UserLanguageSkills.userId eq UUID.fromString(userId)) and
                    (UserLanguageSkills.language eq skillId)
        } > 0
    }
}