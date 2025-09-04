package com.inRussian.config

import com.inRussian.repositories.PasswordRecoveryTokens
import com.inRussian.tables.*
import com.inRussian.tables.v2.BadgeRuleTable
import com.inRussian.tables.v2.UserBadgeTable
import com.inRussian.tables.v2.UserCourseProgressTable
import com.inRussian.tables.v2.UserDailySolveTable

import com.inRussian.tables.v2.UserTaskAttemptTable
import com.inRussian.tables.v2.UserTaskStateTable
import com.inRussian.tables.v2.UserThemeProgressTable
import com.inRussian.tables.v2.UserThemeQueueItemTable
import com.inRussian.tables.v2.UserThemeQueueStateTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction


fun Application.configureDatabase() {
    val dbConfig = environment.config.config("postgres")
    val url = dbConfig.property("url").getString()
    val user = dbConfig.property("user").getString()
    val password = dbConfig.property("password").getString()
    val maxPoolSize = dbConfig.propertyOrNull("maximumPoolSize")?.getString()?.toInt() ?: 10

    val hikariConfig = HikariConfig().apply {
        jdbcUrl = url
        username = user
        this.password = password
        maximumPoolSize = maxPoolSize
    }
    val dataSource = HikariDataSource(hikariConfig)


    Database.connect(dataSource)
    transaction {
        SchemaUtils.create(
            BadgeRuleTable,
            UserBadgeTable,
            UserDailySolveTable,
            UserCourseProgressTable,
            UserThemeQueueStateTable,
            UserThemeQueueItemTable,
            UserTaskAttemptTable,
            UserThemeProgressTable,
            UserTaskStateTable,
            Courses,
            CourseStatistics,
            MediaFiles,
            StaffProfiles,
            Users,
            PasswordRecoveryTokens,
            Themes,
            UserBadges,
            UserCourseEnrollments,
            UserCourseStatistics,
            UserLanguageSkills,
            UserProfiles,
            UserStatistics,
            TaskEntity,
            TaskTypes,
            TaskToTypes,
        )
    }
    try {
        transaction {
            exec("SELECT 1;")
            println("Database connection successful.")
        }
    } catch (e: Exception) {
        println("Database connection failed: ${e.message}")
    }
}