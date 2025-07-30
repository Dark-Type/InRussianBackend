package com.inRussian.services

import com.inRussian.models.content.*
import com.inRussian.models.media.MediaFile
import com.inRussian.models.media.MediaFileMeta
import com.inRussian.models.tasks.*
import com.inRussian.repositories.ContentRepository
import com.inRussian.requests.content.*

import java.io.File
import java.util.Collections
import java.util.UUID

class ContentService(private val contentRepository: ContentRepository) {

    private val uploadDir: String = "uploads"
    val mediaStore: MutableMap<String, MediaFileMeta> = Collections.synchronizedMap(mutableMapOf())

    init {
        File(uploadDir).mkdirs()
    }

    suspend fun createTask(request: CreateTaskRequest): Result<TaskWithDetails> {
        return try {
            val task = contentRepository.createTask(request)
            Result.success(task)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTask(taskId: String): Result<TaskWithDetails> {
        return try {
            val task = contentRepository.getTask(taskId)
                ?: return Result.failure(Exception("Task not found"))
            Result.success(task)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTask(taskId: String, request: UpdateTaskRequest): Result<TaskWithDetails> {
        return try {
            val task = contentRepository.updateTask(taskId, request)
                ?: return Result.failure(Exception("Task not found"))
            Result.success(task)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: String): Result<Boolean> {
        return try {
            val deleted = contentRepository.deleteTask(taskId)
            if (deleted) {
                Result.success(true)
            } else {
                Result.failure(Exception("Task not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTasksByTheme(themeId: String): Result<List<TaskWithDetails>> {
        return try {
            val tasks = contentRepository.getTasksByTheme(themeId)
            Result.success(tasks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTaskContent(taskId: String, request: CreateTaskContentRequest): Result<TaskContentItem> {
        return try {
            val content = contentRepository.createTaskContent(taskId, request)
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTaskContent(contentId: String): Result<TaskContentItem> {
        return try {
            val content = contentRepository.getTaskContent(contentId)
                ?: return Result.failure(Exception("TaskContent not found"))
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTaskContent(contentId: String, request: UpdateTaskContentRequest): Result<TaskContentItem> {
        return try {
            val content = contentRepository.updateTaskContent(contentId, request)
                ?: return Result.failure(Exception("TaskContent not found"))
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTaskContent(contentId: String): Result<Boolean> {
        return try {
            val deleted = contentRepository.deleteTaskContent(contentId)
            if (deleted) {
                Result.success(true)
            } else {
                Result.failure(Exception("TaskContent not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTaskAnswer(taskId: String, request: CreateTaskAnswerRequest): Result<TaskAnswerItem> {
        return try {
            val answer = contentRepository.createTaskAnswer(taskId, request)
            Result.success(answer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTaskAnswer(taskId: String): Result<TaskAnswerItem> {
        return try {
            val answer = contentRepository.getTaskAnswer(taskId)
                ?: return Result.failure(Exception("TaskAnswer not found"))
            Result.success(answer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTaskAnswer(taskId: String, request: UpdateTaskAnswerRequest): Result<TaskAnswerItem> {
        return try {
            val answer = contentRepository.updateTaskAnswer(taskId, request)
                ?: return Result.failure(Exception("TaskAnswer not found"))
            Result.success(answer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTaskAnswer(taskId: String): Result<Boolean> {
        return try {
            val deleted = contentRepository.deleteTaskAnswer(taskId)
            if (deleted) {
                Result.success(true)
            } else {
                Result.failure(Exception("TaskAnswer not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTaskAnswerOption(
        taskId: String,
        request: CreateTaskAnswerOptionRequest
    ): Result<TaskAnswerOptionItem> {
        return try {
            val option = contentRepository.createTaskAnswerOption(taskId, request)
            Result.success(option)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTaskAnswerOption(optionId: String): Result<TaskAnswerOptionItem> {
        return try {
            val option = contentRepository.getTaskAnswerOption(optionId)
                ?: return Result.failure(Exception("TaskAnswerOption not found"))
            Result.success(option)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTaskAnswerOption(
        optionId: String,
        request: UpdateTaskAnswerOptionRequest
    ): Result<TaskAnswerOptionItem> {
        return try {
            val option = contentRepository.updateTaskAnswerOption(optionId, request)
                ?: return Result.failure(Exception("TaskAnswerOption not found"))
            Result.success(option)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTaskAnswerOption(optionId: String): Result<Boolean> {
        return try {
            val deleted = contentRepository.deleteTaskAnswerOption(optionId)
            if (deleted) {
                Result.success(true)
            } else {
                Result.failure(Exception("TaskAnswerOption not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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

    suspend fun getCourseTasksCount(courseId: String): Result<Long> {
        return try {
            val count = contentRepository.getCourseTasksCount(courseId)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSectionTasksCount(sectionId: String): Result<Long> {
        return try {
            val count = contentRepository.getSectionTasksCount(sectionId)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getThemeTasksCount(themeId: String): Result<Long> {
        return try {
            val count = contentRepository.getThemeTasksCount(themeId)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCountStats(): Result<CountStats> {
        return try {
            val stats = contentRepository.getCountStats()
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
