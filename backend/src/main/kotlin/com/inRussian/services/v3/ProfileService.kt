package com.inRussian.services.v3

import com.inRussian.models.users.User
import com.inRussian.models.users.UserLanguageSkill
import com.inRussian.models.users.UserProfile
import com.inRussian.models.users.UserRole
import com.inRussian.models.v2.UserEnrichedProfile
import com.inRussian.repositories.UserLanguageSkillsRepository
import com.inRussian.repositories.UserProfilesRepository
import com.inRussian.repositories.UsersRepository
import com.inRussian.requests.admin.UpdateUserRequest
import com.inRussian.requests.users.CreateUserProfileRequest
import com.inRussian.requests.users.UpdateUserProfileRequest
import com.inRussian.requests.users.UserLanguageSkillRequest
import java.util.UUID

class ProfileService(
    private val users: UsersRepository,
    private val profiles: UserProfilesRepository,
    private val languageSkills: UserLanguageSkillsRepository,
) {

    private fun canWrite(currentRole: UserRole, currentUserId: String, targetUserId: String): Boolean =
        currentRole == UserRole.ADMIN || currentUserId == targetUserId

    private fun canRead(currentRole: UserRole, currentUserId: String, targetUserId: String): Boolean =
        currentRole == UserRole.ADMIN ||
                currentRole == UserRole.EXPERT ||
                currentUserId == targetUserId

    private fun String.isUuid(): Boolean = runCatching { UUID.fromString(this) }.isSuccess

    // --- User base updates ----------------------------------------------------

    suspend fun updateUserBase(
        currentUserId: String,
        currentUserRole: UserRole,
        request: UpdateUserRequest,
        targetUserId: String? = null
    ): Result<User> {
        val userId = targetUserId ?: currentUserId
        if (!userId.isUuid()) return Result.failure(IllegalArgumentException("Invalid userId"))
        if (!canWrite(currentUserRole, currentUserId, userId)) {
            return Result.failure(IllegalAccessException("Access denied"))
        }

        val existing = users.findById(userId) ?: return Result.failure(NoSuchElementException("User not found"))

        // Basic validation examples
        request.phone?.let {
            if (it.length !in 7..20) return Result.failure(IllegalArgumentException("phone_invalid_length"))
        }
        request.avatarId?.let {
            if (!it.isUuid()) return Result.failure(IllegalArgumentException("avatarId_invalid"))
        }

        val updated = users.update(userId, request)
            ?: return Result.failure(IllegalStateException("Failed to update user"))

        return Result.success(updated)
    }

    // --- User profile (student) ----------------------------------------------

    suspend fun createUserProfile(
        currentUserId: String,
        currentUserRole: UserRole,
        request: CreateUserProfileRequest,
        targetUserId: String? = null
    ): Result<UserProfile> {
        val userId = targetUserId ?: currentUserId
        if (!userId.isUuid()) return Result.failure(IllegalArgumentException("Invalid userId"))
        if (!canWrite(currentUserRole, currentUserId, userId)) {
            return Result.failure(IllegalAccessException("Access denied"))
        }

        val user = users.findById(userId) ?: return Result.failure(NoSuchElementException("User not found"))
        if (user.role != UserRole.STUDENT) {
            return Result.failure(IllegalStateException("Only students can have user profiles"))
        }

        if (profiles.findByUserId(userId) != null) {
            return Result.failure(IllegalStateException("User profile already exists"))
        }

        // Basic field sanity
        if (request.name.isBlank() || request.surname.isBlank()) {
            return Result.failure(IllegalArgumentException("name_and_surname_required"))
        }

        val profile = UserProfile(
            userId = userId,
            gender = request.gender,
            dob = request.dob,
            dor = request.dor,
            citizenship = request.citizenship,
            nationality = request.nationality,
            countryOfResidence = request.countryOfResidence,
            cityOfResidence = request.cityOfResidence,
            countryDuringEducation = request.countryDuringEducation,
            periodSpent = request.periodSpent,
            kindOfActivity = request.kindOfActivity,
            education = request.education,
            purposeOfRegister = request.purposeOfRegister
        )
        return Result.success(profiles.create(profile))
    }

    suspend fun updateUserProfile(
        currentUserId: String,
        currentUserRole: UserRole,
        request: UpdateUserProfileRequest,
        targetUserId: String? = null
    ): Result<UserProfile> {
        val userId = targetUserId ?: currentUserId
        if (!userId.isUuid()) return Result.failure(IllegalArgumentException("Invalid userId"))
        if (!canWrite(currentUserRole, currentUserId, userId)) {
            return Result.failure(IllegalAccessException("Access denied"))
        }

        val existing = profiles.findByUserId(userId)
            ?: return Result.failure(NoSuchElementException("User profile not found"))

        val updated = existing.copy(
            gender = request.gender ?: existing.gender,
            dob = request.dob ?: existing.dob,
            dor = request.dor ?: existing.dor,
            citizenship = request.citizenship ?: existing.citizenship,
            nationality = request.nationality ?: existing.nationality,
            countryOfResidence = request.countryOfResidence ?: existing.countryOfResidence,
            cityOfResidence = request.cityOfResidence ?: existing.cityOfResidence,
            countryDuringEducation = request.countryDuringEducation ?: existing.countryDuringEducation,
            periodSpent = request.periodSpent ?: existing.periodSpent,
            kindOfActivity = request.kindOfActivity ?: existing.kindOfActivity,
            education = request.education ?: existing.education,
            purposeOfRegister = request.purposeOfRegister ?: existing.purposeOfRegister
        )

        return Result.success(profiles.update(updated))
    }

    // --- Read profiles -------------------------------------------------------

    suspend fun getUserProfile(
        currentUserId: String,
        currentUserRole: UserRole,
        targetUserId: String? = null
    ): Result<UserProfile> {
        val userId = targetUserId ?: currentUserId
        if (!userId.isUuid()) return Result.failure(IllegalArgumentException("Invalid userId"))
        if (!canRead(currentUserRole, currentUserId, userId)) {
            return Result.failure(IllegalAccessException("Access denied"))
        }
        val profile = profiles.findByUserId(userId)
            ?: return Result.failure(NoSuchElementException("User profile not found"))
        return Result.success(profile)
    }

    suspend fun getUserEnrichedProfile(
        currentUserId: String,
        currentUserRole: UserRole,
        targetUserId: String? = null
    ): Result<UserEnrichedProfile> {
        val userId = targetUserId ?: currentUserId
        if (!userId.isUuid()) return Result.failure(IllegalArgumentException("Invalid userId"))
        if (!canRead(currentUserRole, currentUserId, userId)) {
            return Result.failure(IllegalAccessException("Access denied"))
        }

        val profile = profiles.findByUserId(userId)
            ?: return Result.failure(NoSuchElementException("User profile not found"))
        val user = users.findById(userId)
            ?: return Result.failure(NoSuchElementException("User not found"))
        val skills = languageSkills.findByUser(userId)

        val enriched = UserEnrichedProfile(
            userId = profile.userId,
            gender = profile.gender,
            dob = profile.dob,
            dor = profile.dor,
            citizenship = profile.citizenship,
            nationality = profile.nationality,
            countryOfResidence = profile.countryOfResidence,
            cityOfResidence = profile.cityOfResidence,
            countryDuringEducation = profile.countryDuringEducation,
            periodSpent = profile.periodSpent,
            kindOfActivity = profile.kindOfActivity,
            education = profile.education,
            purposeOfRegister = profile.purposeOfRegister,
            avatarId = user.avatarId,
            email = user.email,
            systemLanguage = user.systemLanguage,
            phone = user.phone,
            languageSkills = skills,
            name = user.name,
            surname = user.surname,
        )
        return Result.success(enriched)
    }

    // --- Language skills -----------------------------------------------------

    suspend fun addOrUpdateUserLanguageSkill(
        currentUserId: String,
        currentUserRole: UserRole,
        request: UserLanguageSkillRequest,
        targetUserId: String? = null
    ): Result<UserLanguageSkill> {
        val userId = targetUserId ?: currentUserId
        if (!userId.isUuid()) return Result.failure(IllegalArgumentException("Invalid userId"))
        if (!canWrite(currentUserRole, currentUserId, userId)) {
            return Result.failure(IllegalAccessException("Access denied"))
        }

        if (request.language.isBlank()) {
            return Result.failure(IllegalArgumentException("language_required"))
        }

        val skill = UserLanguageSkill(
            userId = userId,
            language = request.language.trim(),
            understands = request.understands,
            speaks = request.speaks,
            reads = request.reads,
            writes = request.writes
        )
        return Result.success(languageSkills.upsert(skill))
    }

    suspend fun getUserLanguageSkills(
        currentUserId: String,
        currentUserRole: UserRole,
        targetUserId: String? = null
    ): Result<List<UserLanguageSkill>> {
        val userId = targetUserId ?: currentUserId
        if (!userId.isUuid()) return Result.failure(IllegalArgumentException("Invalid userId"))
        if (!canRead(currentUserRole, currentUserId, userId)) {
            return Result.failure(IllegalAccessException("Access denied"))
        }
        return Result.success(languageSkills.findByUser(userId))
    }

    suspend fun deleteUserLanguageSkill(
        currentUserId: String,
        currentUserRole: UserRole,
        language: String,
        targetUserId: String? = null
    ): Result<Boolean> {
        val userId = targetUserId ?: currentUserId
        if (!userId.isUuid()) return Result.failure(IllegalArgumentException("Invalid userId"))
        if (!canWrite(currentUserRole, currentUserId, userId)) {
            return Result.failure(IllegalAccessException("Access denied"))
        }
        if (language.isBlank()) return Result.failure(IllegalArgumentException("language_required"))

        val deleted = languageSkills.delete(userId, language.trim())
        return if (deleted) Result.success(true) else Result.failure(NoSuchElementException("Language skill not found"))
    }

    // --- Helpers -------------------------------------------------------------

    suspend fun getAvatarId(userId: String): String? =
        if (userId.isUuid()) users.findById(userId)?.avatarId else null
}