package com.inRussian.services

import com.inRussian.models.users.StaffProfile
import com.inRussian.models.users.SystemLanguage
import com.inRussian.models.users.UserLanguageSkill
import com.inRussian.models.users.UserProfile
import com.inRussian.models.users.UserRole
import com.inRussian.repositories.StaffProfileRepository
import com.inRussian.repositories.UserProfileRepository
import com.inRussian.repositories.UserRepository
import com.inRussian.requests.users.*


class ProfileService(
    private val userProfileRepository: UserProfileRepository,
    private val staffProfileRepository: StaffProfileRepository,
    private val userRepository: UserRepository,
) {

    suspend fun createUserProfile(
        currentUserId: String,
        currentUserRole: UserRole,
        request: CreateUserProfileRequest,
        targetUserId: String? = null
    ): Result<UserProfile> {
        val userId = if (currentUserRole == UserRole.ADMIN && targetUserId != null) {
            targetUserId
        } else {
            currentUserId
        }

        val user = userRepository.findById(userId)
            ?: return Result.failure(Exception("User not found"))

        if (user.role != UserRole.STUDENT) {
            return Result.failure(Exception("Only students can have user profiles"))
        }

        if (userProfileRepository.findByUserId(userId) != null) {
            return Result.failure(Exception("User profile already exists"))
        }

        val profile = UserProfile(
            userId = userId,
            surname = request.surname,
            name = request.name,
            patronymic = request.patronymic,
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

        return Result.success(userProfileRepository.create(profile))
    }

    suspend fun updateUserProfile(
        userId: String,
        request: UpdateUserProfileRequest,
        currentUserRole: UserRole,
        currentUserId: String
    ): Result<UserProfile> {
        if (currentUserRole != UserRole.ADMIN && currentUserId != userId) {
            return Result.failure(Exception("You can only edit your own profile"))
        }

        val existingProfile = userProfileRepository.findByUserId(userId)
            ?: return Result.failure(Exception("User profile not found"))

        val updatedProfile = existingProfile.copy(
            surname = request.surname ?: existingProfile.surname,
            name = request.name ?: existingProfile.name,
            patronymic = request.patronymic ?: existingProfile.patronymic,
            gender = request.gender ?: existingProfile.gender,
            dob = request.dob ?: existingProfile.dob,
            dor = request.dor ?: existingProfile.dor,
            citizenship = request.citizenship ?: existingProfile.citizenship,
            nationality = request.nationality ?: existingProfile.nationality,
            countryOfResidence = request.countryOfResidence ?: existingProfile.countryOfResidence,
            cityOfResidence = request.cityOfResidence ?: existingProfile.cityOfResidence,
            countryDuringEducation = request.countryDuringEducation ?: existingProfile.countryDuringEducation,
            periodSpent = request.periodSpent ?: existingProfile.periodSpent,
            kindOfActivity = request.kindOfActivity ?: existingProfile.kindOfActivity,
            education = request.education ?: existingProfile.education,
            purposeOfRegister = request.purposeOfRegister ?: existingProfile.purposeOfRegister
        )

        return Result.success(userProfileRepository.update(updatedProfile))
    }

    suspend fun createStaffProfile(
        currentUserId: String,
        currentUserRole: UserRole,
        request: CreateStaffProfileRequest,
        targetUserId: String? = null
    ): Result<StaffProfile> {
        val userId = if (currentUserRole == UserRole.ADMIN && targetUserId != null) {
            targetUserId
        } else {
            currentUserId
        }

        val user = userRepository.findById(userId)
            ?: return Result.failure(Exception("User not found"))

        if (user.role !in listOf(UserRole.EXPERT, UserRole.CONTENT_MODERATOR, UserRole.ADMIN)) {
            return Result.failure(Exception("Only staff members can have staff profiles"))
        }

        if (staffProfileRepository.findByUserId(userId) != null) {
            return Result.failure(Exception("Staff profile already exists"))
        }

        val profile = StaffProfile(
            userId = userId,
            name = request.name,
            surname = request.surname,
            patronymic = request.patronymic
        )

        return Result.success(staffProfileRepository.create(profile))
    }

    suspend fun getAvatarId(userId: String): String? {
        return userRepository.findById(userId)?.avatarId
    }

    suspend fun updateStaffProfile(
        userId: String,
        request: UpdateStaffProfileRequest,
        currentUserRole: UserRole,
        currentUserId: String
    ): Result<StaffProfile> {
        if (currentUserRole != UserRole.ADMIN && currentUserId != userId) {
            return Result.failure(Exception("You can only edit your own profile"))
        }

        val existingProfile = staffProfileRepository.findByUserId(userId)
            ?: return Result.failure(Exception("Staff profile not found"))

        val updatedProfile = existingProfile.copy(
            name = request.name ?: existingProfile.name,
            surname = request.surname ?: existingProfile.surname,
            patronymic = request.patronymic ?: existingProfile.patronymic
        )
        staffProfileRepository.update(updatedProfile)

        val user = userRepository.findById(userId)
            ?: return Result.failure(Exception("User not found"))
        val updatedUser = user.copy(
            passwordHash = request.passwordHash ?: user.passwordHash,
            systemLanguage = (request.systemLanguage ?: user.systemLanguage) as SystemLanguage,
            phone = request.phone ?: user.phone,
            avatarId = request.avatarId ?: user.avatarId
        )
        userRepository.update(updatedUser)

        return Result.success(updatedProfile)
    }

    suspend fun getUserProfile(userId: String): Result<UserProfile> {
        val profile = userProfileRepository.findByUserId(userId)
            ?: return Result.failure(Exception("User profile not found"))
        return Result.success(profile)
    }

    suspend fun getStaffProfile(userId: String): Result<StaffProfile> {
        val profile = staffProfileRepository.findByUserId(userId)
            ?: return Result.failure(Exception("Staff profile not found"))
        return Result.success(profile)
    }

    suspend fun addUserLanguageSkill(
        currentUserId: String,
        currentUserRole: UserRole,
        request: UserLanguageSkillRequest,
        targetUserId: String? = null
    ): Result<Boolean> {
        val userId = if (currentUserRole == UserRole.ADMIN && targetUserId != null) {
            targetUserId
        } else {
            currentUserId
        }

        if (currentUserRole != UserRole.ADMIN && currentUserId != userId) {
            return Result.failure(Exception("You can only manage your own language skills"))
        }

        val success = userProfileRepository.addSkill(userId, request)
        return if (success) Result.success(true)
        else Result.failure(Exception("Не удалось добавить языковой навык"))
    }

    suspend fun getUserLanguageSkills(
        currentUserId: String,
        currentUserRole: UserRole,
        targetUserId: String? = null
    ): Result<List<UserLanguageSkill>> {
        val userId = if (targetUserId != null) targetUserId else currentUserId

        if ((currentUserRole != UserRole.ADMIN) && (currentUserRole != UserRole.EXPERT) && currentUserId != userId) {
            return Result.failure(Exception("Access denied"))
        }

        val skills = userProfileRepository.getSkills(userId)
        return Result.success(skills)
    }

    suspend fun updateUserLanguageSkill(
        currentUserId: String,
        currentUserRole: UserRole,
        skillId: String,
        request: UserLanguageSkillRequest,
        targetUserId: String? = null
    ): Result<UserLanguageSkill> {
        val userId = if (currentUserRole == UserRole.ADMIN && targetUserId != null) {
            targetUserId
        } else {
            currentUserId
        }

        if (currentUserRole != UserRole.ADMIN && currentUserId != userId) {
            return Result.failure(Exception("You can only manage your own language skills"))
        }

        val updatedSkill = userProfileRepository.updateSkill(skillId, userId, request)
            ?: return Result.failure(Exception("Language skill not found"))

        return Result.success(updatedSkill)
    }

    suspend fun deleteUserLanguageSkill(
        currentUserId: String,
        currentUserRole: UserRole,
        skillId: String,
        targetUserId: String? = null
    ): Result<Boolean> {
        val userId = if (currentUserRole == UserRole.ADMIN && targetUserId != null) {
            targetUserId
        } else {
            currentUserId
        }

        if (currentUserRole != UserRole.ADMIN && currentUserId != userId) {
            return Result.failure(Exception("You can only manage your own language skills"))
        }

        val success = userProfileRepository.deleteSkill(skillId, userId)
        return if (success) Result.success(true)
        else Result.failure(Exception("Failed to delete language skill"))
    }
}