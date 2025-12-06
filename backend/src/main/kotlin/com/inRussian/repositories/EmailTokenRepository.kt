package com.inRussian.repositories

import com.inRussian.config.dbQuery
import com.inRussian.tables.v2.EmailTokens
import com.inRussian.tables.v2.RecoveryCheckResult
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.mindrot.jbcrypt.BCrypt
import java.time.Instant
import kotlin.random.Random

interface EmailTokenRepository {
    suspend fun issue(email: String, ttlMinutes: Long = 10): String
    suspend fun check(email: String, token: String): RecoveryCheckResult
    suspend fun verifyAndConsume(email: String, token: String): RecoveryCheckResult
    suspend fun consume(email: String): Boolean
}

class ExposedEmailTokenRepository : EmailTokenRepository {

    override suspend fun issue(email: String, ttlMinutes: Long): String = dbQuery {
        EmailTokens.deleteWhere { EmailTokens.email eq email.lowercase() }

        val code = generateCode()
        val hash = BCrypt.hashpw(code, BCrypt.gensalt(8))
        val expiresAt = Instant.now().plusSeconds(ttlMinutes * 60)

        EmailTokens.insert {
            it[EmailTokens.email] = email.lowercase()
            it[tokenHash] = hash
            it[EmailTokens.expiresAt] = expiresAt
            it[createdAt] = Instant.now()
        }
        code
    }

    override suspend fun check(email: String, token: String): RecoveryCheckResult = dbQuery {
        val row = EmailTokens
            .selectAll()
            .where { EmailTokens.email eq email.lowercase() }
            .firstOrNull() ?: return@dbQuery RecoveryCheckResult.NOT_FOUND

        if (Instant.now().isAfter(row[EmailTokens.expiresAt])) {
            EmailTokens.deleteWhere { EmailTokens.email eq email.lowercase() }
            return@dbQuery RecoveryCheckResult.EXPIRED
        }

        val ok = BCrypt.checkpw(token, row[EmailTokens.tokenHash])
        if (ok) RecoveryCheckResult.VALID else RecoveryCheckResult.INVALID
    }

    override suspend fun verifyAndConsume(email: String, token: String): RecoveryCheckResult = dbQuery {
        val row = EmailTokens
            .selectAll()
            .where { EmailTokens.email eq email.lowercase() }
            .firstOrNull() ?: return@dbQuery RecoveryCheckResult.NOT_FOUND

        if (Instant.now().isAfter(row[EmailTokens.expiresAt])) {
            EmailTokens.deleteWhere { EmailTokens.email eq email.lowercase() }
            return@dbQuery RecoveryCheckResult.EXPIRED
        }

        val ok = BCrypt.checkpw(token, row[EmailTokens.tokenHash])
        if (ok) {
            EmailTokens.deleteWhere { EmailTokens.email eq email.lowercase() }
            RecoveryCheckResult.VALID
        } else {
            RecoveryCheckResult.INVALID
        }
    }

    override suspend fun consume(email: String): Boolean = dbQuery {
        EmailTokens.deleteWhere { EmailTokens.email eq email.lowercase() } > 0
    }

    private fun generateCode(): String = "%06d".format(Random.nextInt(0, 1_000_000))
}