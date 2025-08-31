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
    suspend fun createTask(request: CreateTaskRequest): TaskWithDetails
    suspend fun getTask(taskId: String): TaskWithDetails?
    suspend fun updateTask(taskId: String, request: UpdateTaskRequest): TaskWithDetails?
    suspend fun deleteTask(taskId: String): Boolean
    suspend fun getTasksByTheme(themeId: String): List<TaskWithDetails>

    // Task Content
    suspend fun createTaskContent(taskId: String, request: CreateTaskContentRequest): TaskContentItem
    suspend fun getTaskContent(contentId: String): TaskContentItem?
    suspend fun updateTaskContent(contentId: String, request: UpdateTaskContentRequest): TaskContentItem?
    suspend fun deleteTaskContent(contentId: String): Boolean

    // Task Answers
    suspend fun createTaskAnswer(taskId: String, request: CreateTaskAnswerRequest): TaskAnswerItem
    suspend fun getTaskAnswer(taskId: String): TaskAnswerItem?
    suspend fun updateTaskAnswer(taskId: String, request: UpdateTaskAnswerRequest): TaskAnswerItem?
    suspend fun deleteTaskAnswer(taskId: String): Boolean

    // Task Answer Options
    suspend fun createTaskAnswerOption(taskId: String, request: CreateTaskAnswerOptionRequest): TaskAnswerOptionItem
    suspend fun getTaskAnswerOption(optionId: String): TaskAnswerOptionItem?
    suspend fun updateTaskAnswerOption(optionId: String, request: UpdateTaskAnswerOptionRequest): TaskAnswerOptionItem?
    suspend fun deleteTaskAnswerOption(optionId: String): Boolean

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

    override suspend fun createTask(request: CreateTaskRequest): TaskWithDetails = transaction {
        val taskId = Tasks.insertAndGetId {
            it[themeId] = UUID.fromString(request.themeId)
            it[name] = request.name
            it[taskType] = request.taskType
            it[question] = request.question
            it[instructions] = request.instructions
            it[isTraining] = request.isTraining
            it[orderNum] = request.orderNum
        }

        Tasks.selectAll().where { Tasks.id eq taskId }.single().toTask()
    }

    override suspend fun getTask(taskId: String): TaskWithDetails? = transaction {
        Tasks.selectAll().where { Tasks.id eq UUID.fromString(taskId) }
            .singleOrNull()?.toTask()?.let { task ->
                val content = TaskContent.selectAll().where { TaskContent.taskId eq UUID.fromString(taskId) }
                    .orderBy(TaskContent.orderNum)
                    .map { it.toTaskContent() }

                val answer = TaskAnswers.selectAll().where { TaskAnswers.taskId eq UUID.fromString(taskId) }
                    .singleOrNull()?.toTaskAnswer()

                val options =
                    TaskAnswerOptions.selectAll().where { TaskAnswerOptions.taskId eq UUID.fromString(taskId) }
                        .orderBy(TaskAnswerOptions.orderNum)
                        .map { it.toTaskAnswerOption() }

                task.copy(content = content, answer = answer, answerOptions = options)
            }
    }

    override suspend fun updateTask(taskId: String, request: UpdateTaskRequest): TaskWithDetails? {
        val taskUuid = UUID.fromString(taskId)
        transaction {
            Tasks.update({ Tasks.id eq taskUuid }) {
                request.name?.let { name -> it[Tasks.name] = name }
                request.question?.let { question -> it[Tasks.question] = question }
                request.instructions?.let { instructions -> it[Tasks.instructions] = instructions }
                request.isTraining?.let { training -> it[Tasks.isTraining] = training }
                request.orderNum?.let { order -> it[Tasks.orderNum] = order }
            }
        }
        return getTask(taskId)
    }

    override suspend fun deleteTask(taskId: String): Boolean = transaction {
        val taskUuid = UUID.fromString(taskId)
        TaskContent.deleteWhere { TaskContent.taskId eq taskUuid }
        TaskAnswers.deleteWhere { TaskAnswers.taskId eq taskUuid }
        TaskAnswerOptions.deleteWhere { TaskAnswerOptions.taskId eq taskUuid }
        Tasks.deleteWhere { Tasks.id eq taskUuid } > 0
    }

    override suspend fun getTasksByTheme(themeId: String): List<TaskWithDetails> = transaction {
        Tasks.selectAll().where { Tasks.themeId eq UUID.fromString(themeId) }
            .orderBy(Tasks.orderNum)
            .map { it.toTask() }
    }

    override suspend fun createTaskContent(taskId: String, request: CreateTaskContentRequest): TaskContentItem =
        transaction {
            val contentId = TaskContent.insertAndGetId {
                it[TaskContent.taskId] = UUID.fromString(taskId)
                it[contentType] = request.contentType
                it[contentId] = request.contentId
                it[description] = request.description
                it[transcription] = request.transcription
                it[translation] = request.translation
                it[orderNum] = request.orderNum
            }

            TaskContent.selectAll().where { TaskContent.id eq contentId }.single().toTaskContent()
        }

    override suspend fun getTaskContent(contentId: String): TaskContentItem? = transaction {
        TaskContent.selectAll().where { TaskContent.id eq UUID.fromString(contentId) }
            .singleOrNull()?.toTaskContent()
    }

    override suspend fun updateTaskContent(contentId: String, request: UpdateTaskContentRequest): TaskContentItem? {
        val contentUuid = UUID.fromString(contentId)
        transaction {
            TaskContent.update({ TaskContent.id eq contentUuid }) {
                request.contentType?.let { type -> it[contentType] = type }
                request.contentId?.let { id -> it[TaskContent.contentId] = id }
                request.description?.let { desc -> it[description] = desc }
                request.transcription?.let { trans -> it[transcription] = trans }
                request.translation?.let { transl -> it[translation] = transl }
                request.orderNum?.let { order -> it[orderNum] = order }
            }
        }
        return getTaskContent(contentId)
    }

    override suspend fun deleteTaskContent(contentId: String): Boolean = transaction {
        TaskContent.deleteWhere { TaskContent.id eq UUID.fromString(contentId) } > 0
    }

    override suspend fun createTaskAnswer(taskId: String, request: CreateTaskAnswerRequest): TaskAnswerItem =
        transaction {
            TaskAnswers.insert {
                it[TaskAnswers.taskId] = UUID.fromString(taskId)
                it[answerType] = request.answerType
                it[correctAnswer] = request.correctAnswer
            }

            TaskAnswers.selectAll().where { TaskAnswers.taskId eq UUID.fromString(taskId) }.single().toTaskAnswer()
        }

    override suspend fun getTaskAnswer(taskId: String): TaskAnswerItem? = transaction {
        TaskAnswers.selectAll().where { TaskAnswers.taskId eq UUID.fromString(taskId) }
            .singleOrNull()?.toTaskAnswer()
    }

    override suspend fun updateTaskAnswer(taskId: String, request: UpdateTaskAnswerRequest): TaskAnswerItem? {
        val taskUuid = UUID.fromString(taskId)
        transaction {
            TaskAnswers.update({ TaskAnswers.taskId eq taskUuid }) {
                request.answerType?.let { type -> it[TaskAnswers.answerType] = type }
                request.correctAnswer?.let { answer -> it[TaskAnswers.correctAnswer] = answer }
            }
        }
        return getTaskAnswer(taskId)
    }

    override suspend fun deleteTaskAnswer(taskId: String): Boolean = transaction {
        TaskAnswers.deleteWhere { TaskAnswers.taskId eq UUID.fromString(taskId) } > 0
    }

    override suspend fun createTaskAnswerOption(
        taskId: String,
        request: CreateTaskAnswerOptionRequest
    ): TaskAnswerOptionItem = transaction {
        val optionId = TaskAnswerOptions.insertAndGetId {
            it[TaskAnswerOptions.taskId] = UUID.fromString(taskId)
            it[optionText] = request.optionText
            it[optionAudioId] = request.optionAudioId
            it[isCorrect] = request.isCorrect
            it[orderNum] = request.orderNum
        }

        TaskAnswerOptions.selectAll().where { TaskAnswerOptions.id eq optionId }.single().toTaskAnswerOption()
    }

    override suspend fun getTaskAnswerOption(optionId: String): TaskAnswerOptionItem? = transaction {
        TaskAnswerOptions.selectAll().where { TaskAnswerOptions.id eq UUID.fromString(optionId) }
            .singleOrNull()?.toTaskAnswerOption()
    }

    override suspend fun updateTaskAnswerOption(
        optionId: String,
        request: UpdateTaskAnswerOptionRequest
    ): TaskAnswerOptionItem? {
        val optionUuid = UUID.fromString(optionId)
        transaction {
            TaskAnswerOptions.update({ TaskAnswerOptions.id eq optionUuid }) {
                request.optionText?.let { text -> it[optionText] = text }
                request.optionAudioId?.let { audio -> it[optionAudioId] = audio }
                request.isCorrect?.let { correct -> it[isCorrect] = correct }
                request.orderNum?.let { order -> it[orderNum] = order }
            }
        }
        return getTaskAnswerOption(optionId)
    }

    override suspend fun deleteTaskAnswerOption(optionId: String): Boolean = transaction {
        TaskAnswerOptions.deleteWhere { TaskAnswerOptions.id eq UUID.fromString(optionId) } > 0
    }

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
        val tasksCount = Tasks.selectAll().count()

        CountStats(coursesCount, sectionsCount, themesCount, tasksCount)
    }

    override suspend fun getCourseTasksCount(courseId: String): Long = transaction {
        (Sections innerJoin Themes innerJoin Tasks)
            .selectAll()
            .where { Sections.courseId eq UUID.fromString(courseId) }
            .count()
    }

    override suspend fun getSectionTasksCount(sectionId: String): Long = transaction {
        (Themes innerJoin Tasks)
            .selectAll()
            .where { Themes.sectionId eq UUID.fromString(sectionId) }
            .count()
    }

    override suspend fun getThemeTasksCount(themeId: String): Long = transaction {
        Tasks.selectAll().where { Tasks.themeId eq UUID.fromString(themeId) }.count()
    }
}