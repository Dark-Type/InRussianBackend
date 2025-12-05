package com.inRussian.routes


import com.inRussian.models.users.SystemLanguage
import com.inRussian.models.users.UserStatus
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
            val (subject, text, html) = getPasswordRecoveryEmailContent(code, ttlMinutes, user.systemLanguage)
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

    // New endpoint for email verification
    post("/email/verify") {
        val req = runCatching { call.receive<EmailVerificationRequest>() }.getOrElse {
            return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid_body"))
        }
        val email = req.email.trim().lowercase()
        val code = req.code.trim()

        if (email.isBlank() || code.isBlank()) {
            return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid_request"))
        }

        val result = recoveryRepo.verifyAndConsume(email, code)
        if (result != RecoveryCheckResult.VALID) {
            return@post call.respond(
                HttpStatusCode.OK,
                EmailVerificationResponse(ok = false, reason = result.name.lowercase())
            )
        }

        // Update user status to ACTIVE
        val user = userRepo.findByEmail(email)
        if (user != null) {
            userRepo.updateStatus(user.id, UserStatus.ACTIVE)
            call.respond(HttpStatusCode.OK, EmailVerificationResponse(ok = true))
        } else {
            call.respond(
                HttpStatusCode.InternalServerError,
                EmailVerificationResponse(ok = false, error = "user_not_found")
            )
        }
    }

    // Resend verification code
    post("/email/resend") {
        val req = runCatching { call.receive<ResendVerificationRequest>() }.getOrElse {
            return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid_body"))
        }
        val email = req.email.trim().lowercase()
        if (email.isBlank()) return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid_request"))

        val user = userRepo.findByEmail(email)
        if (user != null && user.status == UserStatus.PENDING_VERIFICATION) {
            val code = recoveryRepo.issue(email, ttlMinutes)
            val (subject, text, html) = getVerificationEmailContent(code, ttlMinutes, user.systemLanguage)
            runCatching { mailer.send(to = email, subject = subject, text = text, html = html) }
        }
        call.respond(HttpStatusCode.OK, mapOf("ok" to true))
    }
}

private fun getPasswordRecoveryEmailContent(code: String, ttlMinutes: Long, language: SystemLanguage): Triple<String, String, String> {
    return when (language) {
        SystemLanguage.RUSSIAN -> Triple(
            "Код восстановления пароля",
            """
                Ваш код восстановления пароля: $code
                Код действителен в течение $ttlMinutes минут.
                Если вы не запрашивали восстановление пароля, проигнорируйте это письмо.
            """.trimIndent(),
            """
                <p>Ваш код восстановления пароля: <b>$code</b></p>
                <p>Код действителен в течение $ttlMinutes минут.</p>
                <p>Если вы не запрашивали восстановление пароля, проигнорируйте это письмо.</p>
            """.trimIndent()
        )
        SystemLanguage.UZBEK -> Triple(
            "Parolni tiklash kodi",
            """
                Parolni tiklash kodingiz: $code
                Kod $ttlMinutes daqiqa davomida amal qiladi.
                Agar siz parolni tiklashni so'ramagan bo'lsangiz, bu xabarni e'tiborsiz qoldiring.
            """.trimIndent(),
            """
                <p>Parolni tiklash kodingiz: <b>$code</b></p>
                <p>Kod $ttlMinutes daqiqa davomida amal qiladi.</p>
                <p>Agar siz parolni tiklashni so'ramagan bo'lsangiz, bu xabarni e'tiborsiz qoldiring.</p>
            """.trimIndent()
        )
        SystemLanguage.CHINESE -> Triple(
            "密码恢复验证码",
            """
                您的密码恢复验证码是：$code
                验证码有效期为 $ttlMinutes 分钟。
                如果您没有请求密码恢复，请忽略此邮件。
            """.trimIndent(),
            """
                <p>您的密码恢复验证码是：<b>$code</b></p>
                <p>验证码有效期为 $ttlMinutes 分钟。</p>
                <p>如果您没有请求密码恢复，请忽略此邮件。</p>
            """.trimIndent()
        )
        SystemLanguage.HINDI -> Triple(
            "पासवर्ड रिकवरी कोड",
            """
                आपका पासवर्ड रिकवरी कोड है: $code
                यह कोड $ttlMinutes मिनट के लिए वैध है।
                यदि आपने पासवर्ड रिकवरी का अनुरोध नहीं किया है, तो इस ईमेल को अनदेखा करें।
            """.trimIndent(),
            """
                <p>आपका पासवर्ड रिकवरी कोड है: <b>$code</b></p>
                <p>यह कोड $ttlMinutes मिनट के लिए वैध है।</p>
                <p>यदि आपने पासवर्ड रिकवरी का अनुरोध नहीं किया है, तो इस ईमेल को अनदेखा करें।</p>
            """.trimIndent()
        )
        SystemLanguage.TAJIK -> Triple(
            "Рамзи барқарорсозии парол",
            """
                Рамзи барқарорсозии пароли шумо: $code
                Рамз $ttlMinutes дақиқа эътибор дорад.
                Агар шумо барқарорсозии паролро дархост накарда бошед, ин паёмро рад кунед.
            """.trimIndent(),
            """
                <p>Рамзи барқарорсозии пароли шумо: <b>$code</b></p>
                <p>Рамз $ttlMinutes дақиқа эътибор дорад.</p>
                <p>Агар шумо барқарорсозии паролро дархост накарда бошед, ин паёмро рад кунед.</p>
            """.trimIndent()
        )
        SystemLanguage.ENGLISH -> Triple(
            "Your password recovery code",
            """
                Your password recovery code is: $code
                It expires in $ttlMinutes minutes.
                If you didn't request this, you can ignore this email.
            """.trimIndent(),
            """
                <p>Your password recovery code is: <b>$code</b></p>
                <p>It expires in $ttlMinutes minutes.</p>
                <p>If you didn't request this, you can ignore this email.</p>
            """.trimIndent()
        )
    }
}

private fun getVerificationEmailContent(code: String, ttlMinutes: Long, language: SystemLanguage): Triple<String, String, String> {
    return when (language) {
        SystemLanguage.RUSSIAN -> Triple(
            "Код подтверждения регистрации",
            """
                Ваш код подтверждения регистрации: $code
                Код действителен в течение $ttlMinutes минут.
                Если вы не регистрировались на нашем сайте, проигнорируйте это письмо.
            """.trimIndent(),
            """
                <p>Ваш код подтверждения регистрации: <b>$code</b></p>
                <p>Код действителен в течение $ttlMinutes минут.</p>
                <p>Если вы не регистрировались на нашем сайте, проигнорируйте это письмо.</p>
            """.trimIndent()
        )
        SystemLanguage.UZBEK -> Triple(
            "Ro'yxatdan o'tishni tasdiqlash kodi",
            """
                Ro'yxatdan o'tishni tasdiqlash kodingiz: $code
                Kod $ttlMinutes daqiqa davomida amal qiladi.
                Agar siz ro'yxatdan o'tmagan bo'lsangiz, bu xabarni e'tiborsiz qoldiring.
            """.trimIndent(),
            """
                <p>Ro'yxatdan o'tishni tasdiqlash kodingiz: <b>$code</b></p>
                <p>Kod $ttlMinutes daqiqa davomida amal qiladi.</p>
                <p>Agar siz ro'yxatdan o'tmagan bo'lsangiz, bu xabarni e'tiborsiz qoldiring.</p>
            """.trimIndent()
        )
        SystemLanguage.CHINESE -> Triple(
            "注册验证码",
            """
                您的注册验证码是：$code
                验证码有效期为 $ttlMinutes 分钟。
                如果您没有注册，请忽略此邮件。
            """.trimIndent(),
            """
                <p>您的注册验证码是：<b>$code</b></p>
                <p>验证码有效期为 $ttlMinutes 分钟。</p>
                <p>如果您没有注册，请忽略此邮件。</p>
            """.trimIndent()
        )
        SystemLanguage.HINDI -> Triple(
            "पंजीकरण सत्यापन कोड",
            """
                आपका पंजीकरण सत्यापन कोड है: $code
                यह कोड $ttlMinutes मिनट के लिए वैध है।
                यदि आपने पंजीकरण नहीं किया है, तो इस ईमेल को अनदेखा करें।
            """.trimIndent(),
            """
                <p>आपका पंजीकरण सत्यापन कोड है: <b>$code</b></p>
                <p>यह कोड $ttlMinutes मिनट के लिए वैध है।</p>
                <p>यदि आपने पंजीकरण नहीं किया है, तो इस ईमेल को अनदेखा करें।</p>
            """.trimIndent()
        )
        SystemLanguage.TAJIK -> Triple(
            "Рамзи тасдиқи бақайдгирӣ",
            """
                Рамзи тасдиқи бақайдгирии шумо: $code
                Рамз $ttlMinutes дақиқа эътибор дорад.
                Агар шумо бақайдгирӣ нашуда бошед, ин паёмро рад кунед.
            """.trimIndent(),
            """
                <p>Рамзи тасдиқи бақайдгирии шумо: <b>$code</b></p>
                <p>Рамз $ttlMinutes дақиқа эътибор дорад.</p>
                <p>Агар шумо бақайдгирӣ нашуда бошед, ин паёмро рад кунед.</p>
            """.trimIndent()
        )
        SystemLanguage.ENGLISH -> Triple(
            "Registration verification code",
            """
                Your registration verification code is: $code
                The code is valid for $ttlMinutes minutes.
                If you didn't register, please ignore this email.
            """.trimIndent(),
            """
                <p>Your registration verification code is: <b>$code</b></p>
                <p>The code is valid for $ttlMinutes minutes.</p>
                <p>If you didn't register, please ignore this email.</p>
            """.trimIndent()
        )
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

@Serializable
data class EmailVerificationRequest(val email: String, val code: String)
@Serializable
data class EmailVerificationResponse(
    val ok: Boolean,
    val error: String? = null,
    val reason: String? = null
)
@Serializable
data class ResendVerificationRequest(val email: String)