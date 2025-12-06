package com.inRussian.repositories

import com.inRussian.config.dbQuery
import com.inRussian.tables.UserCourseStatistics
import com.inRussian.tables.UserStatistics
import org.jetbrains.exposed.sql.*

import java.util.UUID

interface UserStatisticsRepository {
    suspend fun overallAverageTime(): Long?
    suspend fun overallAverageProgress(): Double?
}

interface UserCourseStatisticsRepository {
    suspend fun averageTimeByCourse(courseId: String): Long?
    suspend fun averageProgressByCourse(courseId: String): Double?
}

class ExposedUserStatisticsRepository : UserStatisticsRepository {
    override suspend fun overallAverageTime(): Long? = dbQuery {
        UserStatistics.selectAll()
            .map { it[UserStatistics.totalTimeSpentSeconds] }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toLong()
    }

    override suspend fun overallAverageProgress(): Double? = dbQuery {
        val userStats = UserStatistics.selectAll().map { row ->
            val totalTasks = row[UserStatistics.totalTasksAttempted]
            val completedTasks = row[UserStatistics.totalTasksCompleted]
            if (totalTasks > 0) (completedTasks.toDouble() / totalTasks.toDouble()) * 100.0 else 0.0
        }
        userStats.takeIf { it.isNotEmpty() }?.average()
    }
}

class ExposedUserCourseStatisticsRepository : UserCourseStatisticsRepository {
    override suspend fun averageTimeByCourse(courseId: String): Long? = dbQuery {
        UserCourseStatistics.selectAll()
            .where { UserCourseStatistics.courseId eq UUID.fromString(courseId) }
            .map { it[UserCourseStatistics.timeSpentSeconds] }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toLong()
    }

    override suspend fun averageProgressByCourse(courseId: String): Double? = dbQuery {
        val progress = UserCourseStatistics.selectAll()
            .where { UserCourseStatistics.courseId eq UUID.fromString(courseId) }
            .map { it[UserCourseStatistics.progressPercentage].toDouble() }
        progress.takeIf { it.isNotEmpty() }?.average()
    }
}