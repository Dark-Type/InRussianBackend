package com.inRussian.services.v3

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.inRussian.config.JWTConfig
import com.inRussian.models.users.SystemLanguage
import com.inRussian.models.users.User
import com.inRussian.models.users.UserInfo
import com.inRussian.models.users.UserRole
import com.inRussian.models.users.UserStatus
import com.inRussian.repositories.EmailTokenRepository
import com.inRussian.repositories.UsersRepository
import com.inRussian.requests.auth.LoginRequest
import com.inRussian.requests.users.*
import com.inRussian.responses.auth.LoginResponse
import com.inRussian.services.mailer.Mailer
import com.inRussian.utils.exceptions.AccountDeactivatedException
import com.inRussian.utils.exceptions.AccountSuspendedException
import com.inRussian.utils.exceptions.AdminAlreadyExistsException
import com.inRussian.utils.exceptions.ConfigurationException
import com.inRussian.utils.exceptions.InvalidCredentialsException
import com.inRussian.utils.exceptions.InvalidTokenException
import com.inRussian.utils.exceptions.UserAlreadyExistsException
import com.inRussian.utils.exceptions.UserNotFoundException
import com.inRussian.utils.validation.AuthValidator
import com.inRussian.utils.validation.ValidationException
import io.ktor.server.application.*
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory
import java.util.*

interface AuthService {
    suspend fun registerStudent(request: StudentRegisterRequest): Result<LoginResponse>
    suspend fun registerStaff(request: StaffRegisterRequest): Result<LoginResponse>
    suspend fun register(user: User): Result<LoginResponse>
    fun createToken(user: User): Result<LoginResponse>
    suspend fun refreshAccessToken(refreshToken: String): Result<String>
    suspend fun login(request: LoginRequest): Result<LoginResponse>
    suspend fun createInitialAdmin(): Result<User>
    suspend fun updateUserStatus(userId: String, status: UserStatus): Result<Boolean>
}

class AuthServiceImplementation(
    private val userRepository: UsersRepository,
    private val application: Application,
    private val mailer: Mailer,
    private val recoveryRepo: EmailTokenRepository
) : AuthService {

    private val logger = LoggerFactory.getLogger(AuthServiceImplementation::class.java)

    private val jwtSecret: String by lazy {
        application.environment.config.propertyOrNull("jwt.secret")?.getString()
            ?: throw ConfigurationException("jwt.secret")
    }
    private val jwtAudience: String by lazy {
        application.environment.config.propertyOrNull("jwt.audience")?.getString() ?: "inrussian-api"
    }
    private val jwtDomain: String by lazy {
        application.environment.config.propertyOrNull("jwt.domain")?.getString() ?: "http://localhost:8080/"
    }

    override suspend fun registerStudent(request: StudentRegisterRequest): Result<LoginResponse> {
        validateStudentRequest(request)

        return runCatching {
            userRepository.findByEmail(request.email)?.let {
                throw UserAlreadyExistsException(request.email)
            }

            val user = User(
                id = UUID.randomUUID().toString(),
                email = request.email.lowercase().trim(),
                passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt(12)),
                phone = request.phone?.trim(),
                role = UserRole.STUDENT,
                systemLanguage = request.systemLanguage,
                name = request.name,
                surname = request.surname,
                patronymic = request.patronymic,
                status = UserStatus.PENDING_VERIFICATION
            )

            val createdUser = userRepository.create(user)
            sendVerificationEmailSafe(createdUser.email, createdUser.systemLanguage)
            createToken(createdUser).getOrThrow()
        }
    }

    override suspend fun registerStaff(request: StaffRegisterRequest): Result<LoginResponse> {
        validateStaffRequest(request)

        return runCatching {
            userRepository.findByEmail(request.email)?.let {
                throw UserAlreadyExistsException(request.email)
            }

            val user = User(
                id = UUID.randomUUID().toString(),
                email = request.email.lowercase().trim(),
                passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt(12)),
                phone = request.phone?.trim(),
                role = request.role,
                systemLanguage = request.systemLanguage,
                name = request.name,
                surname = request.surname,
                patronymic = request.patronymic,
                status = UserStatus.PENDING_VERIFICATION
            )

            val createdUser = userRepository.create(user)
            createToken(createdUser).getOrThrow()
        }
    }

    override suspend fun register(user: User): Result<LoginResponse> {
        return runCatching {
            val createdUser = userRepository.create(user)
            createToken(createdUser).getOrThrow()
        }
    }

    override fun createToken(user: User): Result<LoginResponse> {
        return runCatching {
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
        }
    }

    override suspend fun refreshAccessToken(refreshToken: String): Result<String> {
        val errors = AuthValidator.validateRefreshToken(refreshToken)
        if (errors.isNotEmpty()) throw ValidationException(errors)

        return runCatching {
            val verifier = JWT
                .require(Algorithm.HMAC256(jwtSecret))
                .withAudience(jwtAudience)
                .withIssuer(jwtDomain)
                .build()

            val principal = try {
                verifier.verify(refreshToken)
            } catch (e: JWTVerificationException) {
                throw InvalidTokenException("Invalid or expired refresh token")
            }

            if (principal.getClaim("type").asString() != "refresh") {
                throw InvalidTokenException("Token is not a refresh token")
            }

            val userId = principal.getClaim("userId").asString()
                ?: throw InvalidTokenException("Token missing userId claim")

            val user = userRepository.findById(userId)
                ?: throw UserNotFoundException(userId)

            JWTConfig.generateAccessToken(
                userId = user.id,
                email = user.email,
                role = user.role,
                status = user.status,
                secret = jwtSecret,
                audience = jwtAudience,
                issuer = jwtDomain
            )
        }
    }

    override suspend fun login(request: LoginRequest): Result<LoginResponse> {

        validateLoginRequest(request)

        return runCatching {
            val user = userRepository.findByEmail(request.email.lowercase().trim())
                ?: throw InvalidCredentialsException()

            when (user.status) {
                UserStatus.SUSPENDED -> throw AccountSuspendedException()
                UserStatus.DEACTIVATED -> throw AccountDeactivatedException()
                UserStatus.PENDING_VERIFICATION, UserStatus.ACTIVE -> { /* proceed */
                }
            }

            logger.info(
                "Login debug: email={}, dbHashPrefix={}, incomingPwLen={}, matches={}",
                user.email,
                user.passwordHash.take(15),
                request.password.length,
                BCrypt.checkpw(request.password, user.passwordHash)
            )
            if (!BCrypt.checkpw(request.password, user.passwordHash)) {
                throw InvalidCredentialsException()
            }

            userRepository.updateLastActivity(user.id)
            createToken(user).getOrThrow()
        }
    }

    override suspend fun createInitialAdmin(): Result<User> {
        return runCatching {
            if (userRepository.existsByRole(UserRole.ADMIN)) {
                throw AdminAlreadyExistsException()
            }

            val adminEmail = application.environment.config.propertyOrNull("admin.email")?.getString()
                ?: throw ConfigurationException("admin.email")

            val adminPassword = application.environment.config.propertyOrNull("admin.password")?.getString()
                ?: throw ConfigurationException("admin.password")

            val admin = User(
                id = UUID.randomUUID().toString(),
                email = adminEmail.lowercase().trim(),
                passwordHash = BCrypt.hashpw(adminPassword, BCrypt.gensalt(12)),
                role = UserRole.ADMIN,
                systemLanguage = SystemLanguage.ENGLISH,
                status = UserStatus.ACTIVE,
                name = "Первый",
                surname = "Администратор"
            )

            userRepository.create(admin)
        }
    }

    override suspend fun updateUserStatus(userId: String, status: UserStatus): Result<Boolean> {
        return runCatching {
            val updated = userRepository.updateStatus(userId, status)
            if (!updated) throw UserNotFoundException(userId)
            true
        }
    }

    private fun validateStudentRequest(request: StudentRegisterRequest) {
        val errors = mutableListOf<com.inRussian.utils.validation.FieldError>()
        errors.addAll(AuthValidator.validateEmail(request.email))
        errors.addAll(AuthValidator.validatePassword(request.password))
        errors.addAll(AuthValidator.validatePhone(request.phone))
        if (errors.isNotEmpty()) throw ValidationException(errors)
    }

    private fun validateStaffRequest(request: StaffRegisterRequest) {
        val errors = mutableListOf<com.inRussian.utils.validation.FieldError>()
        errors.addAll(AuthValidator.validateEmail(request.email))
        errors.addAll(AuthValidator.validatePassword(request.password))
        errors.addAll(AuthValidator.validatePhone(request.phone))
        errors.addAll(AuthValidator.validateStaffRole(request.role))
        if (errors.isNotEmpty()) throw ValidationException(errors)
    }

    private fun validateLoginRequest(request: LoginRequest) {
        val errors = mutableListOf<com.inRussian.utils.validation.FieldError>()
        errors.addAll(AuthValidator.validateEmail(request.email))
        if (request.password.isBlank()) {
            errors.add(com.inRussian.utils.validation.FieldError("password", "REQUIRED", "Password is required"))
        }
        if (errors.isNotEmpty()) throw ValidationException(errors)
    }

    private suspend fun sendVerificationEmailSafe(email: String, language: SystemLanguage, ttlMinutes: Long = 10) {
        runCatching {
            val code = recoveryRepo.issue(email, ttlMinutes)
            val (subject, text, html) = getVerificationEmailContent(code, ttlMinutes, language)
            mailer.send(to = email, subject = subject, text = text, html = html)
        }.onFailure { e ->
            logger.error("Failed to send verification email to $email", e)
        }
    }

    private fun getVerificationEmailContent(
        code: String,
        ttlMinutes: Long,
        language: SystemLanguage
    ): Triple<String, String, String> {
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
}