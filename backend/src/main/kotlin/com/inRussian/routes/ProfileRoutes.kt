package com.inRussian.routes

import com.inRussian.config.getUserId
import com.inRussian.config.getUserRole
import com.inRussian.requests.admin.UpdateUserRequest
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
import org.slf4j.LoggerFactory
import kotlin.text.get


private val logger = LoggerFactory.getLogger("ProfileRoutes")

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
                    val userId = principal.getUserId()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    val userRole = principal.getUserRole()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    val targetUserId = call.request.queryParameters["targetUserId"]
                    val request = call.receive<CreateUserProfileRequest>()

                    val result = profileService.createUserProfile(userId, userRole, request, targetUserId)

                    if (result.isSuccess) {
                        call.respond(
                            HttpStatusCode.Created,
                            UserProfileResponse(
                                profile = result.getOrNull()!!,
                                message = "User profile created successfully"
                            )
                        )
                    } else {
                        val statusCode = if (result.exceptionOrNull()?.message == "Access denied") {
                            HttpStatusCode.Forbidden
                        } else {
                            HttpStatusCode.BadRequest
                        }
                        call.respond(
                            statusCode,
                            ErrorResponse(error = result.exceptionOrNull()?.message ?: "Failed to create profile")
                        )
                    }
                }
                get("/enriched") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.getUserId()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    val userRole = principal.getUserRole()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    val targetUserId = call.request.queryParameters["targetUserId"]

                    val result = profileService.getUserEnrichedProfile(userId, userRole, targetUserId)

                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                    } else {
                        val statusCode = if (result.exceptionOrNull()?.message == "Access denied") {
                            HttpStatusCode.Forbidden
                        } else {
                            HttpStatusCode.NotFound
                        }
                        call.respond(
                            statusCode, ErrorResponse(error = result.exceptionOrNull()?.message ?: "Profile not found")
                        )
                    }
                }

                get {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.getUserId()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    val userRole = principal.getUserRole()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid token")

                    val result = profileService.getUserProfile(userId, userRole)

                    if (result.isSuccess) {
                        call.respond(
                            HttpStatusCode.OK,
                            UserProfileResponse(
                                profile = result.getOrNull()!!,
                                message = "User profile retrieved successfully"
                            )
                        )
                    } else {
                        val statusCode = if (result.exceptionOrNull()?.message == "Access denied") {
                            HttpStatusCode.Forbidden
                        } else {
                            HttpStatusCode.NotFound
                        }
                        call.respond(
                            statusCode,
                            ErrorResponse(error = result.exceptionOrNull()?.message ?: "Profile not found")
                        )
                    }
                }

                put {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.getUserId()
                        ?: return@put call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    val userRole = principal.getUserRole()
                        ?: return@put call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    val request = call.receive<UpdateUserProfileRequest>()

                    val result = profileService.updateUserProfile(userId, userRole, request)

                    if (result.isSuccess) {
                        call.respond(
                            HttpStatusCode.OK,
                            UserProfileResponse(
                                profile = result.getOrNull()!!,
                                message = "Profile updated successfully"
                            )
                        )
                    } else {
                        val statusCode = if (result.exceptionOrNull()?.message == "Access denied") {
                            HttpStatusCode.Forbidden
                        } else {
                            HttpStatusCode.BadRequest
                        }
                        call.respond(
                            statusCode,
                            ErrorResponse(error = result.exceptionOrNull()?.message ?: "Failed to update profile")
                        )
                    }
                }
                put("/base") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.getUserId()
                        ?: return@put call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    val userRole = principal.getUserRole()
                        ?: return@put call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    val targetUserId = call.request.queryParameters["targetUserId"]
                    val request = call.receive<UpdateUserRequest>()

                    val result = profileService.updateUserBase(userId, userRole, request, targetUserId)

                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, mapOf("success" to true))
                    } else {
                        val statusCode = if (result.exceptionOrNull()?.message == "Access denied") {
                            HttpStatusCode.Forbidden
                        } else {
                            HttpStatusCode.BadRequest
                        }
                        call.respond(
                            statusCode,
                            ErrorResponse(error = result.exceptionOrNull()?.message ?: "Failed to update user base")
                        )
                    }
                }

                get("/{id}") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val currentUserId = principal.getUserId()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    val currentUserRole = principal.getUserRole()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    val targetUserId = call.parameters["id"]

                    if (targetUserId == null) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse(error = "Missing user ID"))
                        return@get
                    }

                    val result = profileService.getUserProfile(currentUserId, currentUserRole, targetUserId)

                    if (result.isSuccess) {
                        call.respond(
                            HttpStatusCode.OK,
                            UserProfileResponse(
                                profile = result.getOrNull()!!,
                                message = "User profile retrieved successfully"
                            )
                        )
                    } else {
                        val statusCode = if (result.exceptionOrNull()?.message == "Access denied") {
                            HttpStatusCode.Forbidden
                        } else {
                            HttpStatusCode.NotFound
                        }
                        call.respond(
                            statusCode,
                            ErrorResponse(error = result.exceptionOrNull()?.message ?: "Profile not found")
                        )
                    }
                }

                put("/{id}") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val currentUserId = principal.getUserId()
                        ?: return@put call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    val currentUserRole = principal.getUserRole()
                        ?: return@put call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    val targetUserId = call.parameters["id"]

                    if (targetUserId == null) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse(error = "Missing user ID"))
                        return@put
                    }

                    val request = call.receive<UpdateUserProfileRequest>()
                    val result = profileService.updateUserProfile(currentUserId, currentUserRole, request, targetUserId)

                    if (result.isSuccess) {
                        call.respond(
                            HttpStatusCode.OK,
                            UserProfileResponse(
                                profile = result.getOrNull()!!,
                                message = "Profile updated successfully"
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse(error = result.exceptionOrNull()?.message ?: "Access denied")
                        )
                    }
                }

                route("/language-skills") {
                    post {
                        val principal = call.principal<JWTPrincipal>()!!
                        val userId = principal.getUserId()
                            ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                        val userRole = principal.getUserRole()
                            ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                        val targetUserId = call.request.queryParameters["targetUserId"]
                        val request = call.receive<UserLanguageSkillRequest>()

                        val result = profileService.addUserLanguageSkill(userId, userRole, request, targetUserId)

                        if (result.isSuccess) {
                            call.respond(HttpStatusCode.Created, mapOf("success" to true))
                        } else {
                            val statusCode = if (result.exceptionOrNull()?.message == "Access denied") {
                                HttpStatusCode.Forbidden
                            } else {
                                HttpStatusCode.BadRequest
                            }
                            call.respond(
                                statusCode,
                                ErrorResponse(error = result.exceptionOrNull()?.message ?: "Ошибка")
                            )
                        }
                    }

                    get {
                        val principal = call.principal<JWTPrincipal>()!!
                        val userId = principal.getUserId()
                            ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                        val userRole = principal.getUserRole()
                            ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                        val targetUserId = call.request.queryParameters["targetUserId"]

                        val result = profileService.getUserLanguageSkills(userId, userRole, targetUserId)

                        if (result.isSuccess) {
                            call.respond(HttpStatusCode.OK, mapOf("skills" to result.getOrNull()))
                        } else {
                            val statusCode = if (result.exceptionOrNull()?.message == "Access denied") {
                                HttpStatusCode.Forbidden
                            } else {
                                HttpStatusCode.BadRequest
                            }
                            call.respond(
                                statusCode,
                                ErrorResponse(error = result.exceptionOrNull()?.message ?: "Ошибка")
                            )
                        }
                    }

                    put("/{skillId}") {
                        val principal = call.principal<JWTPrincipal>()!!
                        val userId = principal.getUserId()
                            ?: return@put call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                        val userRole = principal.getUserRole()
                            ?: return@put call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                        val skillId = call.parameters["skillId"]
                        val targetUserId = call.request.queryParameters["targetUserId"]

                        if (skillId == null) {
                            call.respond(HttpStatusCode.BadRequest, ErrorResponse(error = "Missing skill ID"))
                            return@put
                        }

                        val request = call.receive<UserLanguageSkillRequest>()
                        val result =
                            profileService.updateUserLanguageSkill(userId, userRole, skillId, request, targetUserId)

                        if (result.isSuccess) {
                            call.respond(HttpStatusCode.OK, mapOf("skill" to result.getOrNull()))
                        } else {
                            val statusCode = if (result.exceptionOrNull()?.message == "Access denied") {
                                HttpStatusCode.Forbidden
                            } else {
                                HttpStatusCode.BadRequest
                            }
                            call.respond(
                                statusCode,
                                ErrorResponse(error = result.exceptionOrNull()?.message ?: "Ошибка")
                            )
                        }
                    }

                    delete("/{skillId}") {
                        val principal = call.principal<JWTPrincipal>()!!
                        val userId = principal.getUserId()
                            ?: return@delete call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                        val userRole = principal.getUserRole()
                            ?: return@delete call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                        val skillId = call.parameters["skillId"]
                        val targetUserId = call.request.queryParameters["targetUserId"]

                        if (skillId == null) {
                            call.respond(HttpStatusCode.BadRequest, ErrorResponse(error = "Missing skill ID"))
                            return@delete
                        }

                        val result = profileService.deleteUserLanguageSkill(userId, userRole, skillId, targetUserId)

                        if (result.isSuccess) {
                            call.respond(HttpStatusCode.OK, mapOf("success" to true))
                        } else {
                            val statusCode = if (result.exceptionOrNull()?.message == "Access denied") {
                                HttpStatusCode.Forbidden
                            } else {
                                HttpStatusCode.BadRequest
                            }
                            call.respond(
                                statusCode,
                                ErrorResponse(error = result.exceptionOrNull()?.message ?: "Ошибка")
                            )
                        }
                    }
                }
            }

            route("/staff") {
                post {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.getUserId()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    val userRole = principal.getUserRole()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    val targetUserId = call.request.queryParameters["targetUserId"]
                    val request = call.receive<CreateStaffProfileRequest>()

                    val result = profileService.createStaffProfile(userId, userRole, request, targetUserId)

                    if (result.isSuccess) {
                        call.respond(
                            HttpStatusCode.Created,
                            StaffProfileResponse(
                                profile = result.getOrNull()!!,
                                message = "Staff profile created successfully"
                            )
                        )
                    } else {
                        val statusCode = if (result.exceptionOrNull()?.message == "Access denied") {
                            HttpStatusCode.Forbidden
                        } else {
                            HttpStatusCode.BadRequest
                        }
                        call.respond(
                            statusCode,
                            ErrorResponse(error = result.exceptionOrNull()?.message ?: "Failed to create staff profile")
                        )
                    }
                }

                get {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.getUserId()
                    val userRole = principal.getUserRole()


                    if (userId == null || userRole == null) {
                        return@get call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    }

                    val result = profileService.getStaffProfile(userId, userRole)

                    if (result.isSuccess) {
                        call.respond(
                            HttpStatusCode.OK,
                            StaffProfileResponse(
                                profile = result.getOrNull()!!,
                                message = "Staff profile retrieved successfully"
                            )
                        )
                    } else {
                        val statusCode = if (result.exceptionOrNull()?.message == "Access denied") {
                            HttpStatusCode.Forbidden
                        } else {
                            HttpStatusCode.NotFound
                        }
                        call.respond(
                            statusCode,
                            ErrorResponse(error = result.exceptionOrNull()?.message ?: "Staff profile not found")
                        )
                    }
                }

                put {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.getUserId()
                        ?: return@put call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    val userRole = principal.getUserRole()
                        ?: return@put call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    val request = call.receive<UpdateStaffProfileRequest>()

                    val result = profileService.updateStaffProfile(userId, userRole, request)

                    if (result.isSuccess) {
                        call.respond(
                            HttpStatusCode.OK,
                            StaffProfileResponse(
                                profile = result.getOrNull()!!,
                                message = "Staff profile updated successfully"
                            )
                        )
                    } else {
                        val statusCode = if (result.exceptionOrNull()?.message == "Access denied") {
                            HttpStatusCode.Forbidden
                        } else {
                            HttpStatusCode.BadRequest
                        }
                        call.respond(
                            statusCode,
                            ErrorResponse(error = result.exceptionOrNull()?.message ?: "Failed to update staff profile")
                        )
                    }
                }

                get("/{id}") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val currentUserId = principal.getUserId()
                    val currentUserRole = principal.getUserRole()
                    val targetUserId = call.parameters["id"]


                    if (currentUserId == null || currentUserRole == null) {
                        return@get call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    }

                    if (targetUserId == null) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse(error = "Missing user ID"))
                        return@get
                    }

                    val result = profileService.getStaffProfile(currentUserId, currentUserRole, targetUserId)

                    if (result.isSuccess) {
                        call.respond(
                            HttpStatusCode.OK,
                            StaffProfileResponse(
                                profile = result.getOrNull()!!,
                                message = "Staff profile retrieved successfully"
                            )
                        )
                    } else {
                        val statusCode = if (result.exceptionOrNull()?.message == "Access denied") {
                            HttpStatusCode.Forbidden
                        } else {
                            HttpStatusCode.NotFound
                        }
                        call.respond(
                            statusCode,
                            ErrorResponse(error = result.exceptionOrNull()?.message ?: "Staff profile not found")
                        )
                    }
                }

                put("/{id}") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val currentUserId = principal.getUserId()
                        ?: return@put call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    val currentUserRole = principal.getUserRole()
                        ?: return@put call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    val targetUserId = call.parameters["id"]

                    if (targetUserId == null) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse(error = "Missing user ID"))
                        return@put
                    }

                    val request = call.receive<UpdateStaffProfileRequest>()
                    val result =
                        profileService.updateStaffProfile(currentUserId, currentUserRole, request, targetUserId)

                    if (result.isSuccess) {
                        call.respond(
                            HttpStatusCode.OK,
                            StaffProfileResponse(
                                profile = result.getOrNull()!!,
                                message = "Staff profile updated successfully"
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse(error = result.exceptionOrNull()?.message ?: "Access denied")
                        )
                    }
                }
            }
        }
    }
}