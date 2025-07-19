package com.inRussian.repositories

import com.inRussian.models.users.UserProfile
import com.inRussian.tables.UserProfiles
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

import java.util.*

interface UserProfileRepository {
    suspend fun findByUserId(userId: String): UserProfile?
    suspend fun create(profile: UserProfile): UserProfile
    suspend fun update(profile: UserProfile): UserProfile
    suspend fun deleteByUserId(userId: String): Boolean
}

class ExposedUserProfileRepository : UserProfileRepository {

    private fun ResultRow.toUserProfile() = UserProfile(
        userId = this[

            UserProfiles.userId].toString(),
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
}

