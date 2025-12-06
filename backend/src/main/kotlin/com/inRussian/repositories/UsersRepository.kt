package com.inRussian.repositories

import com.inRussian.config.dbQuery
import com.inRussian.models.users.User
import com.inRussian.models.users.UserRole
import com.inRussian.models.users.UserStatus
import com.inRussian.requests.admin.UpdateUserRequest
import com.inRussian.tables.Users
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.insertAndGetId
import java.time.LocalDate
import java.util.*

interface UsersRepository {
    suspend fun findAll(
        page: Int,
        size: Int,
        role: UserRole?,
        createdFrom: LocalDate?,
        createdTo: LocalDate?,
        sortBy: UserSortBy,
        sortOrder: SortOrder
    ): List<User>

    suspend fun count(role: UserRole?, createdFrom: LocalDate?, createdTo: LocalDate?): Long
    suspend fun findById(id: String): User?
    suspend fun findByEmail(email: String): User?
    suspend fun create(user: User): User
    suspend fun update(id: String, request: UpdateUserRequest): User?
    suspend fun countRole(role: UserRole): Long

    suspend fun findAll(): List<User>
    suspend fun existsByRole(role: UserRole): Boolean
    suspend fun updateStatus(userId: String, status: UserStatus): Boolean
    suspend fun updateLastActivity(userId: String): Boolean
    suspend fun updatePassword(email: String, passwordHash: String): Boolean
}

class ExposedUsersRepository : UsersRepository {
    override suspend fun findAll(
        page: Int,
        size: Int,
        role: UserRole?,
        createdFrom: LocalDate?,
        createdTo: LocalDate?,
        sortBy: UserSortBy,
        sortOrder: SortOrder
    ): List<User> = dbQuery {
        val query = Users.selectAll()

        if (role != null) query.andWhere { Users.role eq role }
        if (createdFrom != null) query.andWhere { Users.createdAt.date() greaterEq createdFrom }
        if (createdTo != null) query.andWhere { Users.createdAt.date() lessEq createdTo }

        query.orderBy(sortBy.toColumn(), sortOrder)
            .limit(size).offset(start = ((page - 1) * size).toLong())
            .map { it.toUser() }
    }

    override suspend fun count(role: UserRole?, createdFrom: LocalDate?, createdTo: LocalDate?): Long =
        dbQuery {
            val query = Users.selectAll()
            if (role != null) query.andWhere { Users.role eq role }
            if (createdFrom != null) query.andWhere { Users.createdAt.date() greaterEq createdFrom }
            if (createdTo != null) query.andWhere { Users.createdAt.date() lessEq createdTo }
            query.count()
        }

    override suspend fun findById(id: String): User? = dbQuery {
        Users.selectAll()
            .where { Users.id eq UUID.fromString(id) }
            .singleOrNull()
            ?.toUser()
    }

    override suspend fun findByEmail(email: String): User? = dbQuery {
        Users.selectAll()
            .where { Users.email eq email }
            .singleOrNull()
            ?.toUser()
    }

    override suspend fun create(user: User): User = dbQuery {
        val id = Users.insertAndGetId {
            it[email] = user.email
            it[passwordHash] = user.passwordHash
            it[phone] = user.phone
            it[role] = user.role
            it[systemLanguage] = user.systemLanguage
            it[avatarId] = user.avatarId
            it[name] = user.name
            it[surname] = user.surname
            it[patronymic] = user.patronymic
            it[status] = user.status
            it[createdAt] = CurrentTimestamp
            it[updatedAt] = CurrentTimestamp
            it[lastActivityAt] = null
        }
        Users.selectAll().where { Users.id eq id.value }.single().toUser()
    }

    override suspend fun update(id: String, request: UpdateUserRequest): User? = dbQuery {
        val userUuid = UUID.fromString(id)
        Users.update({ Users.id eq userUuid }) {
            request.phone?.let { phone -> it[Users.phone] = phone }
            request.role?.let { role -> it[Users.role] = role }
            request.systemLanguage?.let { lang -> it[Users.systemLanguage] = lang }
            request.avatarId?.let { avatar -> it[Users.avatarId] = avatar }
            request.status.let { s -> it[Users.status] = s }
            it[Users.updatedAt] = CurrentTimestamp
            request.name.let { v -> it[Users.name] = v }
            request.surname.let { v -> it[Users.surname] = v }
            request.patronymic?.let { v -> it[Users.patronymic] = v }
        }
        Users.selectAll().where { Users.id eq userUuid }.singleOrNull()?.toUser()
    }

    override suspend fun countRole(role: UserRole): Long = dbQuery {
        Users.selectAll().where { Users.role eq role }.count()
    }

    // Новые/доп. реализации
    override suspend fun findAll(): List<User> = dbQuery {
        Users.selectAll().map { it.toUser() }
    }

    override suspend fun existsByRole(role: UserRole): Boolean = dbQuery {
        Users.selectAll().where { Users.role eq role }.count() > 0
    }

    override suspend fun updateStatus(userId: String, status: UserStatus): Boolean = dbQuery {
        Users.update({ Users.id eq UUID.fromString(userId) }) {
            it[Users.status] = status
            it[Users.updatedAt] = CurrentTimestamp
        } > 0
    }

    override suspend fun updateLastActivity(userId: String): Boolean = dbQuery {
        Users.update({ Users.id eq UUID.fromString(userId) }) {
            it[Users.lastActivityAt] = CurrentTimestamp
        } > 0
    }

    override suspend fun updatePassword(email: String, passwordHash: String): Boolean = dbQuery {
        Users.update({ Users.email eq email.lowercase() }) {
            it[this.passwordHash] = passwordHash
            it[updatedAt] = CurrentTimestamp
        } > 0
    }

    private fun UserSortBy.toColumn(): Column<*> = when (this) {
        UserSortBy.EMAIL -> Users.email
        UserSortBy.ROLE -> Users.role
        UserSortBy.STATUS -> Users.status
        UserSortBy.CREATED_AT -> Users.createdAt
        UserSortBy.LAST_ACTIVITY_AT -> Users.lastActivityAt
    }
}

fun ResultRow.toUser() = User(
    id = this[Users.id].toString(),
    email = this[Users.email],
    passwordHash = this[Users.passwordHash],
    phone = this[Users.phone],
    role = this[Users.role],
    systemLanguage = this[Users.systemLanguage],
    avatarId = this[Users.avatarId],
    status = this[Users.status],
    name = this[Users.name],
    surname = this[Users.surname],
    patronymic = this[Users.patronymic],
    lastActivityAt = this[Users.lastActivityAt]?.toString(),
    createdAt = this[Users.createdAt].toString(),
    updatedAt = this[Users.updatedAt].toString()
)

enum class UserSortBy { EMAIL, ROLE, STATUS, CREATED_AT, LAST_ACTIVITY_AT }