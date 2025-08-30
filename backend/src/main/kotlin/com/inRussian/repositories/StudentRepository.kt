package com.inRussian.repositories

import com.inRussian.models.badge.Badge
import com.inRussian.tables.UserTaskProgress
import com.inRussian.tables.UserTaskQueue
import com.inRussian.models.content.*
import com.inRussian.models.progress.*
import com.inRussian.models.tasks.*
import com.inRussian.tables.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import java.math.BigDecimal
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import java.math.RoundingMode

interface StudentRepository {
    // UserBadges CRUD
    suspend fun createUserBadge(userId: String, badgeId: String, courseId: String?, themeId: String?): Boolean
    suspend fun getUserBadges(userId: String): List<Badge>
    suspend fun deleteUserBadge(userId: String, badgeId: String): Boolean

    // UserTaskQueue CRUD
    suspend fun addTaskToQueue(request: CreateTaskQueueRequest): UserTaskQueueItem
    suspend fun getTaskQueue(userId: String): List<UserTaskQueueItem>
    suspend fun updateTaskQueuePosition(queueId: String, newPosition: Int): Boolean
    suspend fun removeTaskFromQueue(queueId: String): Boolean
    suspend fun getNextTaskInQueue(userId: String): UserTaskQueueItem?

    // UserTaskProgress CRUD
    suspend fun createTaskProgress(userId: String, taskId: String): UserTaskProgressItem
    suspend fun getTaskProgress(userId: String, taskId: String): UserTaskProgressItem?
    suspend fun updateTaskProgress(userId: String, taskId: String, request: UpdateTaskProgressRequest): UserTaskProgressItem?
    suspend fun markTaskAsCompleted(userId: String, taskId: String, isCorrect: Boolean): UserTaskProgressItem?

    // UserCourseEnrollments CRUD
    suspend fun enrollInCourse(userId: String, courseId: String): Boolean
    suspend fun getUserEnrollments(userId: String): List<UserCourseEnrollmentItem>
    suspend fun unenrollFromCourse(userId: String, courseId: String): Boolean
    suspend fun getCourseEnrollment(userId: String, courseId: String): UserCourseEnrollmentItem?

    // Content Gets
    suspend fun getCoursesByUserLanguage(userId: String): List<Course>
    suspend fun getSectionsByCourse(courseId: String): List<Section>
    suspend fun getThemesBySection(sectionId: String): List<Theme>
    suspend fun getTasksByTheme(themeId: String): List<TaskWithDetails>
    suspend fun getAllTasks(): List<TaskWithDetails>
    suspend fun getTask(taskId: String): TaskWithDetails?

    // Progress calculations
    suspend fun getSectionProgress(userId: String, sectionId: String): SectionProgressItem
    suspend fun getCourseProgress(userId: String, courseId: String): CourseProgressItem


    // Reports
    suspend fun createReport(userId: String, taskId: String, description: String): Report

    suspend fun getTaskContent(taskId: String): List<TaskContentItem>
    suspend fun getTaskAnswer(taskId: String): TaskAnswerItem?
    suspend fun getTaskAnswerOptions(taskId: String): List<TaskAnswerOptionItem>

}

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

// Data classes for repository
data class UserTaskQueueItem(
    val id: String,
    val userId: String,
    val taskId: String,
    val themeId: String,
    val sectionId: String,
    val queuePosition: Int,
    val isOriginalTask: Boolean,
    val isRetryTask: Boolean,
    val originalTaskId: String?,
    val createdAt: String
)

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

data class UserCourseEnrollmentItem(
    val userId: String,
    val courseId: String,
    val enrolledAt: String,
    val completedAt: String?,
    val progress: BigDecimal
)

data class SectionProgressItem(
    val sectionId: String,
    val totalTasks: Int,
    val completedTasks: Int,
    val progressPercentage: BigDecimal
)

data class CourseProgressItem(
    val courseId: String,
    val totalTasks: Int,
    val completedTasks: Int,
    val progressPercentage: BigDecimal,
    val sectionsProgress: List<SectionProgressItem>
)

class ExposedStudentRepository : StudentRepository {

    private fun ResultRow.toUserTaskQueue() = UserTaskQueueItem(
        id = this[UserTaskQueue.id].toString(),
        userId = this[UserTaskQueue.userId].toString(),
        taskId = this[UserTaskQueue.taskId].toString(),
        themeId = this[UserTaskQueue.themeId].toString(),
        sectionId = this[UserTaskQueue.section].toString(),
        queuePosition = this[UserTaskQueue.queuePosition],
        isOriginalTask = this[UserTaskQueue.isOriginalTask],
        isRetryTask = this[UserTaskQueue.isRetryTask],
        originalTaskId = this[UserTaskQueue.originalTaskId]?.toString(),
        createdAt = this[UserTaskQueue.createdAt].toString()
    )

    private fun ResultRow.toUserTaskProgress() = UserTaskProgressItem(
        userId = this[UserTaskProgress.userId].toString(),
        taskId = this[UserTaskProgress.taskId].toString(),
        status = this[UserTaskProgress.status],
        attemptCount = this[UserTaskProgress.attemptCount],
        isCorrect = this[UserTaskProgress.isCorrect],
        lastAttemptAt = this[UserTaskProgress.lastAttemptAt]?.toString(),
        completedAt = this[UserTaskProgress.completedAt]?.toString(),
        shouldRetryAfterTasks = this[UserTaskProgress.shouldRetryAfterTasks]
    )

    private fun ResultRow.toUserCourseEnrollment() = UserCourseEnrollmentItem(
        userId = this[UserCourseEnrollments.userId].toString(),
        courseId = this[UserCourseEnrollments.courseId].toString(),
        enrolledAt = this[UserCourseEnrollments.enrolledAt].toString(),
        completedAt = this[UserCourseEnrollments.completedAt]?.toString(),
        progress = this[UserCourseEnrollments.progress]
    )

    private fun ResultRow.toBadge() = Badge(
        id = this[Badges.id].toString(),
        name = this[Badges.name],
        description = this[Badges.description],
        iconUrl = "/media/content/${this[Badges.imageId]}",
        badgeType = this[Badges.badgeType],
        criteria = this[Badges.criteria],
        createdAt = this[Badges.createdAt].toString()
    )

    private fun ResultRow.toTask() = TaskWithDetails(
        id = this[Tasks.id].toString(),
        themeId = this[Tasks.themeId].toString(),
        name = this[Tasks.name],
        taskType = this[Tasks.taskType],
        question = this[Tasks.question],
        instructions = this[Tasks.instructions],
        isTraining = this[Tasks.isTraining],
        orderNum = this[Tasks.orderNum],
        createdAt = this[Tasks.createdAt].toString()
    )

    private fun ResultRow.toSection() = Section(
        id = this[Sections.id].toString(),
        courseId = this[Sections.courseId].toString(),
        name = this[Sections.name],
        description = this[Sections.description],
        orderNum = this[Sections.orderNum],
        createdAt = this[Sections.createdAt].toString()
    )

    private fun ResultRow.toTheme() = Theme(
        id = this[Themes.id].toString(),
        sectionId = this[Themes.sectionId].toString(),
        name = this[Themes.name],
        description = this[Themes.description],
        orderNum = this[Themes.orderNum],
        createdAt = this[Themes.createdAt].toString()
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

    private fun ResultRow.toReport() = Report(
        id = this[Reports.id].toString(),
        description = this[Reports.description],
        taskId = this[Reports.taskId].toString(),
        reporterId = this[Reports.reporterId].toString(),
        createdAt = this[Reports.createdAt].toString()
    )

    // UserBadges CRUD
    override suspend fun createUserBadge(userId: String, badgeId: String, courseId: String?, themeId: String?): Boolean = transaction {
        try {
            UserBadges.insert {
                it[UserBadges.userId] = UUID.fromString(userId)
                it[UserBadges.badgeId] = UUID.fromString(badgeId)
                it[UserBadges.courseId] = courseId?.let { UUID.fromString(it) }
                it[UserBadges.themeId] = themeId?.let { UUID.fromString(it) }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun getUserBadges(userId: String): List<Badge> = transaction {
        (UserBadges innerJoin Badges)
            .selectAll()
            .where { UserBadges.userId eq UUID.fromString(userId) }
            .orderBy(UserBadges.earnedAt, SortOrder.DESC)
            .map { it.toBadge() }
    }

    override suspend fun deleteUserBadge(userId: String, badgeId: String): Boolean = transaction {
        UserBadges.deleteWhere {
            (UserBadges.userId eq UUID.fromString(userId)) and
                    (UserBadges.badgeId eq UUID.fromString(badgeId))
        } > 0
    }

    // UserTaskQueue CRUD
    override suspend fun addTaskToQueue(request: CreateTaskQueueRequest): UserTaskQueueItem = transaction {
        val queueId = UserTaskQueue.insertAndGetId {
            it[userId] = UUID.fromString(request.userId)
            it[taskId] = UUID.fromString(request.taskId)
            it[themeId] = UUID.fromString(request.themeId)
            it[section] = UUID.fromString(request.sectionId)
            it[queuePosition] = request.queuePosition
            it[isOriginalTask] = request.isOriginalTask
            it[isRetryTask] = request.isRetryTask
            it[originalTaskId] = request.originalTaskId?.let { UUID.fromString(it) }
        }
        UserTaskQueue.selectAll().where { UserTaskQueue.id eq queueId }.single().toUserTaskQueue()
    }

    override suspend fun getTaskQueue(userId: String): List<UserTaskQueueItem> = transaction {
        UserTaskQueue.selectAll()
            .where { UserTaskQueue.userId eq UUID.fromString(userId) }
            .orderBy(UserTaskQueue.queuePosition)
            .map { it.toUserTaskQueue() }
    }

    override suspend fun updateTaskQueuePosition(queueId: String, newPosition: Int): Boolean = transaction {
        UserTaskQueue.update({ UserTaskQueue.id eq UUID.fromString(queueId) }) {
            it[queuePosition] = newPosition
        } > 0
    }

    override suspend fun removeTaskFromQueue(queueId: String): Boolean = transaction {
        UserTaskQueue.deleteWhere { UserTaskQueue.id eq UUID.fromString(queueId) } > 0
    }

    override suspend fun getNextTaskInQueue(userId: String): UserTaskQueueItem? = transaction {
        UserTaskQueue.selectAll()
            .where { UserTaskQueue.userId eq UUID.fromString(userId) }
            .orderBy(UserTaskQueue.queuePosition)
            .limit(1)
            .singleOrNull()?.toUserTaskQueue()
    }

    // UserTaskProgress CRUD
    override suspend fun createTaskProgress(userId: String, taskId: String): UserTaskProgressItem = transaction {
        UserTaskProgress.insert {
            it[UserTaskProgress.userId] = UUID.fromString(userId)
            it[UserTaskProgress.taskId] = UUID.fromString(taskId)
            it[status] = TaskStatus.NOT_STARTED
            it[attemptCount] = 0
        }
        UserTaskProgress.selectAll()
            .where {
                (UserTaskProgress.userId eq UUID.fromString(userId)) and
                        (UserTaskProgress.taskId eq UUID.fromString(taskId))
            }
            .single().toUserTaskProgress()
    }

    override suspend fun getTaskProgress(userId: String, taskId: String): UserTaskProgressItem? = transaction {
        UserTaskProgress.selectAll()
            .where {
                (UserTaskProgress.userId eq UUID.fromString(userId)) and
                        (UserTaskProgress.taskId eq UUID.fromString(taskId))
            }
            .singleOrNull()?.toUserTaskProgress()
    }

    override suspend fun updateTaskProgress(
        userId: String,
        taskId: String,
        request: UpdateTaskProgressRequest
    ): UserTaskProgressItem? {
        transaction {
            UserTaskProgress.update({
                (UserTaskProgress.userId eq UUID.fromString(userId)) and
                        (UserTaskProgress.taskId eq UUID.fromString(taskId))
            }) {
                request.status?.let { status -> it[UserTaskProgress.status] = status }
                request.attemptCount?.let { count -> it[attemptCount] = count }
                request.isCorrect?.let { correct -> it[isCorrect] = correct }
                request.shouldRetryAfterTasks?.let { retry -> it[shouldRetryAfterTasks] = retry }
                it[lastAttemptAt] = CurrentTimestamp
            }
        }
        return getTaskProgress(userId, taskId)
    }

    override suspend fun markTaskAsCompleted(
        userId: String,
        taskId: String,
        isCorrect: Boolean
    ): UserTaskProgressItem? {
        transaction {
            UserTaskProgress.update({
                (UserTaskProgress.userId eq UUID.fromString(userId)) and
                        (UserTaskProgress.taskId eq UUID.fromString(taskId))
            }) {
                it[status] = TaskStatus.COMPLETED
                it[UserTaskProgress.isCorrect] = isCorrect
                it[completedAt] = CurrentTimestamp
                it[lastAttemptAt] = CurrentTimestamp
                it[attemptCount] = attemptCount + 1
            }
        }
        return getTaskProgress(userId, taskId)
    }
    // UserCourseEnrollments CRUD
    override suspend fun enrollInCourse(userId: String, courseId: String): Boolean = transaction {
        try {
            UserCourseEnrollments.insert {
                it[UserCourseEnrollments.userId] = UUID.fromString(userId)
                it[UserCourseEnrollments.courseId] = UUID.fromString(courseId)
                it[progress] = BigDecimal.ZERO
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun getUserEnrollments(userId: String): List<UserCourseEnrollmentItem> = transaction {
        UserCourseEnrollments.selectAll()
            .where { UserCourseEnrollments.userId eq UUID.fromString(userId) }
            .orderBy(UserCourseEnrollments.enrolledAt, SortOrder.DESC)
            .map { it.toUserCourseEnrollment() }
    }

    override suspend fun unenrollFromCourse(userId: String, courseId: String): Boolean = transaction {
        UserCourseEnrollments.deleteWhere {
            (UserCourseEnrollments.userId eq UUID.fromString(userId)) and
                    (UserCourseEnrollments.courseId eq UUID.fromString(courseId))
        } > 0
    }

    override suspend fun getCourseEnrollment(userId: String, courseId: String): UserCourseEnrollmentItem? = transaction {
        UserCourseEnrollments.selectAll()
            .where {
                (UserCourseEnrollments.userId eq UUID.fromString(userId)) and
                        (UserCourseEnrollments.courseId eq UUID.fromString(courseId))
            }
            .singleOrNull()?.toUserCourseEnrollment()
    }

    // Content Gets
    override suspend fun getCoursesByUserLanguage(userId: String): List<Course> = transaction {
        val userLanguage = Users.selectAll()
            .where { Users.id eq UUID.fromString(userId) }
            .singleOrNull()?.get(Users.systemLanguage)?.name ?: return@transaction emptyList()

        Courses.selectAll()
            .where { (Courses.language eq userLanguage) and (Courses.isPublished eq true) }
            .orderBy(Courses.createdAt, SortOrder.DESC)
            .map { it.toCourse() }
    }

    override suspend fun getSectionsByCourse(courseId: String): List<Section> = transaction {
        Sections.selectAll()
            .where { Sections.courseId eq UUID.fromString(courseId) }
            .orderBy(Sections.orderNum)
            .map { it.toSection() }
    }

    override suspend fun getThemesBySection(sectionId: String): List<Theme> = transaction {
        Themes.selectAll()
            .where { Themes.sectionId eq UUID.fromString(sectionId) }
            .orderBy(Themes.orderNum)
            .map { it.toTheme() }
    }

    override suspend fun getTasksByTheme(themeId: String): List<TaskWithDetails> = transaction {
        Tasks.selectAll()
            .where { Tasks.themeId eq UUID.fromString(themeId) }
            .orderBy(Tasks.orderNum)
            .map { it.toTask() }
    }

    override suspend fun getAllTasks(): List<TaskWithDetails> = transaction {
        Tasks.selectAll()
            .orderBy(Tasks.createdAt, SortOrder.DESC)
            .map { it.toTask() }
    }

    override suspend fun getTask(taskId: String): TaskWithDetails? = transaction {
        Tasks.selectAll()
            .where { Tasks.id eq UUID.fromString(taskId) }
            .singleOrNull()?.toTask()
    }

    // Progress calculations
    override suspend fun getSectionProgress(userId: String, sectionId: String): SectionProgressItem = transaction {
        val totalTasks = (Themes innerJoin Tasks)
            .selectAll()
            .where { Themes.sectionId eq UUID.fromString(sectionId) }
            .count().toInt()

        val completedTasks = (Themes innerJoin Tasks innerJoin UserTaskProgress)
            .selectAll()
            .where {
                (Themes.sectionId eq UUID.fromString(sectionId)) and
                        (UserTaskProgress.userId eq UUID.fromString(userId)) and
                        (UserTaskProgress.status eq TaskStatus.COMPLETED)
            }
            .count().toInt()

        val progressPercentage = if (totalTasks > 0) {
            BigDecimal(completedTasks).divide(BigDecimal(totalTasks), 2, RoundingMode.HALF_UP).multiply(BigDecimal(100))
        } else {
            BigDecimal.ZERO
        }

        SectionProgressItem(sectionId, totalTasks, completedTasks, progressPercentage)
    }

    override suspend fun getCourseProgress(userId: String, courseId: String): CourseProgressItem {
        val sections = getSectionsByCourse(courseId)
        val sectionsProgress = sections.map { section ->
            getSectionProgress(userId, section.id)
        }

        val totalTasks = sectionsProgress.sumOf { it.totalTasks }
        val completedTasks = sectionsProgress.sumOf { it.completedTasks }

        val progressPercentage = if (totalTasks > 0) {
            BigDecimal(completedTasks).divide(BigDecimal(totalTasks), 2, RoundingMode.HALF_UP).multiply(BigDecimal(100))
        } else {
            BigDecimal.ZERO
        }

        return CourseProgressItem(courseId, totalTasks, completedTasks, progressPercentage, sectionsProgress)
    }

    // Reports
    override suspend fun createReport(userId: String, taskId: String, description: String): Report = transaction {
        val reportId = Reports.insertAndGetId {
            it[Reports.description] = description
            it[Reports.taskId] = UUID.fromString(taskId)
            it[reporterId] = UUID.fromString(userId)
        }
        Reports.selectAll().where { Reports.id eq reportId }.single().toReport()
    }
    override suspend fun getTaskContent(taskId: String): List<TaskContentItem> = transaction {
        TaskContent.selectAll()
            .where { TaskContent.taskId eq UUID.fromString(taskId) }
            .orderBy(TaskContent.orderNum)
            .map {
                TaskContentItem(
                    id = it[TaskContent.id].toString(),
                    taskId = it[TaskContent.taskId].toString(),
                    contentType = it[TaskContent.contentType],
                    contentId = it[TaskContent.contentId],
                    description = it[TaskContent.description],
                    transcription = it[TaskContent.transcription],
                    translation = it[TaskContent.translation],
                    orderNum = it[TaskContent.orderNum]
                )
            }
    }

    override suspend fun getTaskAnswer(taskId: String): TaskAnswerItem? = transaction {
        TaskAnswers.selectAll()
            .where { TaskAnswers.taskId eq UUID.fromString(taskId) }
            .singleOrNull()
            ?.let {
                TaskAnswerItem(
                    taskId = it[TaskAnswers.taskId].toString(),
                    answerType = it[TaskAnswers.answerType],
                    correctAnswer = it[TaskAnswers.correctAnswer]
                )
            }
    }

    override suspend fun getTaskAnswerOptions(taskId: String): List<TaskAnswerOptionItem> = transaction {
        TaskAnswerOptions.selectAll()
            .where { TaskAnswerOptions.taskId eq UUID.fromString(taskId) }
            .orderBy(TaskAnswerOptions.orderNum)
            .map {
                TaskAnswerOptionItem(
                    id = it[TaskAnswerOptions.id].toString(),
                    taskId = it[TaskAnswerOptions.taskId].toString(),
                    optionText = it[TaskAnswerOptions.optionText],
                    optionAudioId = it[TaskAnswerOptions.optionAudioId],
                    isCorrect = it[TaskAnswerOptions.isCorrect],
                    orderNum = it[TaskAnswerOptions.orderNum]
                )
            }
    }
}