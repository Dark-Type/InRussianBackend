package com.inRussian.routes


import com.inRussian.repositories.PasswordRecoveryTokenRepository
import com.inRussian.repositories.RecoveryCheckResult
import com.inRussian.repositories.UserRepository
import com.inRussian.services.mailer.Mailer
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.mindrot.jbcrypt.BCrypt

fun Route.passwordRecoveryRoutes(
    mailer: Mailer,
    recoveryRepo: PasswordRecoveryTokenRepository,
    userRepo: UserRepository,
    ttlMinutes: Long = 10
) {
    post("/password/recovery/request") {
        val req = runCatching { call.receive<RecoveryRequest>() }.getOrElse {
            return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid_body"))
        }
        val email = req.email.trim().lowercase()
        if (email.isBlank()) return@post call.respond(HttpStatusCode.OK, mapOf("ok" to true))

        val user = userRepo.findByEmail(email)
        if (user != null) {
            val code = recoveryRepo.issue(email, ttlMinutes)
            val subject = "Your password recovery code"
            val text = """
                Your password recovery code is: $code
                It expires in $ttlMinutes minutes.
                If you didn't request this, you can ignore this email.
            """.trimIndent()
            val html = """
                <p>Your password recovery code is: <b>$code</b></p>
                <p>It expires in $ttlMinutes minutes.</p>
                <p>If you didn't request this, you can ignore this email.</p>
            """.trimIndent()
            runCatching { mailer.send(to = email, subject = subject, text = text, html = html) }
        }
        call.respond(HttpStatusCode.OK, mapOf("ok" to true))
    }

    post("/password/recovery/check") {
        val req = runCatching { call.receive<RecoveryCheckRequest>() }.getOrElse {
            return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid_body"))
        }
        val email = req.email.trim().lowercase()
        val code = req.code.trim()
        if (email.isBlank() || code.isBlank()) {
            return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid_request"))
        }

        val status = recoveryRepo.check(email, code)
        call.respond(
            HttpStatusCode.OK,
            RecoveryCheckResponse(ok = status == RecoveryCheckResult.VALID, reason = status.name.lowercase())
        )
    }

    post("/password/recovery/reset") {
        val req = runCatching { call.receive<PasswordResetRequest>() }.getOrElse {
            return@post call.respond(
                HttpStatusCode.BadRequest,
                PasswordResetResponse(ok = false, error = "invalid_body")
            )
        }
        val email = req.email.trim().lowercase()
        val code = req.code.trim()
        val newPassword = req.newPassword

        if (email.isBlank() || code.isBlank() || newPassword.length < 8) {
            return@post call.respond(
                HttpStatusCode.BadRequest,
                PasswordResetResponse(ok = false, error = "invalid_request")
            )
        }

        val result = recoveryRepo.verifyAndConsume(email, code)
        if (result != RecoveryCheckResult.VALID) {
            return@post call.respond(
                HttpStatusCode.OK,
                PasswordResetResponse(ok = false, reason = result.name.lowercase())
            )
        }

        val newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt(12))
        val updated = userRepo.updatePassword(email, newHash)

        if (!updated) {
            return@post call.respond(
                HttpStatusCode.InternalServerError,
                PasswordResetResponse(ok = false, error = "update_failed")
            )
        }
        call.respond(HttpStatusCode.OK, PasswordResetResponse(ok = true))
    }
}

@Serializable
data class RecoveryRequest(val email: String)
@Serializable
data class RecoveryCheckRequest(val email: String, val code: String)
@Serializable
data class RecoveryCheckResponse(val ok: Boolean, val reason: String? = null)
@Serializable
data class PasswordResetRequest(val email: String, val code: String, val newPassword: String)

@Serializable
data class PasswordResetResponse(
    val ok: Boolean,
    val error: String? = null,
    val reason: String? = null
)