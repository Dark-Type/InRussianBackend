package com.inRussian.config

import com.inRussian.repositories.PasswordRecoveryTokens
import com.inRussian.tables.*
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
            Badges, Courses, CourseStatistics, MediaFiles, Sections, StaffProfiles, Users, PasswordRecoveryTokens,
            TaskAnswerOptions, TaskAnswers, Tasks, Themes, TaskContent, UserBadges, UserCourseEnrollments,
            UserCourseStatistics, UserLanguageSkills, UserProfiles, UserStatistics, UserTaskProgress, UserTaskQueue
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