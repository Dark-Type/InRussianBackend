package com.inRussian.repositories

import com.inRussian.config.dbQuery
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.mindrot.jbcrypt.BCrypt
import java.time.Instant
import kotlin.random.Random

enum class RecoveryCheckResult { VALID, INVALID, EXPIRED, NOT_FOUND }

object PasswordRecoveryTokens : Table("password_recovery_tokens") {
    val email = varchar("email", 255).index()
    val tokenHash = varchar("token_hash", 100) // BCrypt-hashed 6-digit code
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(email)
}

interface PasswordRecoveryTokenRepository {
    suspend fun issue(email: String, ttlMinutes: Long = 10): String  // returns plaintext 6-digit code
    suspend fun check(email: String, token: String): RecoveryCheckResult
    suspend fun verifyAndConsume(email: String, token: String): RecoveryCheckResult
    suspend fun consume(email: String): Boolean
}

class ExposedPasswordRecoveryTokenRepository : PasswordRecoveryTokenRepository {

    override suspend fun issue(email: String, ttlMinutes: Long): String = dbQuery {
        // Ensure only one active token per email
        PasswordRecoveryTokens.deleteWhere { PasswordRecoveryTokens.email eq email.lowercase() }

        val code = generateCode()
        val hash = BCrypt.hashpw(code, BCrypt.gensalt(8))
        val expiresAt = Instant.now().plusSeconds(ttlMinutes * 60)

        PasswordRecoveryTokens.insert {
            it[PasswordRecoveryTokens.email] = email.lowercase()
            it[tokenHash] = hash
            it[PasswordRecoveryTokens.expiresAt] = expiresAt
            it[createdAt] = Instant.now()
        }
        code
    }

    override suspend fun check(email: String, token: String): RecoveryCheckResult = dbQuery {
        val row = PasswordRecoveryTokens
            .selectAll()
            .where { PasswordRecoveryTokens.email eq email.lowercase() }
            .firstOrNull() ?: return@dbQuery RecoveryCheckResult.NOT_FOUND

        if (Instant.now().isAfter(row[PasswordRecoveryTokens.expiresAt])) {
            PasswordRecoveryTokens.deleteWhere { PasswordRecoveryTokens.email eq email.lowercase() }
            return@dbQuery RecoveryCheckResult.EXPIRED
        }

        val ok = BCrypt.checkpw(token, row[PasswordRecoveryTokens.tokenHash])
        if (ok) RecoveryCheckResult.VALID else RecoveryCheckResult.INVALID
    }

    override suspend fun verifyAndConsume(email: String, token: String): RecoveryCheckResult = dbQuery {
        val row = PasswordRecoveryTokens
            .selectAll()
            .where { PasswordRecoveryTokens.email eq email.lowercase() }
            .firstOrNull() ?: return@dbQuery RecoveryCheckResult.NOT_FOUND

        if (Instant.now().isAfter(row[PasswordRecoveryTokens.expiresAt])) {
            PasswordRecoveryTokens.deleteWhere { PasswordRecoveryTokens.email eq email.lowercase() }
            return@dbQuery RecoveryCheckResult.EXPIRED
        }

        val ok = BCrypt.checkpw(token, row[PasswordRecoveryTokens.tokenHash])
        if (ok) {
            PasswordRecoveryTokens.deleteWhere { PasswordRecoveryTokens.email eq email.lowercase() }
            RecoveryCheckResult.VALID
        } else {
            RecoveryCheckResult.INVALID
        }
    }

    override suspend fun consume(email: String): Boolean = dbQuery {
        PasswordRecoveryTokens.deleteWhere { PasswordRecoveryTokens.email eq email.lowercase() } > 0
    }

    private fun generateCode(): String = "%06d".format(Random.nextInt(0, 1_000_000))
}