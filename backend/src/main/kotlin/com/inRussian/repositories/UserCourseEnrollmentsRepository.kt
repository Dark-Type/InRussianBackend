package com.inRussian.repositories


import com.inRussian.config.dbQuery
import com.inRussian.models.users.User
import com.inRussian.models.users.UserProfile
import com.inRussian.models.users.UserRole
import com.inRussian.tables.UserCourseEnrollments
import com.inRussian.tables.UserProfiles
import com.inRussian.tables.Users
import com.inRussian.tables.v2.TaskStatus
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.UUID

@kotlinx.serialization.Serializable
data class CreateTaskQueueRequest(
    val userId: String,
    val taskId: String,
    val themeId: String,
    val sectionId: String,
    val queuePosition: Int,
    val isOriginalTask: Boolean = true,
    val isRetryTask: Boolean = false,
    val originalTaskId: String? = null
)

@kotlinx.serialization.Serializable
data class UpdateTaskProgressRequest(
    val status: TaskStatus? = null,
    val attemptCount: Int? = null,
    val isCorrect: Boolean? = null,
    val shouldRetryAfterTasks: Int? = null
)


@kotlinx.serialization.Serializable
data class UserTaskProgressItem(
    val userId: String,
    val taskId: String,
    val status: TaskStatus,
    val attemptCount: Int,
    val isCorrect: Boolean?,
    val lastAttemptAt: String?,
    val completedAt: String?,
    val shouldRetryAfterTasks: Int?
)

@kotlinx.serialization.Serializable
data class UserCourseEnrollmentItem(
    val userId: String,
    val courseId: String,
    val enrolledAt: String,
    val completedAt: String?,
    val progress: Double
)

@kotlinx.serialization.Serializable
data class SectionProgressItem(
    val sectionId: String,
    val totalTasks: Int,
    val completedTasks: Int,
    val progressPercentage: Double
)

interface UserCourseEnrollmentsRepository {
    suspend fun enroll(userId: String, courseId: String): Boolean
    suspend fun complete(userId: String, courseId: String): Boolean
    suspend fun countByCourse(courseId: String): Long
    suspend fun listStudentsByCourse(courseId: String): List<User>
    suspend fun listStudentsWithProfiles(page: Int, size: Int): List<Pair<User, UserProfile?>>

    suspend fun getUserEnrollments(userId: String): List<UserCourseEnrollmentItem>
    suspend fun unenrollFromCourse(userId: String, courseId: String): Boolean
    suspend fun getCourseEnrollment(userId: String, courseId: String): UserCourseEnrollmentItem?
}

class ExposedUserCourseEnrollmentsRepository : UserCourseEnrollmentsRepository {
    override suspend fun enroll(userId: String, courseId: String): Boolean = dbQuery {
        val inserted = UserCourseEnrollments.insertIgnore {
            it[UserCourseEnrollments.userId] = UUID.fromString(userId)
            it[UserCourseEnrollments.courseId] = UUID.fromString(courseId)
        }.insertedCount
        inserted > 0
    }

    override suspend fun complete(userId: String, courseId: String): Boolean = dbQuery {
        UserCourseEnrollments.update({
            (UserCourseEnrollments.userId eq UUID.fromString(userId)) and
                    (UserCourseEnrollments.courseId eq UUID.fromString(courseId))
        }) {
            it[completedAt] = org.jetbrains.exposed.sql.javatime.CurrentTimestamp
            it[progress] = 100.00.toBigDecimal()
        } > 0
    }

    override suspend fun countByCourse(courseId: String): Long = dbQuery {
        UserCourseEnrollments.selectAll()
            .where { UserCourseEnrollments.courseId eq UUID.fromString(courseId) }
            .count()
    }

    override suspend fun listStudentsByCourse(courseId: String): List<User> = dbQuery {
        val ids = UserCourseEnrollments
            .selectAll()
            .where { UserCourseEnrollments.courseId eq UUID.fromString(courseId) }
            .map { it[UserCourseEnrollments.userId] }

        if (ids.isEmpty()) emptyList()
        else Users.selectAll().where { Users.id inList ids }.map { it.toUser() }
    }

    override suspend fun listStudentsWithProfiles(page: Int, size: Int): List<Pair<User, UserProfile?>> = dbQuery {
        val students = Users.selectAll()
            .where { Users.role eq UserRole.STUDENT }
            .limit(size).offset(start = ((page - 1) * size).toLong())
            .map { it.toUser() }

        val profiles = UserProfiles.selectAll()
            .where { UserProfiles.userId inList students.map { UUID.fromString(it.id) } }
            .associateBy({ it[UserProfiles.userId].toString() }, { it.toProfile() })

        students.map { it to profiles[it.id] }
    }

    // Перенесённые реализации:
    override suspend fun getUserEnrollments(userId: String): List<UserCourseEnrollmentItem> = dbQuery {
        UserCourseEnrollments.selectAll()
            .where { UserCourseEnrollments.userId eq UUID.fromString(userId) }
            .orderBy(UserCourseEnrollments.enrolledAt, SortOrder.DESC)
            .map { it.toUserCourseEnrollment() }
    }

    override suspend fun unenrollFromCourse(userId: String, courseId: String): Boolean = dbQuery {
        UserCourseEnrollments.deleteWhere {
            (UserCourseEnrollments.userId eq UUID.fromString(userId)) and
                    (UserCourseEnrollments.courseId eq UUID.fromString(courseId))
        } > 0
    }

    override suspend fun getCourseEnrollment(userId: String, courseId: String): UserCourseEnrollmentItem? = dbQuery {
        UserCourseEnrollments.selectAll()
            .where {
                (UserCourseEnrollments.userId eq UUID.fromString(userId)) and
                        (UserCourseEnrollments.courseId eq UUID.fromString(courseId))
            }
            .singleOrNull()
            ?.toUserCourseEnrollment()
    }

    private fun ResultRow.toUserCourseEnrollment() = UserCourseEnrollmentItem(
        userId = this[UserCourseEnrollments.userId].toString(),
        courseId = this[UserCourseEnrollments.courseId].toString(),
        enrolledAt = this[UserCourseEnrollments.enrolledAt].toString(),
        completedAt = this[UserCourseEnrollments.completedAt]?.toString(),
        progress = this[UserCourseEnrollments.progress].toDouble()
    )
}