package com.inRussian.config

import com.inRussian.tables.*
import com.inRussian.tables.v2.BadgeRuleTable
import com.inRussian.tables.v2.EmailTokens
import com.inRussian.tables.v2.RetrySwitchTable
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
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
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

        minimumIdle = 5
        idleTimeout = 600000 // 10 minutes
        connectionTimeout = 30000 // 30 seconds
        maxLifetime = 1800000 // 30 minutes

        // Connection validation
        connectionTestQuery = "SELECT 1"
        validationTimeout = 5000

        leakDetectionThreshold = 60000 // 60 seconds

        // Auto-commit should be true for Exposed
        isAutoCommit = true

        // Additional settings
        addDataSourceProperty("cachePrepStmts", "true")
        addDataSourceProperty("prepStmtCacheSize", "250")
        addDataSourceProperty("prepStmtCacheSqlLimit", "2048")

        // Keep connections alive
        keepaliveTime = 30000 // 30 seconds
    }

    val dataSource = HikariDataSource(hikariConfig)

    Database.connect(dataSource)

    TransactionManager.manager.defaultMaxAttempts = 3

    transaction {
        SchemaUtils.createMissingTablesAndColumns(
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
            Users,
            EmailTokens,
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
            RetrySwitchTable
        )
        println("✅ Finished schema creation")

    }


    try {
        transaction {
            exec("SELECT 1;")
            println("✅ Database connection successful.")
        }
    } catch (e: Exception) {
        println("❌ Database connection failed: ${e.message}")
        throw e
    }
}
object DatabaseFactory {
    suspend fun <T> dbQuery(
        isolation: Int? = null,
        block: suspend Transaction.() -> T
    ): T = newSuspendedTransaction(
        context = Dispatchers.IO,
        transactionIsolation = isolation
    ) {
        queryTimeout = 30
        block()
    }
}

suspend fun <T> dbQuery(block: suspend Transaction.() -> T): T =
    DatabaseFactory.dbQuery(block = block)