package com.inRussian.routes

import com.inRussian.config.getUserId
import com.inRussian.config.getUserRole
import com.inRussian.requests.users.CreateStaffProfileRequest
import com.inRussian.requests.users.CreateUserProfileRequest
import com.inRussian.requests.users.UpdateStaffProfileRequest
import com.inRussian.requests.users.UpdateUserProfileRequest
import com.inRussian.responses.auth.StaffProfileResponse
import com.inRussian.responses.auth.UserProfileResponse
import com.inRussian.responses.common.ErrorResponse
import com.inRussian.services.ProfileService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

fun Route.profileRoutes(profileService: ProfileService) {
    route("/profiles") {
        authenticate("auth-jwt") {
            route("/user") {
                post {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.getUserId()!!
                    val request = call.receive<CreateUserProfileRequest>()
                    val result = profileService.createUserProfile(userId, request)

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
                        val error = result.exceptionOrNull()
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                error = error?.message ?: "Failed to create profile"
                            )
                        )
                    }
                }

                get {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.getUserId()!!
                    val result = profileService.getUserProfile(userId)

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
                        val error = result.exceptionOrNull()
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(
                                error = error?.message ?: "Profile not found"
                            )
                        )
                    }
                }

                put {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.getUserId()!!
                    val userRole = principal.getUserRole()!!
                    val request = call.receive<UpdateUserProfileRequest>()
                    val result = profileService.updateUserProfile(userId, request, userRole, userId)

                    if (result.isSuccess) {
                        val profile = result.getOrNull()
                        call.respond(
                            HttpStatusCode.OK,
                            UserProfileResponse(
                                profile = profile!!,
                                message = "Profile updated successfully"
                            )
                        )
                    } else {
                        val error = result.exceptionOrNull()
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                error = error?.message ?: "Failed to update profile"
                            )
                        )
                    }
                }

                get("/{id}") {
                    val targetUserId = call.parameters["id"]
                    if (targetUserId == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                error = "Missing user ID"
                            )
                        )
                        return@get
                    }
                    val result = profileService.getUserProfile(targetUserId)

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
                        val error = result.exceptionOrNull()
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(
                                error = error?.message ?: "Profile not found"
                            )
                        )
                    }
                }

                put("/{id}") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val currentUserId = principal.getUserId()!!
                    val currentUserRole = principal.getUserRole()!!
                    val targetUserId = call.parameters["id"]
                    if (targetUserId == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                error = "Missing user ID"
                            )
                        )
                        return@put
                    }
                    val request = call.receive<UpdateUserProfileRequest>()
                    val result = profileService.updateUserProfile(targetUserId, request, currentUserRole, currentUserId)

                    if (result.isSuccess) {
                        val profile = result.getOrNull()
                        call.respond(
                            HttpStatusCode.OK,
                            UserProfileResponse(
                                profile = profile!!,
                                message = "Profile updated successfully"
                            )
                        )
                    } else {
                        val error = result.exceptionOrNull()
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse(
                                error = error?.message ?: "Access denied"
                            )
                        )
                    }
                }
            }

            // Staff Profile routes
            route("/staff") {
                post {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.getUserId()!!
                    val request = call.receive<CreateStaffProfileRequest>()
                    val result = profileService.createStaffProfile(userId, request)

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
                        val error = result.exceptionOrNull()
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                error = error?.message ?: "Failed to create staff profile"
                            )
                        )
                    }
                }

                get {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.getUserId()!!
                    val result = profileService.getStaffProfile(userId)

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
                        val error = result.exceptionOrNull()
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(
                                error = error?.message ?: "Staff profile not found"
                            )
                        )
                    }
                }

                put {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.getUserId()!!
                    val userRole = principal.getUserRole()!!
                    val request = call.receive<UpdateStaffProfileRequest>()
                    val result = profileService.updateStaffProfile(userId, request, userRole, userId)

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
                        val error = result.exceptionOrNull()
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                error = error?.message ?: "Failed to update staff profile"
                            )
                        )
                    }
                }

                get("/{id}") {
                    val targetUserId = call.parameters["id"]
                    if (targetUserId == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                error = "Missing user ID"
                            )
                        )
                        return@get
                    }
                    val result = profileService.getStaffProfile(targetUserId)

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
                        val error = result.exceptionOrNull()
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(
                                error = error?.message ?: "Staff profile not found"
                            )
                        )
                    }
                }

                put("/{id}") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val currentUserId = principal.getUserId()!!
                    val currentUserRole = principal.getUserRole()!!
                    val targetUserId = call.parameters["id"]
                    if (targetUserId == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                error = "Missing user ID"
                            )
                        )
                        return@put
                    }
                    val request = call.receive<UpdateStaffProfileRequest>()
                    val result = profileService.updateStaffProfile(targetUserId, request, currentUserRole, currentUserId)

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
                        val error = result.exceptionOrNull()
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse(
                                error = error?.message ?: "Access denied"
                            )
                        )
                    }
                }
            }
        }
    }
}