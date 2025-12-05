package com.inRussian.services

import com.inRussian.models.content.*
import com.inRussian.models.tasks.*
import com.inRussian.repositories.ContentRepository
import com.inRussian.repositories.ImportReport
import com.inRussian.repositories.ThemeContents
import com.inRussian.repositories.ThemeTreeNode
import com.inRussian.requests.content.*
import java.io.File

class ContentService(private val contentRepository: ContentRepository) {

    private val uploadDir: String = "uploads"

    init {
        File(uploadDir).mkdirs()
    }

    // ---------- Themes ----------

    suspend fun createTheme(request: CreateThemeRequest): Result<Theme> = try {
        Result.success(contentRepository.createTheme(request))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getTheme(themeId: String): Result<Theme> = try {
        val theme = contentRepository.getTheme(themeId)
            ?: return Result.failure(Exception("Theme not found"))
        Result.success(theme)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateTheme(themeId: String, request: UpdateThemeRequest): Result<Theme> = try {
        val theme = contentRepository.updateTheme(themeId, request)
            ?: return Result.failure(Exception("Theme not found"))
        Result.success(theme)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteTheme(themeId: String): Result<Boolean> = try {
        val deleted = contentRepository.deleteTheme(themeId)
        if (deleted) Result.success(true) else Result.failure(Exception("Theme not found"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getThemesByCourse(courseId: String): Result<List<Theme>> = try {
        Result.success(contentRepository.getThemesByCourse(courseId))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getThemesByTheme(parentThemeId: String): Result<List<Theme>> = try {
        Result.success(contentRepository.getThemesByTheme(parentThemeId))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getTasksByTheme(themeId: String): Result<List<TaskModel>> = try {
        Result.success(contentRepository.getTasksByTheme(themeId))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getThemeContents(themeId: String): Result<ThemeContents> = try {
        val contents = contentRepository.getThemeContents(themeId)
            ?: return Result.failure(Exception("Theme not found"))
        Result.success(contents)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getThemeSubtree(themeId: String): Result<ThemeTreeNode> = try {
        val tree = contentRepository.getThemeSubtree(themeId)
            ?: return Result.failure(Exception("Theme not found"))
        Result.success(tree)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getCourseThemeTree(courseId: String): Result<List<ThemeTreeNode>> = try {
        Result.success(contentRepository.getCourseThemeTree(courseId))
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ---------- Courses ----------

    suspend fun createCourse(authorId: String, request: CreateCourseRequest): Result<Course> = try {
        Result.success(contentRepository.createCourse(authorId, request))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getCourse(courseId: String): Result<Course> = try {
        val course = contentRepository.getCourse(courseId)
            ?: return Result.failure(Exception("Course not found"))
        Result.success(course)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateCourse(courseId: String, request: UpdateCourseRequest): Result<Course> = try {
        val course = contentRepository.updateCourse(courseId, request)
            ?: return Result.failure(Exception("Course not found"))
        Result.success(course)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteCourse(courseId: String): Result<Boolean> = try {
        val deleted = contentRepository.deleteCourse(courseId)
        if (deleted) Result.success(true) else Result.failure(Exception("Course not found"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getAllCourses(): Result<List<Course>> = try {
        Result.success(contentRepository.getAllCourses())
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ---------- Reports ----------

    suspend fun createReport(reporterId: String, request: CreateReportRequest): Result<Report> = try {
        Result.success(contentRepository.createReport(reporterId, request))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getReport(reportId: String): Result<Report> = try {
        val report = contentRepository.getReport(reportId)
            ?: return Result.failure(Exception("Report not found"))
        Result.success(report)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteReport(reportId: String): Result<Boolean> = try {
        val deleted = contentRepository.deleteReport(reportId)
        if (deleted) Result.success(true) else Result.failure(Exception("Report not found"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getAllReports(): Result<List<Report>> = try {
        Result.success(contentRepository.getAllReports())
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ---------- Statistics ----------

    suspend fun getCourseTasksCount(courseId: String): Result<Long> = try {
        Result.success(contentRepository.getCourseTasksCount(courseId))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getThemeTasksCount(themeId: String): Result<Long> = try {
        Result.success(contentRepository.getThemeTasksCount(themeId))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getCountStats(): Result<CountStats> = try {
        Result.success(contentRepository.getCountStats())
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun exportCourseJson(courseId: String, sinceIsoUtc: String?): Result<String> = try {
        Result.success(contentRepository.exportCourseJson(courseId, sinceIsoUtc))
    } catch (e: Exception) { Result.failure(e) }
    suspend fun importCourseJson(
        json: String,
        targetCourseId: String?,
        createIfMissing: Boolean,
        languageOverride: String?,
        addOnly: Boolean
    ): Result<ImportReport> = try {
        Result.success(contentRepository.importCourseJson(json, targetCourseId, createIfMissing, languageOverride, addOnly))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun cloneCourseStructure(
        sourceCourseId: String,
        newLanguage: String,
        newCourseName: String
    ): Result<Course> = try {
        Result.success(contentRepository.cloneCourseStructure(sourceCourseId, newLanguage, newCourseName))
    } catch (e: Exception) { Result.failure(e) }

}