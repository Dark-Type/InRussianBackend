package com.inRussian.repositories

import com.inRussian.models.users.User
import com.inRussian.models.users.UserRole
import com.inRussian.models.users.UserStatus
import com.inRussian.tables.Users
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update


import java.util.*

interface UserRepository {
    suspend fun findByEmail(email: String): User?
    suspend fun findById(id: String): User?
    suspend fun create(user: User): User
    suspend fun update(user: User): User
    suspend fun existsByRole(role: UserRole): Boolean
    suspend fun findAll(): List<User>
    suspend fun updateStatus(userId: String, status: UserStatus): Boolean
    suspend fun updateLastActivity(userId: String): Boolean
}

class ExposedUserRepository : UserRepository {

    private fun ResultRow.toUser() = User(
        id = this[Users.id].toString(),
        email = this[Users.email],
        passwordHash = this[Users.passwordHash],
        phone = this[Users.phone],
        role = this[Users.role],
        systemLanguage = this[Users.systemLanguage],
        avatarId = this[Users.avatarId],
        status = this[Users.status],
        lastActivityAt = this[Users.lastActivityAt]?.toString(),
        createdAt = this[Users.createdAt].toString(),
        updatedAt = this[Users.updatedAt].toString()
    )

    override suspend fun findByEmail(email: String): User? = transaction {
        Users.selectAll().where { Users.email eq email }
            .map { it.toUser() }
            .firstOrNull()
    }

    override suspend fun findById(id: String): User? = transaction {
        Users.selectAll().where { Users.id eq UUID.fromString(id) }
            .map { it.toUser() }
            .firstOrNull()
    }

    override suspend fun create(user: User): User = transaction {
        println("Type of user.status: ${user.status::class}, value: ${user.status}")
        Users.insert {
            it[id] = UUID.fromString(user.id)
            it[email] = user.email
            it[passwordHash] = user.passwordHash
            it[phone] = user.phone
            it[role] = user.role
            it[systemLanguage] = user.systemLanguage
            it[avatarId] = user.avatarId
            it[status] = user.status
        }
        user
    }

    override suspend fun update(user: User): User = transaction {
        Users.update({ Users.id eq UUID.fromString(user.id) }) {
            it[email] = user.email
            it[phone] = user.phone
            it[role] = user.role
            it[systemLanguage] = user.systemLanguage
            it[avatarId] = user.avatarId
            it[status] = user.status
            it[updatedAt] = java.time.Instant.now()
        }
        user
    }

    override suspend fun existsByRole(role: UserRole): Boolean = transaction {
        Users.selectAll().where { Users.role eq role }.count() > 0
    }

    override suspend fun findAll(): List<User> = transaction {
        Users.selectAll().map { it.toUser() }
    }

    override suspend fun updateStatus(userId: String, status: UserStatus): Boolean = transaction {
        Users.update({ Users.id eq UUID.fromString(userId) }) {
            it[Users.status] = status
            it[updatedAt] = java.time.Instant.now()
        } > 0
    }

    override suspend fun updateLastActivity(userId: String): Boolean = transaction {
        Users.update({ Users.id eq UUID.fromString(userId) }) {
            it[lastActivityAt] = java.time.Instant.now()
        } > 0
    }
}