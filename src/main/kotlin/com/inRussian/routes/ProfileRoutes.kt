package com.inRussian.routes

import com.inRussian.config.getUserId
import com.inRussian.config.getUserRole
import com.inRussian.requests.users.*
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
            get("/avatar/{userId}") {
                val userId = call.parameters["userId"]
                if (userId == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(error = "Missing user ID"))
                    return@get
                }
                val avatarId = profileService.getAvatarId(userId)
                if (avatarId == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse(error = "User not found or avatar not set"))
                    return@get
                }
                call.respond(mapOf("avatarId" to avatarId))
            }

            route("/user") {
                post {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.getUserId()!!
                    val userRole = principal.getUserRole()!!
                    val targetUserId = call.request.queryParameters["targetUserId"]
                    val request = call.receive<CreateUserProfileRequest>()
                    val result = profileService.createUserProfile(userId, userRole, request, targetUserId)

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
                            ErrorResponse(error = "Missing user ID")
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
                            ErrorResponse(error = error?.message ?: "Profile not found")
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
                            ErrorResponse(error = "Missing user ID")
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
                            ErrorResponse(error = error?.message ?: "Access denied")
                        )
                    }
                }

                route("/language-skills") {
                    post {
                        val principal = call.principal<JWTPrincipal>()!!
                        val userId = principal.getUserId()!!
                        val userRole = principal.getUserRole()!!
                        val targetUserId = call.request.queryParameters["targetUserId"]
                        val request = call.receive<UserLanguageSkillRequest>()
                        val result = profileService.addUserLanguageSkill(userId, userRole, request, targetUserId)

                        if (result.isSuccess) {
                            call.respond(HttpStatusCode.Created, mapOf("success" to true))
                        } else {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(error = result.exceptionOrNull()?.message ?: "Ошибка")
                            )
                        }
                    }

                    get {
                        val principal = call.principal<JWTPrincipal>()!!
                        val userId = principal.getUserId()!!
                        val userRole = principal.getUserRole()!!
                        val targetUserId = call.request.queryParameters["targetUserId"]
                        val result = profileService.getUserLanguageSkills(userId, userRole, targetUserId)

                        if (result.isSuccess) {
                            call.respond(HttpStatusCode.OK, mapOf("skills" to result.getOrNull()))
                        } else {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(error = result.exceptionOrNull()?.message ?: "Ошибка")
                            )
                        }
                    }

                    put("/{skillId}") {
                        val principal = call.principal<JWTPrincipal>()!!
                        val userId = principal.getUserId()!!
                        val userRole = principal.getUserRole()!!
                        val language = call.parameters["language"]
                        val targetUserId = call.request.queryParameters["targetUserId"]

                        if (language == null) {
                            call.respond(HttpStatusCode.BadRequest, ErrorResponse(error = "Missing skill ID"))
                            return@put
                        }

                        val request = call.receive<UserLanguageSkillRequest>()
                        val result = profileService.updateUserLanguageSkill(userId, userRole, language, request, targetUserId)

                        if (result.isSuccess) {
                            call.respond(HttpStatusCode.OK, mapOf("skill" to result.getOrNull()))
                        } else {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(error = result.exceptionOrNull()?.message ?: "Ошибка")
                            )
                        }
                    }

                    delete("/{skillId}") {
                        val principal = call.principal<JWTPrincipal>()!!
                        val userId = principal.getUserId()!!
                        val userRole = principal.getUserRole()!!
                        val language = call.parameters["language"]
                        val targetUserId = call.request.queryParameters["targetUserId"]

                        if (language == null) {
                            call.respond(HttpStatusCode.BadRequest, ErrorResponse(error = "Missing skill ID"))
                            return@delete
                        }

                        val result = profileService.deleteUserLanguageSkill(userId, userRole, language, targetUserId)

                        if (result.isSuccess) {
                            call.respond(HttpStatusCode.OK, mapOf("success" to true))
                        } else {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(error = result.exceptionOrNull()?.message ?: "Ошибка")
                            )
                        }
                    }
                }
            }

            route("/staff") {
                post {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.getUserId()!!
                    val userRole = principal.getUserRole()!!
                    val targetUserId = call.request.queryParameters["targetUserId"]
                    val request = call.receive<CreateStaffProfileRequest>()
                    val result = profileService.createStaffProfile(userId, userRole, request, targetUserId)

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
                            ErrorResponse(error = error?.message ?: "Failed to create staff profile")
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
                            ErrorResponse(error = error?.message ?: "Staff profile not found")
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
                            ErrorResponse(error = error?.message ?: "Failed to update staff profile")
                        )
                    }
                }

                get("/{id}") {
                    val targetUserId = call.parameters["id"]
                    if (targetUserId == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(error = "Missing user ID")
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
                            ErrorResponse(error = error?.message ?: "Staff profile not found")
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
                            ErrorResponse(error = "Missing user ID")
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
                            ErrorResponse(error = error?.message ?: "Access denied")
                        )
                    }
                }
            }
        }
    }
}