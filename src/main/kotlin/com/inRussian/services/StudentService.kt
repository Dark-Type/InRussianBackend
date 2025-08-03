package com.inRussian.services

import com.inRussian.models.badge.Badge
import com.inRussian.models.content.*
import com.inRussian.models.tasks.*
import com.inRussian.repositories.*

interface StudentService {
    suspend fun getCoursesByUserLanguage(userId: String): Result<List<Course>>
    suspend fun getSectionsByCourse(courseId: String): Result<List<Section>>
    suspend fun getThemesByCourse(courseId: String): Result<List<Theme>>
    suspend fun getTasksByTheme(themeId: String): Result<List<TaskWithDetails>>
    suspend fun getTaskVariants(taskId: String): Result<List<TaskAnswerOptionItem>>
    suspend fun getTaskContent(taskId: String): Result<List<TaskContentItem>>
    suspend fun getTaskQuery(userId: String, taskId: String): Result<TaskWithDetails?>
    suspend fun getUserBadges(userId: String): Result<List<Badge>>

    suspend fun createUserBadge(userId: String, badgeId: String, courseId: String?, themeId: String?): Result<Boolean>
    suspend fun deleteUserBadge(userId: String, badgeId: String): Result<Boolean>
    suspend fun addTaskToQueue(request: CreateTaskQueueRequest): Result<UserTaskQueueItem>
    suspend fun getTaskQueue(userId: String): Result<List<UserTaskQueueItem>>
    suspend fun updateTaskQueuePosition(queueId: String, newPosition: Int): Result<Boolean>
    suspend fun removeTaskFromQueue(queueId: String): Result<Boolean>
    suspend fun getNextTaskInQueue(userId: String): Result<UserTaskQueueItem?>
    suspend fun createTaskProgress(userId: String, taskId: String): Result<UserTaskProgressItem>
    suspend fun getTaskProgress(userId: String, taskId: String): Result<UserTaskProgressItem?>
    suspend fun updateTaskProgress(userId: String, taskId: String, request: UpdateTaskProgressRequest): Result<UserTaskProgressItem?>
    suspend fun markTaskAsCompleted(userId: String, taskId: String, isCorrect: Boolean): Result<UserTaskProgressItem?>
    suspend fun enrollInCourse(userId: String, courseId: String): Result<Boolean>
    suspend fun getUserEnrollments(userId: String): Result<List<UserCourseEnrollmentItem>>
    suspend fun unenrollFromCourse(userId: String, courseId: String): Result<Boolean>
    suspend fun getCourseEnrollment(userId: String, courseId: String): Result<UserCourseEnrollmentItem?>
    suspend fun getAllTasks(): Result<List<TaskWithDetails>>
    suspend fun getTask(taskId: String): Result<TaskWithDetails?>
    suspend fun getSectionProgress(userId: String, sectionId: String): Result<SectionProgressItem>
    suspend fun getCourseProgress(userId: String, courseId: String): Result<CourseProgressItem>
    suspend fun createReport(userId: String, taskId: String, description: String): Result<Report>
    suspend fun getTaskAnswer(taskId: String): Result<TaskAnswerItem?>
}

class StudentServiceImpl(
    private val repository: StudentRepository
) : StudentService {

    override suspend fun getCoursesByUserLanguage(userId: String) =
        runCatching { repository.getCoursesByUserLanguage(userId) }

    override suspend fun getSectionsByCourse(courseId: String) =
        runCatching { repository.getSectionsByCourse(courseId) }

    override suspend fun getThemesByCourse(courseId: String) =
        runCatching {
            val sections = repository.getSectionsByCourse(courseId)
            sections.flatMap { repository.getThemesBySection(it.id) }
        }

    override suspend fun getTasksByTheme(themeId: String) =
        runCatching { repository.getTasksByTheme(themeId) }

    override suspend fun getTaskVariants(taskId: String) =
        runCatching { repository.getTaskAnswerOptions(taskId) }

    override suspend fun getTaskContent(taskId: String) =
        runCatching { repository.getTaskContent(taskId) }

    override suspend fun getTaskQuery(userId: String, taskId: String) =
        runCatching { repository.getTask(taskId) }

    override suspend fun getUserBadges(userId: String) =
        runCatching { repository.getUserBadges(userId) }

    // Новые методы
    override suspend fun createUserBadge(userId: String, badgeId: String, courseId: String?, themeId: String?) =
        runCatching { repository.createUserBadge(userId, badgeId, courseId, themeId) }

    override suspend fun deleteUserBadge(userId: String, badgeId: String) =
        runCatching { repository.deleteUserBadge(userId, badgeId) }

    override suspend fun addTaskToQueue(request: CreateTaskQueueRequest) =
        runCatching { repository.addTaskToQueue(request) }

    override suspend fun getTaskQueue(userId: String) =
        runCatching { repository.getTaskQueue(userId) }

    override suspend fun updateTaskQueuePosition(queueId: String, newPosition: Int) =
        runCatching { repository.updateTaskQueuePosition(queueId, newPosition) }

    override suspend fun removeTaskFromQueue(queueId: String) =
        runCatching { repository.removeTaskFromQueue(queueId) }

    override suspend fun getNextTaskInQueue(userId: String) =
        runCatching { repository.getNextTaskInQueue(userId) }

    override suspend fun createTaskProgress(userId: String, taskId: String) =
        runCatching { repository.createTaskProgress(userId, taskId) }

    override suspend fun getTaskProgress(userId: String, taskId: String) =
        runCatching { repository.getTaskProgress(userId, taskId) }

    override suspend fun updateTaskProgress(userId: String, taskId: String, request: UpdateTaskProgressRequest) =
        runCatching { repository.updateTaskProgress(userId, taskId, request) }

    override suspend fun markTaskAsCompleted(userId: String, taskId: String, isCorrect: Boolean) =
        runCatching { repository.markTaskAsCompleted(userId, taskId, isCorrect) }

    override suspend fun enrollInCourse(userId: String, courseId: String) =
        runCatching { repository.enrollInCourse(userId, courseId) }

    override suspend fun getUserEnrollments(userId: String) =
        runCatching { repository.getUserEnrollments(userId) }

    override suspend fun unenrollFromCourse(userId: String, courseId: String) =
        runCatching { repository.unenrollFromCourse(userId, courseId) }

    override suspend fun getCourseEnrollment(userId: String, courseId: String) =
        runCatching { repository.getCourseEnrollment(userId, courseId) }

    override suspend fun getAllTasks() =
        runCatching { repository.getAllTasks() }

    override suspend fun getTask(taskId: String) =
        runCatching { repository.getTask(taskId) }

    override suspend fun getSectionProgress(userId: String, sectionId: String) =
        runCatching { repository.getSectionProgress(userId, sectionId) }

    override suspend fun getCourseProgress(userId: String, courseId: String) =
        runCatching { repository.getCourseProgress(userId, courseId) }

    override suspend fun createReport(userId: String, taskId: String, description: String) =
        runCatching { repository.createReport(userId, taskId, description) }

    override suspend fun getTaskAnswer(taskId: String) =
        runCatching { repository.getTaskAnswer(taskId) }
}