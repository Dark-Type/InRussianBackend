package com.inRussian.services

import com.inRussian.models.users.StaffProfile
import com.inRussian.models.users.UserProfile
import com.inRussian.models.users.UserRole
import com.inRussian.repositories.StaffProfileRepository
import com.inRussian.repositories.UserProfileRepository
import com.inRussian.repositories.UserRepository
import com.inRussian.requests.users.*
import java.io.File
import java.util.Collections
import java.util.UUID
import kotlin.collections.set

class ProfileService(
    private val userProfileRepository: UserProfileRepository,
    private val staffProfileRepository: StaffProfileRepository,
    private val userRepository: UserRepository,
) {

    suspend fun createUserProfile(userId: String, request: CreateUserProfileRequest): Result<UserProfile> {
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

    suspend fun createStaffProfile(userId: String, request: CreateStaffProfileRequest): Result<StaffProfile> {
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

        return Result.success(staffProfileRepository.update(updatedProfile))
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

    suspend fun addUserLanguageSkill(userId: String, request: UserLanguageSkillRequest): Result<Boolean> {
        val success = userProfileRepository.addSkill(userId, request)
        return if (success) Result.success(true)
        else Result.failure(Exception("Не удалось добавить языковой навык"))
    }

}