package com.inRussian.repositories

import com.inRussian.models.content.*
import com.inRussian.models.tasks.*
import com.inRussian.requests.content.*
import com.inRussian.tables.*
import com.inRussian.tables.TaskContent
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.name
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.toString

interface ContentRepository {
    // Tasks

    // Themes
    suspend fun createTheme(request: CreateThemeRequest): Theme
    suspend fun getTheme(themeId: String): Theme?
    suspend fun updateTheme(themeId: String, request: UpdateThemeRequest): Theme?
    suspend fun deleteTheme(themeId: String): Boolean
    suspend fun getThemesBySection(sectionId: String): List<Theme>

    // Sections
    suspend fun createSection(request: CreateSectionRequest): Section
    suspend fun getSection(sectionId: String): Section?
    suspend fun updateSection(sectionId: String, request: UpdateSectionRequest): Section?
    suspend fun deleteSection(sectionId: String): Boolean
    suspend fun getSectionsByCourse(courseId: String): List<Section>

    // Courses
    suspend fun createCourse(authorId: String, request: CreateCourseRequest): Course
    suspend fun getCourse(courseId: String): Course?
    suspend fun updateCourse(courseId: String, request: UpdateCourseRequest): Course?
    suspend fun deleteCourse(courseId: String): Boolean
    suspend fun getAllCourses(): List<Course>

    // Reports
    suspend fun createReport(reporterId: String, request: CreateReportRequest): Report
    suspend fun getReport(reportId: String): Report?
    suspend fun deleteReport(reportId: String): Boolean
    suspend fun getAllReports(): List<Report>

    // Statistics
    suspend fun getCountStats(): CountStats
    suspend fun getCourseTasksCount(courseId: String): Long
    suspend fun getSectionTasksCount(sectionId: String): Long
    suspend fun getThemeTasksCount(themeId: String): Long

}

class ExposedContentRepository : ContentRepository {

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

    private fun ResultRow.toTaskContent() = TaskContentItem(
        id = this[TaskContent.id].toString(),
        taskId = this[TaskContent.taskId].toString(),
        contentType = this[TaskContent.contentType],
        contentId = this[TaskContent.contentId],
        description = this[TaskContent.description],
        transcription = this[TaskContent.transcription],
        translation = this[TaskContent.translation],
        orderNum = this[TaskContent.orderNum]
    )

    private fun ResultRow.toTaskAnswer() = TaskAnswerItem(
        taskId = this[TaskAnswers.taskId].toString(),
        answerType = this[TaskAnswers.answerType],
        correctAnswer = this[TaskAnswers.correctAnswer]
    )

    private fun ResultRow.toTaskAnswerOption() = TaskAnswerOptionItem(
        id = this[TaskAnswerOptions.id].toString(),
        taskId = this[TaskAnswerOptions.taskId].toString(),
        optionText = this[TaskAnswerOptions.optionText],
        optionAudioId = this[TaskAnswerOptions.optionAudioId],
        isCorrect = this[TaskAnswerOptions.isCorrect],
        orderNum = this[TaskAnswerOptions.orderNum]
    )

    private fun ResultRow.toTheme() = Theme(
        id = this[Themes.id].toString(),
        sectionId = this[Themes.sectionId].toString(),
        name = this[Themes.name],
        description = this[Themes.description],
        orderNum = this[Themes.orderNum],
        createdAt = this[Themes.createdAt].toString()
    )

    private fun ResultRow.toSection() = Section(
        id = this[Sections.id].toString(),
        courseId = this[Sections.courseId].toString(),
        name = this[Sections.name],
        description = this[Sections.description],
        orderNum = this[Sections.orderNum],
        createdAt = this[Sections.createdAt].toString()
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
        posterId = this[Courses.posterId]?.toString()
    )
    private fun ResultRow.toReport() = Report(
        id = this[Reports.id].toString(),
        description = this[Reports.description],
        taskId = this[Reports.taskId].toString(),
        reporterId = this[Reports.reporterId].toString(),
        createdAt = this[Reports.createdAt].toString()
    )



    override suspend fun createTheme(request: CreateThemeRequest): Theme = transaction {
        val themeId = Themes.insertAndGetId {
            it[sectionId] = UUID.fromString(request.sectionId)
            it[name] = request.name
            it[description] = request.description
            it[orderNum] = request.orderNum
        }

        Themes.selectAll().where { Themes.id eq themeId }.single().toTheme()
    }

    override suspend fun getTheme(themeId: String): Theme? = transaction {
        Themes.selectAll().where { Themes.id eq UUID.fromString(themeId) }
            .singleOrNull()?.toTheme()
    }

    override suspend fun updateTheme(themeId: String, request: UpdateThemeRequest): Theme? {
        val themeUuid = UUID.fromString(themeId)
        transaction {
            Themes.update({ Themes.id eq themeUuid }) {
                request.name?.let { name -> it[Themes.name] = name }
                request.description?.let { desc -> it[Themes.description] = desc }
                request.orderNum?.let { order -> it[Themes.orderNum] = order }
            }
        }
        return getTheme(themeId)
    }

    override suspend fun deleteTheme(themeId: String): Boolean = transaction {
        val themeUuid = UUID.fromString(themeId)

        val taskIds = Tasks.selectAll().where { Tasks.themeId eq themeUuid }
            .map { it[Tasks.id] }

        taskIds.forEach { taskId ->
            TaskContent.deleteWhere { TaskContent.taskId eq taskId }
            TaskAnswers.deleteWhere { TaskAnswers.taskId eq taskId }
            TaskAnswerOptions.deleteWhere { TaskAnswerOptions.taskId eq taskId }
        }

        Tasks.deleteWhere { Tasks.themeId eq themeUuid }
        Themes.deleteWhere { Themes.id eq themeUuid } > 0
    }

    override suspend fun getThemesBySection(sectionId: String): List<Theme> = transaction {
        Themes.selectAll().where { Themes.sectionId eq UUID.fromString(sectionId) }
            .orderBy(Themes.orderNum)
            .map { it.toTheme() }
    }

    override suspend fun createSection(request: CreateSectionRequest): Section = transaction {
        val sectionId = Sections.insertAndGetId {
            it[courseId] = UUID.fromString(request.courseId)
            it[name] = request.name
            it[description] = request.description
            it[orderNum] = request.orderNum
        }

        Sections.selectAll().where { Sections.id eq sectionId }.single().toSection()
    }

    override suspend fun getSection(sectionId: String): Section? = transaction {
        Sections.selectAll().where { Sections.id eq UUID.fromString(sectionId) }
            .singleOrNull()?.toSection()
    }

    override suspend fun updateSection(sectionId: String, request: UpdateSectionRequest): Section? {
        val sectionUuid = UUID.fromString(sectionId)
        transaction {
            Sections.update({ Sections.id eq sectionUuid }) {
                request.name?.let { name -> it[Sections.name] = name }
                request.description?.let { desc -> it[Sections.description] = desc }
                request.orderNum?.let { order -> it[Sections.orderNum] = order }
            }
        }
        return getSection(sectionId)
    }

    override suspend fun deleteSection(sectionId: String): Boolean = transaction {
        val sectionUuid = UUID.fromString(sectionId)

        val themeIds = Themes.selectAll().where { Themes.sectionId eq sectionUuid }
            .map { it[Themes.id] }

        themeIds.forEach { themeId ->
            val taskIds = Tasks.selectAll().where { Tasks.themeId eq themeId }
                .map { it[Tasks.id] }

            taskIds.forEach { taskId ->
                TaskContent.deleteWhere { TaskContent.taskId eq taskId }
                TaskAnswers.deleteWhere { TaskAnswers.taskId eq taskId }
                TaskAnswerOptions.deleteWhere { TaskAnswerOptions.taskId eq taskId }
            }

            Tasks.deleteWhere { Tasks.themeId eq themeId }
        }

        Themes.deleteWhere { Themes.sectionId eq sectionUuid }
        Sections.deleteWhere { Sections.id eq sectionUuid } > 0
    }

    override suspend fun getSectionsByCourse(courseId: String): List<Section> = transaction {
        Sections.selectAll().where { Sections.courseId eq UUID.fromString(courseId) }
            .orderBy(Sections.orderNum)
            .map { it.toSection() }
    }

    override suspend fun createCourse(authorId: String, request: CreateCourseRequest): Course = transaction {
        val courseId = Courses.insertAndGetId {
            it[name] = request.name
            it[description] = request.description
            it[Courses.authorId] = UUID.fromString(authorId)
            it[posterId] = request.coursePoster?.let(UUID::fromString)
            it[authorUrl] = request.authorUrl
            it[language] = request.language
            it[isPublished] = request.isPublished
            it[updatedAt] = CurrentTimestamp
        }
        Courses.selectAll().where { Courses.id eq courseId }.single().toCourse()
    }

    override suspend fun getCourse(courseId: String): Course? = transaction {
        Courses.selectAll().where { Courses.id eq UUID.fromString(courseId) }
            .singleOrNull()?.toCourse()
    }

    override suspend fun updateCourse(courseId: String, request: UpdateCourseRequest): Course? {
        val courseUuid = UUID.fromString(courseId)
        transaction {
            Courses.update({ Courses.id eq courseUuid }) {
                request.name?.let { name -> it[Courses.name] = name }
                request.description?.let { desc -> it[description] = desc }
                request.authorUrl?.let { url -> it[authorUrl] = url }
                request.language?.let { lang -> it[language] = lang }
                request.isPublished?.let { published -> it[isPublished] = published }
                request.coursePoster?.let { poster -> it[posterId] = UUID.fromString(poster) } // FIX: обновление постера
                it[updatedAt] = CurrentTimestamp
            }
        }
        return getCourse(courseId)
    }

    override suspend fun deleteCourse(courseId: String): Boolean = transaction {
        val courseUuid = UUID.fromString(courseId)

        val sectionIds = Sections.selectAll().where { Sections.courseId eq courseUuid }
            .map { it[Sections.id] }

        sectionIds.forEach { sectionId ->
            val themeIds = Themes.selectAll().where { Themes.sectionId eq sectionId }
                .map { it[Themes.id] }

            themeIds.forEach { themeId ->
                val taskIds = Tasks.selectAll().where { Tasks.themeId eq themeId }
                    .map { it[Tasks.id] }

                taskIds.forEach { taskId ->
                    TaskContent.deleteWhere { TaskContent.taskId eq taskId }
                    TaskAnswers.deleteWhere { TaskAnswers.taskId eq taskId }
                    TaskAnswerOptions.deleteWhere { TaskAnswerOptions.taskId eq taskId }
                }

                Tasks.deleteWhere { Tasks.themeId eq themeId }
            }

            Themes.deleteWhere { Themes.sectionId eq sectionId }
        }

        Sections.deleteWhere { Sections.courseId eq courseUuid }
        Courses.deleteWhere { Courses.id eq courseUuid } > 0
    }

    override suspend fun getAllCourses(): List<Course> = transaction {
        Courses.selectAll().orderBy(Courses.createdAt, SortOrder.DESC)
            .map { it.toCourse() }
    }

    override suspend fun createReport(reporterId: String, request: CreateReportRequest): Report = transaction {
        val reportId = Reports.insertAndGetId {
            it[description] = request.description
            it[taskId] = UUID.fromString(request.taskId)
            it[Reports.reporterId] = UUID.fromString(reporterId)
        }

        Reports.selectAll().where { Reports.id eq reportId }.single().toReport()
    }

    override suspend fun getReport(reportId: String): Report? = transaction {
        Reports.selectAll().where { Reports.id eq UUID.fromString(reportId) }
            .singleOrNull()?.toReport()
    }

    override suspend fun deleteReport(reportId: String): Boolean = transaction {
        Reports.deleteWhere { Reports.id eq UUID.fromString(reportId) } > 0
    }

    override suspend fun getAllReports(): List<Report> = transaction {
        Reports.selectAll().orderBy(Reports.createdAt, SortOrder.DESC)
            .map { it.toReport() }
    }

    override suspend fun getCountStats(): CountStats = transaction {
        val coursesCount = Courses.selectAll().count()
        val sectionsCount = Sections.selectAll().count()
        val themesCount = Themes.selectAll().count()
        val tasksCount = TaskEntity.selectAll().count()

        CountStats(coursesCount, sectionsCount, themesCount, tasksCount)
    }

    override suspend fun getCourseTasksCount(courseId: String): Long = transaction {
        (Sections innerJoin Themes innerJoin TaskEntity)
            .selectAll()
            .where { Sections.courseId eq UUID.fromString(courseId) }
            .count()
    }

    override suspend fun getSectionTasksCount(sectionId: String): Long = transaction {
        (Themes innerJoin TaskEntity)
            .selectAll()
            .where { Themes.sectionId eq UUID.fromString(sectionId) }
            .count()
    }
    override suspend fun getThemeTasksCount(themeId: String): Long = transaction {
        TaskEntity
            .selectAll()
            .where { TaskEntity.themeId eq UUID.fromString(themeId) }
            .count()
    }
}