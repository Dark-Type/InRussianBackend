package com.inRussian.routes.v3.profile

import com.inRussian.config.getUserId
import com.inRussian.config.getUserRole
import com.inRussian.requests.admin.UpdateUserRequest
import com.inRussian.requests.users.CreateUserProfileRequest
import com.inRussian.requests.users.UpdateUserProfileRequest
import com.inRussian.requests.users.UserLanguageSkillRequest
import com.inRussian.responses.auth.UserProfileResponse
import com.inRussian.responses.common.ErrorResponse
import com.inRussian.services.v3.ProfileService
import io.ktor.http.*
import io.ktor.resources.Resource
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.resources.*
import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable
import java.util.UUID

fun Route.profileRoutes(profileService: ProfileService) {
    authenticate("auth-jwt") {

        // GET /profiles/avatar/{userId}
        get<ProfilesResource.Avatar> { res ->
            val userId = res.userId
            if (!userId.isUuid()) {
                return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse(error = "invalid_user_id"))
            }
            val avatarId = profileService.getAvatarId(userId)
                ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse(error = "user_not_found_or_avatar_not_set")
                )
            call.respond(mapOf("avatarId" to avatarId))
        }

        // POST /profiles/user[?targetUserId=...]
        post<ProfilesResource.User> { res ->
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.getUserId()!!
            val userRole = principal.getUserRole()!!

            val targetUserId = res.targetUserId
            if (targetUserId != null && !targetUserId.isUuid()) {
                return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse(error = "invalid_targetUserId"))
            }

            val req = runCatching { call.receive<CreateUserProfileRequest>() }.getOrElse {
                return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse(error = "invalid_body"))
            }

            val result = profileService.createUserProfile(userId, userRole, req, targetUserId)
            if (result.isSuccess) {
                call.respond(
                    HttpStatusCode.Created,
                    UserProfileResponse(profile = result.getOrNull()!!, message = "User profile created successfully")
                )
            } else {
                val status = when (result.exceptionOrNull()?.message) {
                    "Access denied" -> HttpStatusCode.Forbidden
                    else -> HttpStatusCode.BadRequest
                }
                call.respond(
                    status,
                    ErrorResponse(error = result.exceptionOrNull()?.message ?: "failed_to_create_profile")
                )
            }
        }

        // GET /profiles/user/enriched[?targetUserId=...]
        get<ProfilesResource.UserEnriched> { res ->
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.getUserId()!!
            val userRole = principal.getUserRole()!!

            val targetUserId = res.targetUserId
            if (targetUserId != null && !targetUserId.isUuid()) {
                return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse(error = "invalid_targetUserId"))
            }

            val result = profileService.getUserEnrichedProfile(userId, userRole, targetUserId)
            if (result.isSuccess) {
                call.respond(HttpStatusCode.OK, result.getOrNull()!!)
            } else {
                val status = when (result.exceptionOrNull()?.message) {
                    "Access denied" -> HttpStatusCode.Forbidden
                    "User profile not found" -> HttpStatusCode.NotFound
                    else -> HttpStatusCode.NotFound
                }
                call.respond(status, ErrorResponse(error = result.exceptionOrNull()?.message ?: "profile_not_found"))
            }
        }

        // GET /profiles/user (self)
        get<ProfilesResource.UserSelf> {
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.getUserId()!!
            val userRole = principal.getUserRole()!!

            val result = profileService.getUserProfile(userId, userRole)
            if (result.isSuccess) {
                call.respond(
                    HttpStatusCode.OK,
                    UserProfileResponse(profile = result.getOrNull()!!, message = "User profile retrieved successfully")
                )
            } else {
                val status = when (result.exceptionOrNull()?.message) {
                    "Access denied" -> HttpStatusCode.Forbidden
                    else -> HttpStatusCode.NotFound
                }
                call.respond(status, ErrorResponse(error = result.exceptionOrNull()?.message ?: "profile_not_found"))
            }
        }

        // PUT /profiles/user (self)
        put<ProfilesResource.UserSelf> {
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.getUserId()!!
            val userRole = principal.getUserRole()!!

            val req = runCatching { call.receive<UpdateUserProfileRequest>() }.getOrElse {
                return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse(error = "invalid_body"))
            }

            val result = profileService.updateUserProfile(userId, userRole, req)
            if (result.isSuccess) {
                call.respond(
                    HttpStatusCode.OK,
                    UserProfileResponse(profile = result.getOrNull()!!, message = "Profile updated successfully")
                )
            } else {
                val status = when (result.exceptionOrNull()?.message) {
                    "Access denied" -> HttpStatusCode.Forbidden
                    else -> HttpStatusCode.BadRequest
                }
                call.respond(
                    status,
                    ErrorResponse(error = result.exceptionOrNull()?.message ?: "failed_to_update_profile")
                )
            }
        }

        // PUT /profiles/user/base[?targetUserId=...]
        put<ProfilesResource.UserBase> { res ->
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.getUserId()!!
            val userRole = principal.getUserRole()!!

            val targetUserId = res.targetUserId
            if (targetUserId != null && !targetUserId.isUuid()) {
                return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse(error = "invalid_targetUserId"))
            }

            val req = runCatching { call.receive<UpdateUserRequest>() }.getOrElse {
                return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse(error = "invalid_body"))
            }

            val result = profileService.updateUserBase(userId, userRole, req, targetUserId)
            if (result.isSuccess) {
                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } else {
                val status = when (result.exceptionOrNull()?.message) {
                    "Access denied" -> HttpStatusCode.Forbidden
                    else -> HttpStatusCode.BadRequest
                }
                call.respond(
                    status,
                    ErrorResponse(error = result.exceptionOrNull()?.message ?: "failed_to_update_user")
                )
            }
        }

        // GET /profiles/user/{id}
        get<ProfilesResource.UserById> { res ->
            val principal = call.principal<JWTPrincipal>()!!
            val currentUserId = principal.getUserId()!!
            val currentUserRole = principal.getUserRole()!!
            val targetUserId = res.id

            if (!targetUserId.isUuid()) {
                return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse(error = "invalid_user_id"))
            }

            val result = profileService.getUserProfile(currentUserId, currentUserRole, targetUserId)
            if (result.isSuccess) {
                call.respond(
                    HttpStatusCode.OK,
                    UserProfileResponse(profile = result.getOrNull()!!, message = "User profile retrieved successfully")
                )
            } else {
                val status = when (result.exceptionOrNull()?.message) {
                    "Access denied" -> HttpStatusCode.Forbidden
                    else -> HttpStatusCode.NotFound
                }
                call.respond(status, ErrorResponse(error = result.exceptionOrNull()?.message ?: "profile_not_found"))
            }
        }

        // PUT /profiles/user/{id}
        put<ProfilesResource.UserById> { res ->
            val principal = call.principal<JWTPrincipal>()!!
            val currentUserId = principal.getUserId()!!
            val currentUserRole = principal.getUserRole()!!
            val targetUserId = res.id

            if (!targetUserId.isUuid()) {
                return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse(error = "invalid_user_id"))
            }

            val req = runCatching { call.receive<UpdateUserProfileRequest>() }.getOrElse {
                return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse(error = "invalid_body"))
            }

            val result = profileService.updateUserProfile(currentUserId, currentUserRole, req, targetUserId)
            if (result.isSuccess) {
                call.respond(
                    HttpStatusCode.OK,
                    UserProfileResponse(profile = result.getOrNull()!!, message = "Profile updated successfully")
                )
            } else {
                val status = when (result.exceptionOrNull()?.message) {
                    "Access denied" -> HttpStatusCode.Forbidden
                    else -> HttpStatusCode.BadRequest
                }
                call.respond(
                    status,
                    ErrorResponse(error = result.exceptionOrNull()?.message ?: "failed_to_update_profile")
                )
            }
        }

        // LANGUAGE SKILLS ------------------------------------------------------

        // POST /profiles/user/language-skills[?targetUserId=...]
        post<ProfilesResource.LanguageSkills> { res ->
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.getUserId()!!
            val userRole = principal.getUserRole()!!

            val targetUserId = res.targetUserId
            if (targetUserId != null && !targetUserId.isUuid()) {
                return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse(error = "invalid_targetUserId"))
            }

            val req = runCatching { call.receive<UserLanguageSkillRequest>() }.getOrElse {
                return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse(error = "invalid_body"))
            }

            val result = profileService.addOrUpdateUserLanguageSkill(userId, userRole, req, targetUserId)
            if (result.isSuccess) {
                call.respond(HttpStatusCode.Created, mapOf("skill" to result.getOrNull()))
            } else {
                val status = when (result.exceptionOrNull()?.message) {
                    "Access denied" -> HttpStatusCode.Forbidden
                    else -> HttpStatusCode.BadRequest
                }
                call.respond(status, ErrorResponse(error = result.exceptionOrNull()?.message ?: "failed_to_save_skill"))
            }
        }

        // GET /profiles/user/language-skills[?targetUserId=...]
        get<ProfilesResource.LanguageSkills> { res ->
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.getUserId()!!
            val userRole = principal.getUserRole()!!

            val targetUserId = res.targetUserId
            if (targetUserId != null && !targetUserId.isUuid()) {
                return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse(error = "invalid_targetUserId"))
            }

            val result = profileService.getUserLanguageSkills(userId, userRole, targetUserId)
            if (result.isSuccess) {
                call.respond(HttpStatusCode.OK, mapOf("skills" to result.getOrNull()))
            } else {
                val status = when (result.exceptionOrNull()?.message) {
                    "Access denied" -> HttpStatusCode.Forbidden
                    else -> HttpStatusCode.BadRequest
                }
                call.respond(status, ErrorResponse(error = result.exceptionOrNull()?.message ?: "failed_to_get_skills"))
            }
        }

        // DELETE /profiles/user/language-skills/{language}[?targetUserId=...]
        delete<ProfilesResource.LanguageSkillByLang> { res ->
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.getUserId()!!
            val userRole = principal.getUserRole()!!

            val targetUserId = res.targetUserId
            if (targetUserId != null && !targetUserId.isUuid()) {
                return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse(error = "invalid_targetUserId"))
            }

            val language = res.language
            if (language.isBlank()) {
                return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse(error = "language_required"))
            }

            val result = profileService.deleteUserLanguageSkill(userId, userRole, language, targetUserId)
            if (result.isSuccess) {
                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } else {
                val status = when (result.exceptionOrNull()?.message) {
                    "Access denied" -> HttpStatusCode.Forbidden
                    else -> HttpStatusCode.BadRequest
                }
                call.respond(
                    status,
                    ErrorResponse(error = result.exceptionOrNull()?.message ?: "failed_to_delete_skill")
                )
            }
        }
    }
}

// -------- Resources --------

@Serializable
@Resource("/profiles")
class ProfilesResource {
    @Serializable
    @Resource("avatar/{userId}")
    data class Avatar(val parent: ProfilesResource = ProfilesResource(), val userId: String)

    @Serializable
    @Resource("user")
    data class User(val parent: ProfilesResource = ProfilesResource(), val targetUserId: String? = null)

    @Serializable
    @Resource("user/enriched")
    data class UserEnriched(val parent: ProfilesResource = ProfilesResource(), val targetUserId: String? = null)

    @Serializable
    @Resource("user/base")
    data class UserBase(val parent: ProfilesResource = ProfilesResource(), val targetUserId: String? = null)

    @Serializable
    @Resource("user/{id}")
    data class UserById(val parent: ProfilesResource = ProfilesResource(), val id: String)

    @Serializable
    @Resource("user/language-skills")
    data class LanguageSkills(val parent: ProfilesResource = ProfilesResource(), val targetUserId: String? = null)

    @Serializable
    @Resource("user/language-skills/{language}")
    data class LanguageSkillByLang(
        val parent: ProfilesResource = ProfilesResource(),
        val language: String,
        val targetUserId: String? = null
    )

    @Serializable
    @Resource("user/self")
    class UserSelf(val parent: ProfilesResource = ProfilesResource())
}

// -------- Helpers --------

private fun String.isUuid(): Boolean = runCatching { UUID.fromString(this) }.isSuccess