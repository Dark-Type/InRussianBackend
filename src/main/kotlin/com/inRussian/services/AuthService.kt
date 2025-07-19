package com.inRussian.services

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
   
        val createdUser = userRepository.create(user)
        val token = JWTConfig.generateToken(
            userId = createdUser.id,
            email = createdUser.email,
            role = createdUser.role,
            secret = jwtSecret,
            audience = jwtAudience,
            issuer = jwtDomain
        )

        return Result.success(
            LoginResponse(
                accessToken = token,
                user = UserInfo(
                    id = createdUser.id,
                    email = createdUser.email,
                    role = createdUser.role.name,
                    phone = createdUser.phone,
                    systemLanguage = createdUser.systemLanguage.name,
                    status = createdUser.status.name
                )
            )
        )
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
            status = UserStatus.ACTIVE
        )

        val createdUser = userRepository.create(user)
        val token = JWTConfig.generateToken(
            userId = createdUser.id,
            email = createdUser.email,
            role = createdUser.role,
            secret = jwtSecret,
            audience = jwtAudience,
            issuer = jwtDomain
        )

        return Result.success(
            LoginResponse(
                accessToken = token,
                user = UserInfo(
                    id = createdUser.id,
                    email = createdUser.email,
                    role = createdUser.role.name,
                    phone = createdUser.phone,
                    systemLanguage = createdUser.systemLanguage.name,
                    status = createdUser.status.name
                )
            )
        )
    }

    suspend fun login(request: LoginRequest): Result<LoginResponse> {
        val user = userRepository.findByEmail(request.email)
            ?: return Result.failure(Exception("Invalid email or password"))

        when (user.status) {
            UserStatus.SUSPENDED -> return Result.failure(Exception("Account is suspended"))
            UserStatus.DEACTIVATED -> return Result.failure(Exception("Account is deactivated"))
            UserStatus.PENDING_VERIFICATION -> return Result.failure(Exception("Please verify your email first"))
            UserStatus.ACTIVE -> { /* Continue with login */
            }
        }

        if (!BCrypt.checkpw(request.password, user.passwordHash)) {
            return Result.failure(Exception("Invalid email or password"))
        }

        userRepository.updateLastActivity(user.id)

        val token = JWTConfig.generateToken(
            userId = user.id,
            email = user.email,
            role = user.role,
            secret = jwtSecret,
            audience = jwtAudience,
            issuer = jwtDomain
        )

        return Result.success(
            LoginResponse(
                accessToken = token,
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