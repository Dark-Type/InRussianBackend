package com.inRussian.tables.v2

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

enum class RecoveryCheckResult { VALID, INVALID, EXPIRED, NOT_FOUND }

object EmailTokens : Table("password_recovery_tokens") {
    val email = varchar("email", 255).index()
    val tokenHash = varchar("token_hash", 100)
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(email)
}