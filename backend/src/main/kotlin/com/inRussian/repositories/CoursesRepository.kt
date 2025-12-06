package com.inRussian.repositories

import com.inRussian.config.dbQuery
import com.inRussian.models.content.Course
import com.inRussian.requests.content.CreateCourseRequest
import com.inRussian.requests.content.UpdateCourseRequest
import com.inRussian.tables.Courses
import com.inRussian.tables.Users
import com.inRussian.tables.MediaFiles
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.dao.id.EntityID
import java.util.UUID

interface CoursesRepository {
    suspend fun create(authorId: String, req: CreateCourseRequest): Course
    suspend fun findById(id: String): Course?
    suspend fun update(id: String, req: UpdateCourseRequest): Course?
    suspend fun delete(id: String): Boolean
    suspend fun listAll(): List<Course>
}

class ExposedCoursesRepository : com.inRussian.repositories.CoursesRepository {
    override suspend fun create(authorId: String, req: CreateCourseRequest): Course = dbQuery {
        val id = Courses.insertAndGetId {
            it[Courses.name] = req.name
            it[Courses.description] = req.description
            it[Courses.authorId] = EntityID(UUID.fromString(authorId), Users)
            it[Courses.posterId] = req.coursePoster?.let { pid -> EntityID(UUID.fromString(pid), MediaFiles) }
            it[Courses.authorUrl] = req.authorUrl
            it[Courses.language] = req.language
            it[Courses.isPublished] = req.isPublished
            it[Courses.updatedAt] = CurrentTimestamp
        }
        Courses.selectAll().where { Courses.id eq id }.single().toCourse()
    }

    override suspend fun findById(id: String): Course? = dbQuery {
        Courses.selectAll().where { Courses.id eq UUID.fromString(id) }.singleOrNull()?.toCourse()
    }

    override suspend fun update(id: String, req: UpdateCourseRequest): Course? = dbQuery {
        val uuid = UUID.fromString(id)
        Courses.update({ Courses.id eq uuid }) {
            req.name?.let { v -> it[Courses.name] = v }
            req.description?.let { v -> it[Courses.description] = v }
            req.authorUrl?.let { v -> it[Courses.authorUrl] = v }
            req.language?.let { v -> it[Courses.language] = v }
            req.isPublished?.let { v -> it[Courses.isPublished] = v }
            req.coursePoster?.let { v -> it[Courses.posterId] = EntityID(UUID.fromString(v), MediaFiles) }
            it[Courses.updatedAt] = CurrentTimestamp
        }
        Courses.selectAll().where { Courses.id eq uuid }.singleOrNull()?.toCourse()
    }

    override suspend fun delete(id: String): Boolean = dbQuery {
        Courses.deleteWhere { Courses.id eq UUID.fromString(id) } > 0
    }

    override suspend fun listAll(): List<Course> = dbQuery {
        Courses.selectAll().orderBy(Courses.createdAt, SortOrder.DESC).map { it.toCourse() }
    }
}

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