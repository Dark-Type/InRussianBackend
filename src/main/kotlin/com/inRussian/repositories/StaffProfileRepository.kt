package com.inRussian.repositories

import com.inRussian.models.users.StaffProfile
import com.inRussian.tables.StaffProfiles
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

import java.util.*

interface StaffProfileRepository {
    suspend fun findByUserId(userId: String): StaffProfile?
    suspend fun create(profile: StaffProfile): StaffProfile
    suspend fun update(profile: StaffProfile): StaffProfile
    suspend fun deleteByUserId(userId: String): Boolean
}

class ExposedStaffProfileRepository : StaffProfileRepository {

    private fun ResultRow.toStaffProfile() = StaffProfile(
        userId = this[StaffProfiles.userId].toString(),
        name = this[StaffProfiles.name],
        surname = this[StaffProfiles.surname],
        patronymic = this[StaffProfiles.patronymic]
    )

    override suspend fun findByUserId(userId: String): StaffProfile? = transaction {
        StaffProfiles.selectAll().where { StaffProfiles.userId eq UUID.fromString(userId) }
            .map { it.toStaffProfile() }
            .firstOrNull()
    }

    override suspend fun create(profile: StaffProfile): StaffProfile = transaction {
        StaffProfiles.insert {
            it[StaffProfiles.userId] = UUID.fromString(profile.userId)
            it[StaffProfiles.name] = profile.name
            it[StaffProfiles.surname] = profile.surname
            it[StaffProfiles.patronymic] = profile.patronymic
        }
        profile
    }

    override suspend fun update(profile: StaffProfile): StaffProfile = transaction {
        StaffProfiles.update({ StaffProfiles.userId eq UUID.fromString(profile.userId) }) {
            it[StaffProfiles.name] = profile.name
            it[StaffProfiles.surname] = profile.surname
            it[StaffProfiles.patronymic] = profile.patronymic
        }
        profile
    }

    override suspend fun deleteByUserId(userId: String): Boolean = transaction {
        StaffProfiles.deleteWhere { StaffProfiles.userId eq UUID.fromString(userId) } > 0
    }
}