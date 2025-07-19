package com.inRussian.repositories

import com.inRussian.models.users.User
import com.inRussian.models.users.UserRole
import com.inRussian.models.users.UserStatus
import com.inRussian.requests.admin.UpdateUserRequest
import com.inRussian.tables.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

interface AdminRepository {
    suspend fun getAllUsers(
        page: Int,
        size: Int,
        role: UserRole?,
        createdFrom: LocalDate?,
        createdTo: LocalDate?,
        sortBy: String,
        sortOrder: String
    ): List<User>
    suspend fun getUsersCount(role: UserRole?, createdFrom: LocalDate?, createdTo: LocalDate?): Long
    suspend fun updateUser(userId: String, request: UpdateUserRequest): User?
    suspend fun getStudentsCountByCourse(courseId: String): Long
    suspend fun getOverallStudentsCount(): Long
    suspend fun getCourseAverageTime(courseId: String): Long?
    suspend fun getCourseAverageProgress(courseId: String): BigDecimal?
    suspend fun getOverallAverageTime(): Long?
    suspend fun getOverallAverageProgress(): BigDecimal?
}

class ExposedAdminRepository(private val userRepository: UserRepository) : AdminRepository {

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

    override suspend fun getAllUsers(
        page: Int,
        size: Int,
        role: UserRole?,
        createdFrom: LocalDate?,
        createdTo: LocalDate?,
        sortBy: String,
        sortOrder: String
    ): List<User> = transaction {
        val query = Users.selectAll()

        if (role != null) {
            query.andWhere { Users.role eq role }
        }

        if (createdFrom != null) {
            query.andWhere { Users.createdAt.date() greaterEq createdFrom }
        }

        if (createdTo != null) {
            query.andWhere { Users.createdAt.date() lessEq createdTo }
        }

        val orderBy = when (sortBy) {
            "email" -> Users.email
            "role" -> Users.role
            "status" -> Users.status
            "createdAt" -> Users.createdAt
            "lastActivityAt" -> Users.lastActivityAt
            else -> Users.createdAt
        }

        if (sortOrder.lowercase() == "desc") {
            query.orderBy(orderBy, SortOrder.DESC)
        } else {
            query.orderBy(orderBy, SortOrder.ASC)
        }

        query.limit(size).offset(start = (page - 1) * size.toLong())
            .map { it.toUser() }
    }

    override suspend fun getUsersCount(role: UserRole?, createdFrom: LocalDate?, createdTo: LocalDate?): Long = transaction {
        val query = Users.selectAll()

        if (role != null) {
            query.andWhere { Users.role eq role }
        }

        if (createdFrom != null) {
            query.andWhere { Users.createdAt.date() greaterEq createdFrom }
        }

        if (createdTo != null) {
            query.andWhere { Users.createdAt.date() lessEq createdTo }
        }

        query.count()
    }

    override suspend fun updateUser(userId: String, request: UpdateUserRequest): User? = transaction {
        val userUuid = UUID.fromString(userId)

        Users.update({ Users.id eq userUuid }) {
            request.phone?.let { phone -> it[Users.phone] = phone }
            request.role?.let { role -> it[Users.role] = role }
            request.systemLanguage?.let { lang -> it[Users.systemLanguage] = lang }
            request.avatarId?.let { avatar -> it[Users.avatarId] = avatar }
            it[Users.status] = request.status
            it[Users.updatedAt] = CurrentTimestamp
        }

        Users.selectAll().where { Users.id eq userUuid }
            .singleOrNull()?.toUser()
    }

    override suspend fun getStudentsCountByCourse(courseId: String): Long = transaction {
        UserCourseEnrollments.selectAll()
            .where { UserCourseEnrollments.courseId eq UUID.fromString(courseId) }
            .count()
    }

    override suspend fun getOverallStudentsCount(): Long = transaction {
        Users.selectAll()
            .where { Users.role eq UserRole.STUDENT }
            .count()
    }

    override suspend fun getCourseAverageTime(courseId: String): Long? = transaction {
        UserCourseStatistics.selectAll()
            .where { UserCourseStatistics.courseId eq UUID.fromString(courseId) }
            .map { it[UserCourseStatistics.timeSpentSeconds] }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toLong()
    }

    override suspend fun getCourseAverageProgress(courseId: String): BigDecimal? = transaction {
        val progressList = UserCourseStatistics.selectAll()
            .where { UserCourseStatistics.courseId eq UUID.fromString(courseId) }
            .map { it[UserCourseStatistics.progressPercentage] }

        if (progressList.isEmpty()) {
            null
        } else {
            progressList.fold(BigDecimal.ZERO) { acc, progress -> acc + progress }
                .divide(BigDecimal(progressList.size), 2, java.math.RoundingMode.HALF_UP)
        }
    }

    override suspend fun getOverallAverageTime(): Long? = transaction {
        UserStatistics.selectAll()
            .map { it[UserStatistics.totalTimeSpentSeconds] }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toLong()
    }

    override suspend fun getOverallAverageProgress(): BigDecimal? = transaction {
        val userStats = UserStatistics.selectAll().map { row ->
            val totalTasks = row[UserStatistics.totalTasksAttempted]
            val completedTasks = row[UserStatistics.totalTasksCompleted]
            if (totalTasks > 0) {
                BigDecimal(completedTasks).divide(BigDecimal(totalTasks), 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal(100))
            } else {
                BigDecimal.ZERO
            }
        }

        if (userStats.isEmpty()) {
            null
        } else {
            userStats.fold(BigDecimal.ZERO) { acc, progress -> acc + progress }
                .divide(BigDecimal(userStats.size), 2, java.math.RoundingMode.HALF_UP)
        }
    }
}