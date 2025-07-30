package com.inRussian.routes

import com.inRussian.models.users.UserRole
import com.inRussian.models.users.UserStatus
import com.inRussian.requests.admin.UpdateUserRequest
import com.inRussian.requests.users.*
import com.inRussian.responses.common.ErrorResponse
import com.inRussian.responses.auth.MessageResponse
import com.inRussian.responses.auth.StaffProfileResponse
import com.inRussian.responses.auth.UserProfileResponse
import com.inRussian.services.AdminService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import java.time.LocalDate
import java.time.format.DateTimeParseException

fun Route.adminRoutes(adminService: AdminService) {
    authenticate("admin-jwt") {
        route("/admin") {
            route("/users") {
                post("/staff") {
                    val request = call.receive<StaffRegisterRequest>()
                    val result = adminService.registerStaff(request)

                    if (result.isSuccess) {
                        val response = result.getOrNull()
                        call.respond(HttpStatusCode.Created, response!!)
                    } else {
                        val error = result.exceptionOrNull()
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                success = false,
                                error = error?.message ?: "Staff registration failed",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
                get {
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                    val role = call.request.queryParameters["role"]?.let {
                        try {
                            UserRole.valueOf(it)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    val sortBy = call.request.queryParameters["sortBy"] ?: "createdAt"
                    val sortOrder = call.request.queryParameters["sortOrder"] ?: "desc"

                    val createdFrom = call.request.queryParameters["createdFrom"]?.let {
                        try {
                            LocalDate.parse(it)
                        } catch (e: DateTimeParseException) {
                            null
                        }
                    }
                    val createdTo = call.request.queryParameters["createdTo"]?.let {
                        try {
                            LocalDate.parse(it)
                        } catch (e: DateTimeParseException) {
                            null
                        }
                    }

                    val result = adminService.getAllUsers(page, size, role, createdFrom, createdTo, sortBy, sortOrder)

                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                    } else {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                success = false,
                                error = result.exceptionOrNull()?.message ?: "Failed to get users",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }

                get("/count") {
                    val role = call.request.queryParameters["role"]?.let {
                        try {
                            UserRole.valueOf(it)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    val createdFrom = call.request.queryParameters["createdFrom"]?.let {
                        try {
                            LocalDate.parse(it)
                        } catch (e: DateTimeParseException) {
                            null
                        }
                    }
                    val createdTo = call.request.queryParameters["createdTo"]?.let {
                        try {
                            LocalDate.parse(it)
                        } catch (e: DateTimeParseException) {
                            null
                        }
                    }

                    val result = adminService.getUsersCount(role, createdFrom, createdTo)

                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, mapOf("count" to result.getOrNull()!!))
                    } else {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                success = false,
                                error = result.exceptionOrNull()?.message ?: "Failed to get user count",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }

                get("/{userId}") {
                    val userId = call.parameters["userId"]
                    if (userId == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                success = false,
                                error = "Missing user ID",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        return@get
                    }

                    val result = adminService.getUserById(userId)

                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(
                                success = false,
                                error = result.exceptionOrNull()?.message ?: "User not found",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }

                put("/{userId}") {
                    val userId = call.parameters["userId"]
                    if (userId == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                success = false,
                                error = "Missing user ID",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        return@put
                    }

                    val request = call.receive<UpdateUserRequest>()
                    val result = adminService.updateUser(userId, request)

                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                    } else {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                success = false,
                                error = result.exceptionOrNull()?.message ?: "Failed to update user",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }

                put("/{userId}/status") {
                    val userId = call.parameters["userId"]
                    if (userId == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                success = false,
                                error = "Missing user ID",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        return@put
                    }

                    val statusRequest = call.receive<Map<String, String>>()
                    val status = statusRequest["status"]?.let {
                        try {
                            UserStatus.valueOf(it)
                        } catch (e: IllegalArgumentException) {
                            null
                        }
                    }

                    if (status == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                success = false,
                                error = "Invalid status",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        return@put
                    }

                    val result = adminService.updateUserStatus(userId, status)

                    if (result.isSuccess) {
                        call.respond(
                            HttpStatusCode.OK,
                            MessageResponse(
                                success = true,
                                message = "User status updated successfully",
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                success = false,
                                error = result.exceptionOrNull()?.message ?: "Failed to update status",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
                route("/{userId}/profile/user") {
                    post {
                        val userId = call.parameters["userId"]
                        if (userId == null) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(
                                    success = false,
                                    error = "Missing user ID",
                                    code = null,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                            return@post
                        }

                        val request = call.receive<CreateUserProfileRequest>()
                        val result = adminService.createUserProfileForUser(userId, request)

                        if (result.isSuccess) {
                            val profile = result.getOrNull()
                            call.respond(
                                HttpStatusCode.Created,
                                UserProfileResponse(
                                    profile = profile!!,
                                    message = "User profile created successfully"
                                )
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(
                                    success = false,
                                    error = result.exceptionOrNull()?.message ?: "Failed to create user profile",
                                    code = null,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }
                    }

                    get {
                        val userId = call.parameters["userId"]
                        if (userId == null) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(
                                    success = false,
                                    error = "Missing user ID",
                                    code = null,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                            return@get
                        }

                        val result = adminService.getUserProfileByUserId(userId)

                        if (result.isSuccess) {
                            val profile = result.getOrNull()
                            call.respond(
                                HttpStatusCode.OK,
                                UserProfileResponse(
                                    profile = profile!!,
                                    message = "User profile retrieved successfully"
                                )
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.NotFound,
                                ErrorResponse(
                                    success = false,
                                    error = result.exceptionOrNull()?.message ?: "User profile not found",
                                    code = null,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }
                    }

                    put {
                        val userId = call.parameters["userId"]
                        if (userId == null) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(
                                    success = false,
                                    error = "Missing user ID",
                                    code = null,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                            return@put
                        }

                        val request = call.receive<UpdateUserProfileRequest>()
                        val result = adminService.updateUserProfileForUser(userId, request)

                        if (result.isSuccess) {
                            val profile = result.getOrNull()
                            call.respond(
                                HttpStatusCode.OK,
                                UserProfileResponse(
                                    profile = profile!!,
                                    message = "User profile updated successfully"
                                )
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(
                                    success = false,
                                    error = result.exceptionOrNull()?.message ?: "Failed to update user profile",
                                    code = null,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }
                    }

                    delete {
                        val userId = call.parameters["userId"]
                        if (userId == null) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(
                                    success = false,
                                    error = "Missing user ID",
                                    code = null,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                            return@delete
                        }

                        val result = adminService.deleteUserProfileForUser(userId)

                        if (result.isSuccess) {
                            call.respond(
                                HttpStatusCode.OK,
                                MessageResponse(
                                    success = true,
                                    message = "User profile deleted successfully",
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.NotFound,
                                ErrorResponse(
                                    success = false,
                                    error = result.exceptionOrNull()?.message ?: "User profile not found",
                                    code = null,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }

                // Staff Profile management
                route("/{userId}/profile/staff") {
                    post {
                        val userId = call.parameters["userId"]
                        if (userId == null) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(
                                    success = false,
                                    error = "Missing user ID",
                                    code = null,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                            return@post
                        }

                        val request = call.receive<CreateStaffProfileRequest>()
                        val result = adminService.createStaffProfileForUser(userId, request)

                        if (result.isSuccess) {
                            val profile = result.getOrNull()
                            call.respond(
                                HttpStatusCode.Created,
                                StaffProfileResponse(
                                    profile = profile!!,
                                    message = "Staff profile created successfully"
                                )
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(
                                    success = false,
                                    error = result.exceptionOrNull()?.message ?: "Failed to create staff profile",
                                    code = null,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }
                    }

                    get {
                        val userId = call.parameters["userId"]
                        if (userId == null) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(
                                    success = false,
                                    error = "Missing user ID",
                                    code = null,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                            return@get
                        }

                        val result = adminService.getStaffProfileByUserId(userId)

                        if (result.isSuccess) {
                            val profile = result.getOrNull()
                            call.respond(
                                HttpStatusCode.OK,
                                StaffProfileResponse(
                                    profile = profile!!,
                                    message = "Staff profile retrieved successfully"
                                )
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.NotFound,
                                ErrorResponse(
                                    success = false,
                                    error = result.exceptionOrNull()?.message ?: "Staff profile not found",
                                    code = null,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }
                    }

                    put {
                        val userId = call.parameters["userId"]
                        if (userId == null) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(
                                    success = false,
                                    error = "Missing user ID",
                                    code = null,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                            return@put
                        }

                        val request = call.receive<UpdateStaffProfileRequest>()
                        val result = adminService.updateStaffProfileForUser(userId, request)

                        if (result.isSuccess) {
                            val profile = result.getOrNull()
                            call.respond(
                                HttpStatusCode.OK,
                                StaffProfileResponse(
                                    profile = profile!!,
                                    message = "Staff profile updated successfully"
                                )
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(
                                    success = false,
                                    error = result.exceptionOrNull()?.message ?: "Failed to update staff profile",
                                    code = null,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }
                    }

                    delete {
                        val userId = call.parameters["userId"]
                        if (userId == null) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(
                                    success = false,
                                    error = "Missing user ID",
                                    code = null,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                            return@delete
                        }

                        val result = adminService.deleteStaffProfileForUser(userId)

                        if (result.isSuccess) {
                            call.respond(
                                HttpStatusCode.OK,
                                MessageResponse(
                                    success = true,
                                    message = "Staff profile deleted successfully",
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.NotFound,
                                ErrorResponse(
                                    success = false,
                                    error = result.exceptionOrNull()?.message ?: "Staff profile not found",
                                    code = null,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
            }

            route("/statistics") {
                get("/students/overall") {
                    val result = adminService.getOverallStudentsCount()

                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, mapOf("totalStudents" to result.getOrNull()!!))
                    } else {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                success = false,
                                error = result.exceptionOrNull()?.message ?: "Failed to get students count",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }

                get("/students/course/{courseId}") {
                    val courseId = call.parameters["courseId"]
                    if (courseId == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                success = false,
                                error = "Missing course ID",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        return@get
                    }

                    val result = adminService.getStudentsCountByCourse(courseId)

                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, mapOf("studentsCount" to result.getOrNull()!!))
                    } else {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                success = false,
                                error = result.exceptionOrNull()?.message ?: "Failed to get course students count",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }

                get("/course/{courseId}") {
                    val courseId = call.parameters["courseId"]
                    if (courseId == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                success = false,
                                error = "Missing course ID",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        return@get
                    }

                    val result = adminService.getCourseStatistics(courseId)

                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                    } else {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                success = false,
                                error = result.exceptionOrNull()?.message ?: "Failed to get course statistics",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }

                get("/overall") {
                    val result = adminService.getOverallStatistics()

                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                    } else {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                success = false,
                                error = result.exceptionOrNull()?.message ?: "Failed to get overall statistics",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        }
    }
}