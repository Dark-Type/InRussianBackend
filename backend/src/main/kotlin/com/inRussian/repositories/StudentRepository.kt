package com.inRussian.repositories

import com.inRussian.config.dbQuery
import com.inRussian.models.content.*
import com.inRussian.models.progress.*
import com.inRussian.tables.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
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

@Serializable
data class UpdateTaskProgressRequest(
    val status: TaskStatus? = null,
    val attemptCount: Int? = null,
    val isCorrect: Boolean? = null,
    val shouldRetryAfterTasks: Int? = null
)


@Serializable
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

@Serializable
data class UserCourseEnrollmentItem(
    val userId: String,
    val courseId: String,
    val enrolledAt: String,
    val completedAt: String?,
    val progress: Double
)

@Serializable
data class SectionProgressItem(
    val sectionId: String,
    val totalTasks: Int,
    val completedTasks: Int,
    val progressPercentage: Double
)


interface StudentRepository {


    // UserCourseEnrollments CRUD
    suspend fun enrollInCourse(userId: String, courseId: String): Boolean
    suspend fun getUserEnrollments(userId: String): List<UserCourseEnrollmentItem>
    suspend fun unenrollFromCourse(userId: String, courseId: String): Boolean
    suspend fun getCourseEnrollment(userId: String, courseId: String): UserCourseEnrollmentItem?

    // Content Gets
    suspend fun getCoursesByUserLanguage(userId: String): List<Course>


}


class ExposedStudentRepository : StudentRepository {


    private fun ResultRow.toUserCourseEnrollment() = UserCourseEnrollmentItem(
        userId = this[UserCourseEnrollments.userId].toString(),
        courseId = this[UserCourseEnrollments.courseId].toString(),
        enrolledAt = this[UserCourseEnrollments.enrolledAt].toString(),
        completedAt = this[UserCourseEnrollments.completedAt]?.toString(),
        progress = this[UserCourseEnrollments.progress].toDouble()
    )


    private fun ResultRow.toCourse() = Course(
        id = this[Courses.id].toString(),
        name = this[Courses.name],
        description = this[Courses.description],
        authorId = this[Courses.authorId].toString(),
        authorUrl = this[Courses.authorUrl],
        language = this[Courses.language],
        isPublished = this[Courses.isPublished],
        createdAt = this[Courses.createdAt].toString(),
        updatedAt = this[Courses.updatedAt].toString(),
        posterId = this[Courses.posterId].toString()
    )

    // UserCourseEnrollments CRUD
    override suspend fun enrollInCourse(userId: String, courseId: String): Boolean = dbQuery {
        try {
            UserCourseEnrollments.insert {
                it[UserCourseEnrollments.userId] = UUID.fromString(userId)
                it[UserCourseEnrollments.courseId] = UUID.fromString(courseId)
                it[UserCourseEnrollments.progress] = BigDecimal.ZERO
            }
            true
        } catch (_: Exception) {
            false
        }
    }

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

    override suspend fun getCourseEnrollment(userId: String, courseId: String): UserCourseEnrollmentItem? =
        dbQuery {
            UserCourseEnrollments.selectAll()
                .where {
                    (UserCourseEnrollments.userId eq UUID.fromString(userId)) and
                            (UserCourseEnrollments.courseId eq UUID.fromString(courseId))
                }
                .singleOrNull()?.toUserCourseEnrollment()
        }

    // Content Gets
    override suspend fun getCoursesByUserLanguage(userId: String): List<Course> = dbQuery {
        val userLanguage = Users.selectAll()
            .where { Users.id eq UUID.fromString(userId) }
            .singleOrNull()?.get(Users.systemLanguage)?.name ?: return@dbQuery emptyList()

        Courses.selectAll()
            .where { (Courses.language eq userLanguage) and (Courses.isPublished eq true) }
            .orderBy(Courses.createdAt, SortOrder.DESC)
            .map { it.toCourse() }
    }


}