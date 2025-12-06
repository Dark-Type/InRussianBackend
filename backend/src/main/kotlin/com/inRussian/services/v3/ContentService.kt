package com.inRussian.services.v3

import com.inRussian.models.content.*
import com.inRussian.models.tasks.*
import com.inRussian.models.users.SystemLanguage
import com.inRussian.repositories.*
import com.inRussian.requests.content.*
import com.inRussian.utils.validation.FieldError
import com.inRussian.utils.validation.ValidationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID

@Serializable
data class ImportReport(
    val createdThemes: Int,
    val createdTasks: Int,
    val skippedThemes: Int,
    val skippedTasks: Int
)

@Serializable
data class CourseExport(
    val version: Int = 1,
    val exportedAt: String,
    val course: Course,
    val themes: List<ThemeTreeNode>
)

@Serializable
data class CloneCourseRequest(
    val newLanguage: String,
    val newCourseName: String? = null,
    val copyTasks: Boolean = true
)

class ContentService(
    private val themesRepository: ThemesRepository,
    private val coursesRepository: CoursesRepository,
    private val reportsRepository: ReportsRepository,
    private val tasksRepository: TasksRepository,
    private val contentStatsRepository: ContentStatsRepository
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    // ---------- Themes ----------

    suspend fun createTheme(request: CreateThemeRequest): Result<Theme> = runCatching {
        validateCreateThemeRequest(request)
        themesRepository.create(request)
    }

    suspend fun getTheme(themeId: String): Result<Theme> = runCatching {
        validateUuid(themeId, "themeId")
        themesRepository.findById(themeId)
            ?: throw NoSuchElementException("Theme not found: $themeId")
    }

    suspend fun updateTheme(themeId: String, request: UpdateThemeRequest): Result<Theme> = runCatching {
        validateUuid(themeId, "themeId")
        validateUpdateThemeRequest(request)
        themesRepository.update(themeId, request)
            ?: throw NoSuchElementException("Theme not found: $themeId")
    }

    suspend fun deleteTheme(themeId: String): Result<Boolean> = runCatching {
        validateUuid(themeId, "themeId")
        val deleted = themesRepository.deleteCascade(themeId)
        if (!deleted) throw NoSuchElementException("Theme not found: $themeId")
        true
    }

    suspend fun getThemesByCourse(courseId: String): Result<List<Theme>> = runCatching {
        validateUuid(courseId, "courseId")
        validateCourseExists(courseId)
        themesRepository.listRootByCourse(courseId)
    }

    suspend fun getThemesByTheme(parentThemeId: String): Result<List<Theme>> = runCatching {
        validateUuid(parentThemeId, "parentThemeId")
        themesRepository.listChildren(parentThemeId)
    }

    suspend fun getTasksByTheme(themeId: String): Result<List<TaskModel>> = runCatching {
        validateUuid(themeId, "themeId")
        tasksRepository.listByTheme(UUID.fromString(themeId))
    }

    suspend fun getThemeContents(themeId: String): Result<ThemeContents> = runCatching {
        validateUuid(themeId, "themeId")
        themesRepository.getContents(themeId)
            ?: throw NoSuchElementException("Theme not found: $themeId")
    }

    suspend fun getThemeSubtree(themeId: String): Result<ThemeTreeNode> = runCatching {
        validateUuid(themeId, "themeId")
        themesRepository.getSubtree(themeId)
            ?: throw NoSuchElementException("Theme not found: $themeId")
    }

    suspend fun getCourseThemeTree(courseId: String): Result<List<ThemeTreeNode>> = runCatching {
        validateUuid(courseId, "courseId")
        validateCourseExists(courseId)
        themesRepository.getCourseTree(courseId)
    }

    // ---------- Courses ----------

    suspend fun createCourse(authorId: String, request: CreateCourseRequest): Result<Course> = runCatching {
        validateUuid(authorId, "authorId")
        validateCreateCourseRequest(request)
        coursesRepository.create(authorId, request)
    }

    suspend fun getCourse(courseId: String): Result<Course> = runCatching {
        validateUuid(courseId, "courseId")
        coursesRepository.findById(courseId)
            ?: throw NoSuchElementException("Course not found: $courseId")
    }

    suspend fun updateCourse(courseId: String, request: UpdateCourseRequest): Result<Course> = runCatching {
        validateUuid(courseId, "courseId")
        validateUpdateCourseRequest(request)
        coursesRepository.update(courseId, request)
            ?: throw NoSuchElementException("Course not found: $courseId")
    }

    suspend fun deleteCourse(courseId: String): Result<Boolean> = runCatching {
        validateUuid(courseId, "courseId")
        val deleted = coursesRepository.delete(courseId)
        if (!deleted) throw NoSuchElementException("Course not found: $courseId")
        true
    }

    suspend fun getAllCourses(): Result<List<Course>> = runCatching {
        coursesRepository.listAll()
    }

    suspend fun getCourseWithTree(courseId: String): Result<Pair<Course, List<ThemeTreeNode>>> = runCatching {
        validateUuid(courseId, "courseId")
        val course = coursesRepository.findById(courseId)
            ?: throw NoSuchElementException("Course not found: $courseId")
        val tree = themesRepository.getCourseTree(courseId)
        course to tree
    }

    // ---------- Reports ----------

    suspend fun createReport(reporterId: String, request: CreateReportRequest): Result<Report> = runCatching {
        validateUuid(reporterId, "reporterId")
        validateCreateReportRequest(request)
        reportsRepository.create(reporterId, request)
    }

    suspend fun getReport(reportId: String): Result<Report> = runCatching {
        validateUuid(reportId, "reportId")
        reportsRepository.findById(reportId)
            ?: throw NoSuchElementException("Report not found: $reportId")
    }

    suspend fun deleteReport(reportId: String): Result<Boolean> = runCatching {
        validateUuid(reportId, "reportId")
        val deleted = reportsRepository.delete(reportId)
        if (!deleted) throw NoSuchElementException("Report not found: $reportId")
        true
    }

    suspend fun getAllReports(): Result<List<Report>> = runCatching {
        reportsRepository.listAll()
    }

    // ---------- Statistics ----------

    suspend fun getCourseTasksCount(courseId: String): Result<Long> = runCatching {
        validateUuid(courseId, "courseId")
        validateCourseExists(courseId)
        contentStatsRepository.courseTasksCount(courseId)
    }

    suspend fun getThemeTasksCount(themeId: String): Result<Long> = runCatching {
        validateUuid(themeId, "themeId")
        contentStatsRepository.themeTasksCount(themeId)
    }

    suspend fun getCountStats(): Result<CountStats> = runCatching {
        contentStatsRepository.countStats()
    }

    // ---------- Export ----------

    suspend fun exportCourseJson(courseId: String): Result<String> = runCatching {
        validateUuid(courseId, "courseId")
        val course = coursesRepository.findById(courseId)
            ?: throw NoSuchElementException("Course not found: $courseId")
        val themes = themesRepository.getCourseTree(courseId)
        val export = CourseExport(
            version = 1,
            exportedAt = Instant.now().toString(),
            course = course,
            themes = themes
        )
        json.encodeToString(export)
    }

    // ---------- Clone Course ----------

    suspend fun cloneCourse(
        sourceCourseId: String,
        authorId: String,
        request: CloneCourseRequest
    ): Result<Course> = runCatching {
        validateUuid(sourceCourseId, "sourceCourseId")
        validateUuid(authorId, "authorId")
        validateCloneCourseRequest(request)

        val sourceCourse = coursesRepository.findById(sourceCourseId)
            ?: throw NoSuchElementException("Source course not found: $sourceCourseId")

        val newCourseName = request.newCourseName
            ?: "${sourceCourse.name} (${request.newLanguage.uppercase()})"

        val newCourse = coursesRepository.create(
            authorId,
            CreateCourseRequest(
                name = newCourseName,
                description = sourceCourse.description,
                authorUrl = sourceCourse.authorUrl,
                language = request.newLanguage,
                isPublished = false,
                coursePoster = sourceCourse.posterId
            )
        )

        val sourceTree = themesRepository.getCourseTree(sourceCourseId)
        cloneThemeTree(sourceTree, newCourse.id, null, request.copyTasks)

        newCourse
    }

    private suspend fun cloneThemeTree(
        nodes: List<ThemeTreeNode>,
        newCourseId: String,
        parentThemeId: String?,
        copyTasks: Boolean
    ) {
        for (node in nodes) {
            val newTheme = themesRepository.create(
                CreateThemeRequest(
                    courseId = newCourseId,
                    parentThemeId = parentThemeId,
                    name = node.theme.name,
                    description = node.theme.description,
                    position = node.theme.position
                )
            )

            if (copyTasks) {
                val tasks = tasksRepository.listByTheme(UUID.fromString(node.theme.id))
                for (task in tasks) {
                    tasksRepository.cloneTask(UUID.fromString(task.id), UUID.fromString(newTheme.id))
                }
            }

            if (node.children.isNotEmpty()) {
                cloneThemeTree(node.children, newCourseId, newTheme.id, copyTasks)
            }
        }
    }

    suspend fun courseExists(courseId: String): Result<Boolean> = runCatching {
        validateUuid(courseId, "courseId")
        coursesRepository.findById(courseId) != null
    }

    // ---------- Validation Helpers ----------

    private fun validateUuid(value: String, fieldName: String) {
        try {
            UUID.fromString(value)
        } catch (e: IllegalArgumentException) {
            throw ValidationException(
                listOf(FieldError(fieldName, "invalid_uuid", "Invalid UUID format: $value"))
            )
        }
    }

    private suspend fun validateCourseExists(courseId: String) {
        if (coursesRepository.findById(courseId) == null) {
            throw NoSuchElementException("Course not found: $courseId")
        }
    }

    private fun validateCreateThemeRequest(request: CreateThemeRequest) {
        val errors = mutableListOf<FieldError>()

        if (request.name.isBlank()) {
            errors.add(FieldError("name", "required", "Theme name is required"))
        } else if (request.name.length > 255) {
            errors.add(FieldError("name", "max_length", "Theme name must not exceed 255 characters"))
        }

        if (request.courseId.isNullOrBlank()) {
            errors.add(FieldError("courseId", "required", "Course ID is required"))
        } else {
            try {
                UUID.fromString(request.courseId)
            } catch (e: IllegalArgumentException) {
                errors.add(FieldError("courseId", "invalid_uuid", "Invalid course ID format"))
            }
        }

        request.parentThemeId?.let { pid ->
            try {
                UUID.fromString(pid)
            } catch (e: IllegalArgumentException) {
                errors.add(FieldError("parentThemeId", "invalid_uuid", "Invalid parent theme ID format"))
            }
        }

        request.position?.let { pos ->
            if (pos < 0) {
                errors.add(FieldError("position", "invalid_value", "Position must be non-negative"))
            }
        }

        if (errors.isNotEmpty()) throw ValidationException(errors)
    }

    private fun validateUpdateThemeRequest(request: UpdateThemeRequest) {
        val errors = mutableListOf<FieldError>()

        request.name?.let { name ->
            if (name.isBlank()) {
                errors.add(FieldError("name", "invalid_value", "Theme name cannot be blank"))
            } else if (name.length > 255) {
                errors.add(FieldError("name", "max_length", "Theme name must not exceed 255 characters"))
            }
        }

        request.parentThemeId?.let { pid ->
            try {
                UUID.fromString(pid)
            } catch (e: IllegalArgumentException) {
                errors.add(FieldError("parentThemeId", "invalid_uuid", "Invalid parent theme ID format"))
            }
        }

        request.position?.let { pos ->
            if (pos < 0) {
                errors.add(FieldError("position", "invalid_value", "Position must be non-negative"))
            }
        }

        if (errors.isNotEmpty()) throw ValidationException(errors)
    }

    private fun validateCreateCourseRequest(request: CreateCourseRequest) {
        val errors = mutableListOf<FieldError>()

        if (request.name.isBlank()) {
            errors.add(FieldError("name", "required", "Course name is required"))
        } else if (request.name.length > 255) {
            errors.add(FieldError("name", "max_length", "Course name must not exceed 255 characters"))
        }

        if (request.language.isBlank()) {
            errors.add(FieldError("language", "required", "Language is required"))
        } else {
            try {
                SystemLanguage.valueOf(request.language.uppercase())
            } catch (e: IllegalArgumentException) {
                val allowed = SystemLanguage.values()
                    .joinToString(", ") { it.name.lowercase() }
                errors.add(
                    FieldError(
                        "language",
                        "invalid_value",
                        "Unsupported language. Allowed: $allowed"
                    )
                )
            }
        }

        request.coursePoster?.let { pid ->
            try {
                UUID.fromString(pid)
            } catch (e: IllegalArgumentException) {
                errors.add(FieldError("coursePoster", "invalid_uuid", "Invalid poster ID format"))
            }
        }

        if (errors.isNotEmpty()) throw ValidationException(errors)
    }

    private fun validateUpdateCourseRequest(request: UpdateCourseRequest) {
        val errors = mutableListOf<FieldError>()

        request.name?.let { name ->
            if (name.isBlank()) {
                errors.add(FieldError("name", "invalid_value", "Course name cannot be blank"))
            } else if (name.length > 255) {
                errors.add(FieldError("name", "max_length", "Course name must not exceed 255 characters"))
            }
        }

        request.language?.let { lang ->
            try {
                SystemLanguage.valueOf(lang.uppercase())
            } catch (e: IllegalArgumentException) {
                val allowed = SystemLanguage.values()
                    .joinToString(", ") { it.name.lowercase() }
                errors.add(
                    FieldError(
                        "language",
                        "invalid_value",
                        "Unsupported language. Allowed: $allowed"
                    )
                )
            }
        }

        if (errors.isNotEmpty()) throw ValidationException(errors)
    }

    private fun validateCreateReportRequest(request: CreateReportRequest) {
        val errors = mutableListOf<FieldError>()

        if (request.description.isBlank()) {
            errors.add(FieldError("description", "required", "Report description is required"))
        } else if (request.description.length > 2000) {
            errors.add(FieldError("description", "max_length", "Description must not exceed 2000 characters"))
        }

        try {
            UUID.fromString(request.taskId)
        } catch (e: IllegalArgumentException) {
            errors.add(FieldError("taskId", "invalid_uuid", "Invalid task ID format"))
        }

        if (errors.isNotEmpty()) throw ValidationException(errors)
    }

    private fun validateCloneCourseRequest(request: CloneCourseRequest) {
        val errors = mutableListOf<FieldError>()

        if (request.newLanguage.isBlank()) {
            errors.add(FieldError("newLanguage", "required", "New language is required"))
        } else {
            try {
                SystemLanguage.valueOf(request.newLanguage.uppercase())
            } catch (e: IllegalArgumentException) {
                val allowed = SystemLanguage.values()
                    .joinToString(", ") { it.name.lowercase() }
                errors.add(
                    FieldError(
                        "newLanguage",
                        "invalid_value",
                        "Unsupported language. Allowed: $allowed"
                    )
                )
            }
        }

        request.newCourseName?.let { name ->
            if (name.isBlank()) {
                errors.add(FieldError("newCourseName", "invalid_value", "Course name cannot be blank"))
            } else if (name.length > 255) {
                errors.add(FieldError("newCourseName", "max_length", "Course name must not exceed 255 characters"))
            }
        }

        if (errors.isNotEmpty()) throw ValidationException(errors)
    }


}
