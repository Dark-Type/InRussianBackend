package com.inRussian.routes

import com.inRussian.config.getUserEmail
import com.inRussian.config.getUserId
import com.inRussian.config.getUserRole
import com.inRussian.requests.auth.LoginRequest
import com.inRussian.requests.users.StaffRegisterRequest
import com.inRussian.requests.users.StudentRegisterRequest
import com.inRussian.responses.auth.AdminCreatedResponse
import com.inRussian.responses.auth.MessageResponse
import com.inRussian.responses.auth.RefreshTokenRequest
import com.inRussian.responses.auth.UserInfoData
import com.inRussian.responses.auth.UserInfoResponse
import com.inRussian.responses.common.ErrorResponse
import com.inRussian.services.AuthService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(authService: AuthService) {
    route("/auth") {
        post("/student/register") {
            val request = call.receive<StudentRegisterRequest>()
            val result = authService.registerStudent(request)

            if (result.isSuccess) {
                val response = result.getOrNull()
                call.respond(HttpStatusCode.Created, response!!)
            } else {
                val error = result.exceptionOrNull()
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = error?.message ?: "Registration failed",
                        code = null,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
        post("/refresh") {
            val request = call.receive<RefreshTokenRequest>()
            val result = authService.refreshAccessToken(request.refreshToken)

            if (result.isSuccess) {
                val accessToken = result.getOrNull()
                call.respond(HttpStatusCode.OK, mapOf("accessToken" to accessToken))
            } else {
                val error = result.exceptionOrNull()
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse(
                        success = false,
                        error = error?.message ?: "Refresh failed",
                        code = null,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
        post("/staff/register") {
            val request = call.receive<StaffRegisterRequest>()
            val result = authService.registerStaff(request)

            if (result.isSuccess) {
                val response = result.getOrNull()
                call.respond(HttpStatusCode.Created, response!!)
            } else {
                val error = result.exceptionOrNull()
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = error?.message ?: "Registration failed",
                        code = null,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            val result = authService.login(request)

            if (result.isSuccess) {
                val response = result.getOrNull()
                call.respond(HttpStatusCode.OK, response!!)
            } else {
                val error = result.exceptionOrNull()
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse(
                        success = false,
                        error = error?.message ?: "Login failed",
                        code = null,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }

        post("/admin/create-initial") {
            authService.createInitialAdmin()
                .onSuccess { admin ->
                    call.respond(
                        HttpStatusCode.Created,
                        AdminCreatedResponse(
                            success = true,
                            email = admin.email,
                            message = "Initial admin created successfully",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
                .onFailure { error ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(
                            success = false,
                            error = error.message ?: "Failed to create admin",
                            code = null,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
        }


        authenticate("auth-jwt") {
            post("/logout") {
                call.respond(
                    HttpStatusCode.OK,
                    MessageResponse(
                        success = true,
                        message = "Logged out successfully",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }

            get("/me") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.getUserId()!!
                val userRole = principal.getUserRole()!!
                val userEmail = principal.getUserEmail()!!

                call.respond(
                    HttpStatusCode.OK,
                    UserInfoResponse(
                        success = true,
                        user = UserInfoData(
                            id = userId,
                            email = userEmail,
                            role = userRole.name
                        ),
                        message = "User info retrieved successfully",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }
}