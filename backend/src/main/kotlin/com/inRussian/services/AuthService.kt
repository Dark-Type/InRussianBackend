package com.inRussian.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.inRussian.config.JWTConfig
import com.inRussian.models.users.User
import com.inRussian.models.users.UserInfo
import com.inRussian.models.users.UserRole
import com.inRussian.models.users.UserStatus
import com.inRussian.repositories.UserRepository
import com.inRussian.requests.auth.LoginRequest
import com.inRussian.requests.users.*
import com.inRussian.responses.auth.LoginResponse
import io.ktor.server.application.*
import org.mindrot.jbcrypt.BCrypt
import java.util.*

class AuthService(
    private val userRepository: UserRepository,
    private val application: Application
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
            status = UserStatus.ACTIVE
        )

        return register(user)
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
        println("valid 1")
        when (user.status) {
            UserStatus.SUSPENDED -> return Result.failure(Exception("Account is suspended"))
            UserStatus.DEACTIVATED -> return Result.failure(Exception("Account is deactivated"))
            UserStatus.PENDING_VERIFICATION -> {}
            UserStatus.ACTIVE -> {}
        }
        println("valid 2")

        if (!BCrypt.checkpw(request.password, user.passwordHash)) {
            return Result.failure(Exception("Invalid email or password"))
        }
        println("valid 3")

        userRepository.updateLastActivity(user.id)
        println("valid 4")

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