package com.inRussian.repositories.v2

import com.inRussian.tables.UserCourseEnrollments
import com.inRussian.tables.v2.UserCourseProgressTable
import com.inRussian.tables.v2.UserSectionProgressTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

class StatsRepository {

    fun listEnrolledCourseIds(userId: UUID): List<UUID> =
        UserCourseEnrollments
            .selectAll().where { UserCourseEnrollments.userId eq userId }
            .map { it[UserCourseEnrollments.courseId].value }

    fun listCourseProgressForUser(userId: UUID, courseIds: List<UUID>): List<ResultRow> =
        if (courseIds.isEmpty()) emptyList()
        else UserCourseEnrollments
            .selectAll().where {
                (UserCourseProgressTable.userId eq userId) and
                        (UserCourseProgressTable.courseId inList courseIds)
            }
            .toList()

    fun listSectionProgressForUser(userId: UUID, courseIds: List<UUID>): List<ResultRow> =
        if (courseIds.isEmpty()) emptyList()
        else UserSectionProgressTable
            .selectAll().where {
                (UserSectionProgressTable.userId eq userId) and
                        (UserSectionProgressTable.courseId inList courseIds)
            }
            .orderBy(UserSectionProgressTable.sectionId to SortOrder.ASC)
            .toList()
}