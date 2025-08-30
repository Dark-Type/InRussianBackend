package com.inRussian.repositories

import com.inRussian.models.users.User
import com.inRussian.models.users.UserRole
import com.inRussian.models.users.UserProfile
import com.inRussian.models.users.StaffProfile
import com.inRussian.requests.admin.UpdateUserRequest
import com.inRussian.requests.users.CreateStaffProfileRequest
import com.inRussian.requests.users.CreateUserProfileRequest
import com.inRussian.requests.users.UpdateStaffProfileRequest
import com.inRussian.requests.users.UpdateUserProfileRequest
import com.inRussian.tables.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

interface AdminRepository {
    suspend fun getAllUsers(
        page: Int,
        size: Int,
        role: UserRole?,
        createdFrom: LocalDate?,
        createdTo: LocalDate?,
        sortBy: String,
        sortOrder: String
    ): List<User>

    suspend fun getUsersCount(role: UserRole?, createdFrom: LocalDate?, createdTo: LocalDate?): Long
    suspend fun updateUser(userId: String, request: UpdateUserRequest): User?
    suspend fun getStudentsCountByCourse(courseId: String): Long
    suspend fun getOverallStudentsCount(): Long
    suspend fun getCourseAverageTime(courseId: String): Long?
    suspend fun getCourseAverageProgress(courseId: String): Double?
    suspend fun getOverallAverageTime(): Long?
    suspend fun getOverallAverageProgress(): Double?

    suspend fun createUserProfile(userId: String, request: CreateUserProfileRequest): UserProfile?
    suspend fun getUserProfile(userId: String): UserProfile?
    suspend fun updateUserProfile(userId: String, request: UpdateUserProfileRequest): UserProfile?
    suspend fun deleteUserProfile(userId: String): Boolean
    suspend fun createStaffProfile(userId: String, request: CreateStaffProfileRequest): StaffProfile?
    suspend fun getStaffProfile(userId: String): StaffProfile?
    suspend fun updateStaffProfile(userId: String, request: UpdateStaffProfileRequest): StaffProfile?
    suspend fun deleteStaffProfile(userId: String): Boolean
    suspend fun getStudentsByCourseId(courseId: String): List<User>
    suspend fun getAllStudentsWithProfiles(page: Int, size: Int): List<Pair<User, UserProfile?>>
}

class ExposedAdminRepository(
    private val userRepository: UserRepository,
    private val userProfileRepository: UserProfileRepository,
    private val staffProfileRepository: StaffProfileRepository
) : AdminRepository {

    private fun ResultRow.toUser() = User(
        id = this[Users.id].toString(),
        email = this[Users.email],
        passwordHash = this[Users.passwordHash],
        phone = this[Users.phone],
        role = this[Users.role],
        systemLanguage = this[Users.systemLanguage],
        avatarId = this[Users.avatarId],
        status = this[Users.status],
        lastActivityAt = this[Users.lastActivityAt]?.toString(),
        createdAt = this[Users.createdAt].toString(),
        updatedAt = this[Users.updatedAt].toString()
    )

    override suspend fun getAllUsers(
        page: Int,
        size: Int,
        role: UserRole?,
        createdFrom: LocalDate?,
        createdTo: LocalDate?,
        sortBy: String,
        sortOrder: String
    ): List<User> = transaction {
        val query = Users.selectAll()

        if (role != null) {
            query.andWhere { Users.role eq role }
        }

        if (createdFrom != null) {
            query.andWhere { Users.createdAt.date() greaterEq createdFrom }
        }

        if (createdTo != null) {
            query.andWhere { Users.createdAt.date() lessEq createdTo }
        }

        val orderBy = when (sortBy) {
            "email" -> Users.email
            "role" -> Users.role
            "status" -> Users.status
            "createdAt" -> Users.createdAt
            "lastActivityAt" -> Users.lastActivityAt
            else -> Users.createdAt
        }

        if (sortOrder.lowercase() == "desc") {
            query.orderBy(orderBy, SortOrder.DESC)
        } else {
            query.orderBy(orderBy, SortOrder.ASC)
        }

        query.limit(size).offset(start = (page - 1) * size.toLong())
            .map { it.toUser() }
    }

    override suspend fun getUsersCount(role: UserRole?, createdFrom: LocalDate?, createdTo: LocalDate?): Long =
        transaction {
            val query = Users.selectAll()

            if (role != null) {
                query.andWhere { Users.role eq role }
            }

            if (createdFrom != null) {
                query.andWhere { Users.createdAt.date() greaterEq createdFrom }
            }

            if (createdTo != null) {
                query.andWhere { Users.createdAt.date() lessEq createdTo }
            }

            query.count()
        }

    override suspend fun updateUser(userId: String, request: UpdateUserRequest): User? = transaction {
        val userUuid = UUID.fromString(userId)

        Users.update({ Users.id eq userUuid }) {
            request.phone?.let { phone -> it[Users.phone] = phone }
            request.role?.let { role -> it[Users.role] = role }
            request.systemLanguage?.let { lang -> it[systemLanguage] = lang }
            request.avatarId?.let { avatar -> it[avatarId] = avatar }
            it[status] = request.status
            it[updatedAt] = CurrentTimestamp
        }

        Users.selectAll().where { Users.id eq userUuid }
            .singleOrNull()?.toUser()
    }

    override suspend fun getStudentsCountByCourse(courseId: String): Long = transaction {
        UserCourseEnrollments.selectAll()
            .where { UserCourseEnrollments.courseId eq UUID.fromString(courseId) }
            .count()
    }

    override suspend fun getOverallStudentsCount(): Long = transaction {
        Users.selectAll()
            .where { Users.role eq UserRole.STUDENT }
            .count()
    }

    override suspend fun getCourseAverageTime(courseId: String): Long? = transaction {
        UserCourseStatistics.selectAll()
            .where { UserCourseStatistics.courseId eq UUID.fromString(courseId) }
            .map { it[UserCourseStatistics.timeSpentSeconds] }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toLong()
    }

    override suspend fun getCourseAverageProgress(courseId: String): Double? = transaction {
        val progressList = UserCourseStatistics.selectAll()
            .where { UserCourseStatistics.courseId eq UUID.fromString(courseId) }
            .map { it[UserCourseStatistics.progressPercentage].toDouble() }

        if (progressList.isEmpty()) {
            null
        } else {
            progressList.average()
        }
    }

    override suspend fun getOverallAverageTime(): Long? = transaction {
        UserStatistics.selectAll()
            .map { it[UserStatistics.totalTimeSpentSeconds] }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toLong()
    }

    override suspend fun getOverallAverageProgress(): Double? = transaction {
        val userStats = UserStatistics.selectAll().map { row ->
            val totalTasks = row[UserStatistics.totalTasksAttempted]
            val completedTasks = row[UserStatistics.totalTasksCompleted]
            if (totalTasks > 0) {
                (completedTasks.toDouble() / totalTasks.toDouble()) * 100.0
            } else {
                0.0
            }
        }

        if (userStats.isEmpty()) {
            null
        } else {
            userStats.average()
        }
    }

    override suspend fun createUserProfile(userId: String, request: CreateUserProfileRequest): UserProfile? {
        val user = userRepository.findById(userId) ?: return null

        if (user.role != UserRole.STUDENT) return null
        if (userProfileRepository.findByUserId(userId) != null) return null

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

        return userProfileRepository.create(profile)
    }

    override suspend fun getUserProfile(userId: String): UserProfile? {
        return userProfileRepository.findByUserId(userId)
    }

    override suspend fun updateUserProfile(userId: String, request: UpdateUserProfileRequest): UserProfile? {
        val existingProfile = userProfileRepository.findByUserId(userId) ?: return null

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

        return userProfileRepository.update(updatedProfile)
    }

    override suspend fun deleteUserProfile(userId: String): Boolean {
        return userProfileRepository.deleteByUserId(userId)
    }

    override suspend fun createStaffProfile(userId: String, request: CreateStaffProfileRequest): StaffProfile? {
        val user = userRepository.findById(userId) ?: return null

        if (user.role !in listOf(UserRole.EXPERT, UserRole.CONTENT_MODERATOR, UserRole.ADMIN)) {
            return null
        }
        if (staffProfileRepository.findByUserId(userId) != null) return null

        val profile = StaffProfile(
            userId = userId,
            name = request.name,
            surname = request.surname,
            patronymic = request.patronymic
        )

        return staffProfileRepository.create(profile)
    }

    override suspend fun getStaffProfile(userId: String): StaffProfile? {
        return staffProfileRepository.findByUserId(userId)
    }

    override suspend fun updateStaffProfile(userId: String, request: UpdateStaffProfileRequest): StaffProfile? {
        val existingProfile = staffProfileRepository.findByUserId(userId) ?: return null

        val updatedProfile = existingProfile.copy(
            name = request.name ?: existingProfile.name,
            surname = request.surname ?: existingProfile.surname,
            patronymic = request.patronymic ?: existingProfile.patronymic
        )

        return staffProfileRepository.update(updatedProfile)
    }

    override suspend fun deleteStaffProfile(userId: String): Boolean {
        return staffProfileRepository.deleteByUserId(userId)
    }

    override suspend fun getStudentsByCourseId(courseId: String): List<User> = transaction {
        val enrolledUserIds = UserCourseEnrollments
            .selectAll()
            .where { UserCourseEnrollments.courseId eq UUID.fromString(courseId) }
            .map { it[UserCourseEnrollments.userId] }

        if (enrolledUserIds.isEmpty()) {
            emptyList()
        } else {
            Users.selectAll()
                .where { Users.id inList enrolledUserIds }
                .map { it.toUser() }
        }
    }

    override suspend fun getAllStudentsWithProfiles(page: Int, size: Int): List<Pair<User, UserProfile?>> =
        transaction {
            val students = Users.selectAll()
                .where { Users.role eq UserRole.STUDENT }
                .limit(size)
                .offset(start = (page - 1) * size.toLong())
                .map { it.toUser() }

            students.map { student ->
                val profile = UserProfiles.selectAll()
                    .where { UserProfiles.userId eq UUID.fromString(student.id) }
                    .singleOrNull()
                    ?.let { row ->
                        UserProfile(
                            userId = row[UserProfiles.userId].toString(),
                            surname = row[UserProfiles.surname],
                            name = row[UserProfiles.name],
                            patronymic = row[UserProfiles.patronymic],
                            gender = row[UserProfiles.gender],
                            dob = row[UserProfiles.dob].toString(),
                            dor = row[UserProfiles.dor].toString(),
                            citizenship = row[UserProfiles.citizenship],
                            nationality = row[UserProfiles.nationality],
                            countryOfResidence = row[UserProfiles.countryOfResidence],
                            cityOfResidence = row[UserProfiles.cityOfResidence],
                            countryDuringEducation = row[UserProfiles.countryDuringEducation],
                            periodSpent = row[UserProfiles.periodSpent],
                            kindOfActivity = row[UserProfiles.kindOfActivity],
                            education = row[UserProfiles.education],
                            purposeOfRegister = row[UserProfiles.purposeOfRegister]
                        )
                    }
                student to profile
            }
        }
}