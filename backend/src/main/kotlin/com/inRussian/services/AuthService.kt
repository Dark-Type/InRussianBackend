package com.inRussian.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.inRussian.config.JWTConfig
import com.inRussian.models.users.SystemLanguage
import com.inRussian.models.users.User
import com.inRussian.models.users.UserInfo
import com.inRussian.models.users.UserRole
import com.inRussian.models.users.UserStatus
import com.inRussian.repositories.PasswordRecoveryTokenRepository
import com.inRussian.repositories.UserRepository
import com.inRussian.requests.auth.LoginRequest
import com.inRussian.requests.users.*
import com.inRussian.responses.auth.LoginResponse
import com.inRussian.services.mailer.Mailer
import io.ktor.server.application.*
import org.mindrot.jbcrypt.BCrypt
import java.util.*

class AuthService(
    private val userRepository: UserRepository,
    private val application: Application,
    private val mailer: Mailer,
    private val recoveryRepo: PasswordRecoveryTokenRepository
) {
    private val jwtSecret =
        application.environment.config.propertyOrNull("jwt.secret")?.getString() ?: "your-secret-key"
    private val jwtAudience =
        application.environment.config.propertyOrNull("jwt.audience")?.getString() ?: "inrussian-api"
    private val jwtDomain =
        application.environment.config.propertyOrNull("jwt.domain")?.getString() ?: "http://localhost:8080/"

    suspend fun registerStudent(request: StudentRegisterRequest): Result<LoginResponse> {
        if (userRepository.findByEmail(request.email) != null) {
            return Result.failure(Exception("User with this email already exists"))
        }

        val user = User(
            id = UUID.randomUUID().toString(),
            email = request.email,
            passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt(12)),
            phone = request.phone,
            role = UserRole.STUDENT,
            systemLanguage = request.systemLanguage,
            status = UserStatus.PENDING_VERIFICATION
        )

        val createdUser = userRepository.create(user)

        // Send verification email
        sendVerificationEmail(createdUser.email, createdUser.systemLanguage)

        return createToken(createdUser)
    }

    suspend fun registerStaff(request: StaffRegisterRequest): Result<LoginResponse> {
        if (request.role !in listOf(UserRole.EXPERT, UserRole.CONTENT_MODERATOR, UserRole.ADMIN)) {
            return Result.failure(Exception("Invalid role for staff registration"))
        }

        if (userRepository.findByEmail(request.email) != null) {
            return Result.failure(Exception("User with this email already exists"))
        }

        val user = User(
            id = UUID.randomUUID().toString(),
            email = request.email,
            passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt(12)),
            phone = request.phone,
            role = request.role,
            systemLanguage = request.systemLanguage,
            status = UserStatus.PENDING_VERIFICATION
        )
        return register(user)
    }

    suspend fun register(user: User): Result<LoginResponse> {
        val createdUser = userRepository.create(user)
        return createToken(createdUser)
    }

    private suspend fun sendVerificationEmail(email: String, language: SystemLanguage, ttlMinutes: Long = 10) {
        val code = recoveryRepo.issue(email, ttlMinutes)
        val (subject, text, html) = getVerificationEmailContent(code, ttlMinutes, language)

        runCatching {
            mailer.send(to = email, subject = subject, text = text, html = html)
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

    fun createToken(user: User): Result<LoginResponse> {
        val accessToken = JWTConfig.generateAccessToken(
            userId = user.id,
            email = user.email,
            role = user.role,
            status = user.status,
            secret = jwtSecret,
            audience = jwtAudience,
            issuer = jwtDomain
        )
        val refreshToken = JWTConfig.generateRefreshToken(
            userId = user.id,
            secret = jwtSecret,
            audience = jwtAudience,
            issuer = jwtDomain
        )

        return Result.success(
            LoginResponse(
                accessToken = accessToken,
                refreshToken = refreshToken,
                user = UserInfo(
                    id = user.id,
                    email = user.email,
                    role = user.role.name,
                    phone = user.phone,
                    systemLanguage = user.systemLanguage.name,
                    status = user.status.name
                )
            )
        )
    }

    suspend fun refreshAccessToken(refreshToken: String): Result<String> {
        return try {
            val verifier = JWT
                .require(Algorithm.HMAC256(jwtSecret))
                .withAudience(jwtAudience)
                .withIssuer(jwtDomain)
                .build()
            val principal = verifier.verify(refreshToken)

            if (principal.getClaim("type").asString() != "refresh") {
                return Result.failure(Exception("Токен не является refresh-токеном"))
            }

            val userId = principal.getClaim("userId").asString()
            val user = userRepository.findById(userId)
                ?: return Result.failure(Exception("Пользователь не найден"))

            val newAccessToken = JWTConfig.generateAccessToken(
                userId = user.id,
                email = user.email,
                role = user.role,
                status = user.status,
                secret = jwtSecret,
                audience = jwtAudience,
                issuer = jwtDomain
            )
            Result.success(newAccessToken)
        } catch (_: Exception) {
            Result.failure(Exception("Недействительный или истёкший refresh-токен"))
        }
    }

    suspend fun login(request: LoginRequest): Result<LoginResponse> {
        val user = userRepository.findByEmail(request.email)
            ?: return Result.failure(Exception("Invalid email or password"))
        when (user.status) {
            UserStatus.SUSPENDED -> return Result.failure(Exception("Account is suspended"))
            UserStatus.DEACTIVATED -> return Result.failure(Exception("Account is deactivated"))
            UserStatus.PENDING_VERIFICATION -> {}
            UserStatus.ACTIVE -> {}
        }

        if (!BCrypt.checkpw(request.password, user.passwordHash)) {
            return Result.failure(Exception("Invalid email or password"))
        }

        userRepository.updateLastActivity(user.id)

        return createToken(user)
    }

    suspend fun createInitialAdmin(): Result<User> {
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            return Result.failure(Exception("Admin already exists"))
        }

        val adminEmail = application.environment.config.propertyOrNull("admin.email")?.getString()
            ?: return Result.failure(Exception("Admin email not configured"))

        val adminPassword = application.environment.config.propertyOrNull("admin.password")?.getString()
            ?: return Result.failure(Exception("Admin password not configured"))

        val admin = User(
            id = UUID.randomUUID().toString(),
            email = adminEmail,
            passwordHash = BCrypt.hashpw(adminPassword, BCrypt.gensalt(12)),
            role = UserRole.ADMIN,
            systemLanguage = com.inRussian.models.users.SystemLanguage.ENGLISH,
            status = UserStatus.ACTIVE
        )

        return Result.success(userRepository.create(admin))
    }


    suspend fun updateUserStatus(userId: String, status: UserStatus): Result<Boolean> {
        val updated = userRepository.updateStatus(userId, status)
        return if (updated) {
            Result.success(true)
        } else {
            Result.failure(Exception("Failed to update user status"))
        }
    }
}