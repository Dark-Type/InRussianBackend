package com.inRussian.repositories

import com.inRussian.config.dbQuery
import com.inRussian.models.users.UserProfile
import com.inRussian.tables.UserProfiles
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDate
import java.util.UUID

interface UserProfilesRepository {
    suspend fun findByUserId(userId: String): UserProfile?
    suspend fun create(profile: UserProfile): UserProfile
    suspend fun update(profile: UserProfile): UserProfile
    suspend fun deleteByUserId(userId: String): Boolean
}

class ExposedUserProfilesRepository : UserProfilesRepository {
    override suspend fun findByUserId(userId: String): UserProfile? = dbQuery {
        UserProfiles.selectAll()
            .where { UserProfiles.userId eq UUID.fromString(userId) }
            .singleOrNull()
            ?.toProfile()
    }

    override suspend fun create(profile: UserProfile): UserProfile = dbQuery {
        UserProfiles.insert {
            it[userId] = UUID.fromString(profile.userId)
            it[gender] = profile.gender
            it[dob] = LocalDate.parse(profile.dob)
            it[dor] = LocalDate.parse(profile.dor)
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

    override suspend fun update(profile: UserProfile): UserProfile = dbQuery {
        UserProfiles.update({ UserProfiles.userId eq UUID.fromString(profile.userId) }) {
            it[gender] = profile.gender
            it[dob] = LocalDate.parse(profile.dob)
            it[dor] = LocalDate.parse(profile.dor)
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

    override suspend fun deleteByUserId(userId: String): Boolean = dbQuery {
        UserProfiles.deleteWhere { UserProfiles.userId eq UUID.fromString(userId) } > 0
    }
}

fun ResultRow.toProfile() = UserProfile(
    userId = this[UserProfiles.userId].toString(),
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