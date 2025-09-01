package com.inRussian.services

import com.inRussian.models.content.*
import com.inRussian.models.tasks.*
import com.inRussian.repositories.ContentRepository
import com.inRussian.requests.content.*
import java.io.File

class ContentService(private val contentRepository: ContentRepository) {

    private val uploadDir: String = "uploads"
    init {
        File(uploadDir).mkdirs()
    }

    suspend fun createTheme(request: CreateThemeRequest): Result<Theme> {
        return try {
            val theme = contentRepository.createTheme(request)
            Result.success(theme)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTheme(themeId: String): Result<Theme> {
        return try {
            val theme = contentRepository.getTheme(themeId)
                ?: return Result.failure(Exception("Theme not found"))
            Result.success(theme)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTheme(themeId: String, request: UpdateThemeRequest): Result<Theme> {
        return try {
            val theme = contentRepository.updateTheme(themeId, request)
                ?: return Result.failure(Exception("Theme not found"))
            Result.success(theme)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTheme(themeId: String): Result<Boolean> {
        return try {
            val deleted = contentRepository.deleteTheme(themeId)
            if (deleted) {
                Result.success(true)
            } else {
                Result.failure(Exception("Theme not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getThemesBySection(sectionId: String): Result<List<Theme>> {
        return try {
            val themes = contentRepository.getThemesBySection(sectionId)
            Result.success(themes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createSection(request: CreateSectionRequest): Result<Section> {
        return try {
            val section = contentRepository.createSection(request)
            Result.success(section)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSection(sectionId: String): Result<Section> {
        return try {
            val section = contentRepository.getSection(sectionId)
                ?: return Result.failure(Exception("Section not found"))
            Result.success(section)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSection(sectionId: String, request: UpdateSectionRequest): Result<Section> {
        return try {
            val section = contentRepository.updateSection(sectionId, request)
                ?: return Result.failure(Exception("Section not found"))
            Result.success(section)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSection(sectionId: String): Result<Boolean> {
        return try {
            val deleted = contentRepository.deleteSection(sectionId)
            if (deleted) {
                Result.success(true)
            } else {
                Result.failure(Exception("Section not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSectionsByCourse(courseId: String): Result<List<Section>> {
        return try {
            val sections = contentRepository.getSectionsByCourse(courseId)
            Result.success(sections)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createCourse(authorId: String, request: CreateCourseRequest): Result<Course> {
        return try {
            val course = contentRepository.createCourse(authorId, request)
            Result.success(course)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCourse(courseId: String): Result<Course> {
        return try {
            val course = contentRepository.getCourse(courseId)
                ?: return Result.failure(Exception("Course not found"))
            Result.success(course)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCourse(courseId: String, request: UpdateCourseRequest): Result<Course> {
        return try {
            val course = contentRepository.updateCourse(courseId, request)
                ?: return Result.failure(Exception("Course not found"))
            Result.success(course)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCourse(courseId: String): Result<Boolean> {
        return try {
            val deleted = contentRepository.deleteCourse(courseId)
            if (deleted) {
                Result.success(true)
            } else {
                Result.failure(Exception("Course not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllCourses(): Result<List<Course>> {
        return try {
            val courses = contentRepository.getAllCourses()
            Result.success(courses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createReport(reporterId: String, request: CreateReportRequest): Result<Report> {
        return try {
            val report = contentRepository.createReport(reporterId, request)
            Result.success(report)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReport(reportId: String): Result<Report> {
        return try {
            val report = contentRepository.getReport(reportId)
                ?: return Result.failure(Exception("Report not found"))
            Result.success(report)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteReport(reportId: String): Result<Boolean> {
        return try {
            val deleted = contentRepository.deleteReport(reportId)
            if (deleted) {
                Result.success(true)
            } else {
                Result.failure(Exception("Report not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllReports(): Result<List<Report>> {
        return try {
            val reports = contentRepository.getAllReports()
            Result.success(reports)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCourseTasksCount(courseId: String): Result<Long> = try {
        Result.success(contentRepository.getCourseTasksCount(courseId))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getSectionTasksCount(sectionId: String): Result<Long> = try {
        Result.success(contentRepository.getSectionTasksCount(sectionId))
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

}
