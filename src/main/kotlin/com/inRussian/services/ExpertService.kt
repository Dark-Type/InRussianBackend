package com.inRussian.services

import com.inRussian.models.users.User
import com.inRussian.models.users.UserRole
import com.inRussian.models.users.UserProfile
import com.inRussian.models.content.*
import com.inRussian.models.tasks.*
import com.inRussian.repositories.AdminRepository
import com.inRussian.repositories.ContentRepository
import java.math.BigDecimal
import java.time.LocalDate

interface ExpertService {
    suspend fun getAllStudents(
        page: Int = 1,
        size: Int = 20,
        createdFrom: LocalDate? = null,
        createdTo: LocalDate? = null,
        sortBy: String = "createdAt",
        sortOrder: String = "desc"
    ): Result<List<User>>

    suspend fun getStudentsCount(createdFrom: LocalDate? = null, createdTo: LocalDate? = null): Result<Long>
    suspend fun getStudentsWithProfiles(page: Int = 1, size: Int = 20): Result<List<Pair<User, UserProfile?>>>
    suspend fun getStudentsByCourse(courseId: String): Result<List<User>>

    suspend fun getStudentsCountByCourse(courseId: String): Result<Long>
    suspend fun getOverallStudentsCount(): Result<Long>
    suspend fun getCourseAverageTime(courseId: String): Result<Long?>
    suspend fun getCourseAverageProgress(courseId: String): Result<BigDecimal?>
    suspend fun getOverallAverageTime(): Result<Long?>
    suspend fun getOverallAverageProgress(): Result<BigDecimal?>

    suspend fun getAllCourses(): Result<List<Course>>
    suspend fun getCourse(courseId: String): Result<Course?>
    suspend fun getSectionsByCourse(courseId: String): Result<List<Section>>
    suspend fun getSection(sectionId: String): Result<Section?>
    suspend fun getThemesBySection(sectionId: String): Result<List<Theme>>
    suspend fun getTheme(themeId: String): Result<Theme?>
    suspend fun getTasksByTheme(themeId: String): Result<List<TaskWithDetails>>
    suspend fun getTask(taskId: String): Result<TaskWithDetails?>
    suspend fun getTaskContent(contentId: String): Result<TaskContentItem?>
    suspend fun getTaskAnswer(taskId: String): Result<TaskAnswerItem?>
    suspend fun getTaskAnswerOption(optionId: String): Result<TaskAnswerOptionItem?>
    suspend fun getAllReports(): Result<List<Report>>
    suspend fun getReport(reportId: String): Result<Report?>
    suspend fun getCountStats(): Result<CountStats>
    suspend fun getCourseTasksCount(courseId: String): Result<Long>
    suspend fun getSectionTasksCount(sectionId: String): Result<Long>
    suspend fun getThemeTasksCount(themeId: String): Result<Long>
}

class ExpertServiceImpl(
    private val adminRepository: AdminRepository,
    private val contentRepository: ContentRepository
) : ExpertService {

    override suspend fun getAllStudents(
        page: Int,
        size: Int,
        createdFrom: LocalDate?,
        createdTo: LocalDate?,
        sortBy: String,
        sortOrder: String
    ): Result<List<User>> = try {
        val students = adminRepository.getAllUsers(
            page = page,
            size = size,
            role = UserRole.STUDENT,
            createdFrom = createdFrom,
            createdTo = createdTo,
            sortBy = sortBy,
            sortOrder = sortOrder
        )
        Result.success(students)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getStudentsCount(createdFrom: LocalDate?, createdTo: LocalDate?): Result<Long> = try {
        val count = adminRepository.getUsersCount(
            role = UserRole.STUDENT,
            createdFrom = createdFrom,
            createdTo = createdTo
        )
        Result.success(count)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getStudentsWithProfiles(page: Int, size: Int): Result<List<Pair<User, UserProfile?>>> = try {
        val studentsWithProfiles = adminRepository.getAllStudentsWithProfiles(page, size)
        Result.success(studentsWithProfiles)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getStudentsByCourse(courseId: String): Result<List<User>> = try {
        val students = adminRepository.getStudentsByCourseId(courseId)
        Result.success(students)
    } catch (e: Exception) {
        Result.failure(e)
    }
    override suspend fun getStudentsCountByCourse(courseId: String): Result<Long> = try {
        val count = adminRepository.getStudentsCountByCourse(courseId)
        Result.success(count)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getOverallStudentsCount(): Result<Long> = try {
        val count = adminRepository.getOverallStudentsCount()
        Result.success(count)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getCourseAverageTime(courseId: String): Result<Long?> = try {
        val time = adminRepository.getCourseAverageTime(courseId)
        Result.success(time)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getCourseAverageProgress(courseId: String): Result<BigDecimal?> = try {
        val progress = adminRepository.getCourseAverageProgress(courseId)
        Result.success(progress)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getOverallAverageTime(): Result<Long?> = try {
        val time = adminRepository.getOverallAverageTime()
        Result.success(time)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getOverallAverageProgress(): Result<BigDecimal?> = try {
        val progress = adminRepository.getOverallAverageProgress()
        Result.success(progress)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getAllCourses(): Result<List<Course>> = try {
        Result.success(contentRepository.getAllCourses())
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getCourse(courseId: String): Result<Course?> = try {
        Result.success(contentRepository.getCourse(courseId))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getSectionsByCourse(courseId: String): Result<List<Section>> = try {
        Result.success(contentRepository.getSectionsByCourse(courseId))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getSection(sectionId: String): Result<Section?> = try {
        Result.success(contentRepository.getSection(sectionId))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getThemesBySection(sectionId: String): Result<List<Theme>> = try {
        Result.success(contentRepository.getThemesBySection(sectionId))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getTheme(themeId: String): Result<Theme?> = try {
        Result.success(contentRepository.getTheme(themeId))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getTasksByTheme(themeId: String): Result<List<TaskWithDetails>> = try {
        Result.success(contentRepository.getTasksByTheme(themeId))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getTask(taskId: String): Result<TaskWithDetails?> = try {
        Result.success(contentRepository.getTask(taskId))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getTaskContent(contentId: String): Result<TaskContentItem?> = try {
        Result.success(contentRepository.getTaskContent(contentId))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getTaskAnswer(taskId: String): Result<TaskAnswerItem?> = try {
        Result.success(contentRepository.getTaskAnswer(taskId))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getTaskAnswerOption(optionId: String): Result<TaskAnswerOptionItem?> = try {
        Result.success(contentRepository.getTaskAnswerOption(optionId))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getAllReports(): Result<List<Report>> = try {
        Result.success(contentRepository.getAllReports())
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getReport(reportId: String): Result<Report?> = try {
        Result.success(contentRepository.getReport(reportId))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getCountStats(): Result<CountStats> = try {
        Result.success(contentRepository.getCountStats())
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getCourseTasksCount(courseId: String): Result<Long> = try {
        Result.success(contentRepository.getCourseTasksCount(courseId))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getSectionTasksCount(sectionId: String): Result<Long> = try {
        Result.success(contentRepository.getSectionTasksCount(sectionId))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getThemeTasksCount(themeId: String): Result<Long> = try {
        Result.success(contentRepository.getThemeTasksCount(themeId))
    } catch (e: Exception) {
        Result.failure(e)
    }


}