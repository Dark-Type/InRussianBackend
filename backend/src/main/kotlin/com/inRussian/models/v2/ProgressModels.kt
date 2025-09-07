package com.inRussian.models.v2


import com.inRussian.models.tasks.TaskBody
import com.inRussian.models.users.Gender
import com.inRussian.models.users.PeriodSpent
import com.inRussian.models.users.SystemLanguage
import com.inRussian.models.users.UserLanguageSkill
import kotlinx.serialization.KSerializer
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder


data class AttemptInsert(
    val attemptId: UUID,
    val userId: UUID,
    val taskId: UUID,
    val themeId: UUID,
    val courseId: UUID,
    val attemptsCount: Int,
    val timeSpentMs: Long,
    val createdAt: Instant? = null
)

data class AttemptRecord(
    val id: UUID,
    val userId: UUID,
    val taskId: UUID,
    val attemptsCount: Int,
    val timeSpentMs: Long,
    val createdAt: Instant
)

@Serializable
data class CourseProgressDTO(
    @Serializable(with = UUIDSerializer::class) val userId: UUID,
    @Serializable(with = UUIDSerializer::class) val courseId: UUID,
    val solvedTasks: Int,
    val totalTasks: Int,
    val percent: Double,
    val averageTimeMs: Int,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant
)

@Serializable
data class AwardedBadgeDTO(
    @Serializable(with = UUIDSerializer::class) val badgeId: UUID,
    @Serializable(with = UUIDSerializer::class) val courseId: UUID? = null,
    @Serializable(with = InstantSerializer::class) val awardedAt: Instant,
    @Serializable(with = UUIDSerializer::class) val themeId: UUID? = null
)

@Serializable
data class SolveResult(
    val removedFromQueue: Boolean,
    val movedToEnd: Boolean,
    val themeProgress: ThemeProgressDTO? = null,
    val courseProgress: CourseProgressDTO? = null,
    val newlyAwardedBadges: List<AwardedBadgeDTO> = emptyList(),
    val themeCompleted: Boolean
)

@Serializable
data class NextTaskResult(
    @Serializable(with = UUIDSerializer::class) val taskId: UUID,
    @Serializable(with = UUIDSerializer::class) val themeId: UUID
)
@Serializable
data class UserEnrichedProfile(
    val userId: String,
    val surname: String,
    val name: String,
    val patronymic: String? = null,
    val gender: Gender,
    val dob: String,
    val dor: String,
    val citizenship: String? = null,
    val nationality: String? = null,
    val countryOfResidence: String? = null,
    val cityOfResidence: String? = null,
    val countryDuringEducation: String? = null,
    val periodSpent: PeriodSpent? = null,
    val kindOfActivity: String? = null,
    val education: String? = null,
    val purposeOfRegister: String? = null,
    val avatarId: String? = null,
    val email: String,
    val systemLanguage: SystemLanguage,
    val phone: String? = null,
    val languageSkills: List<UserLanguageSkill> = emptyList()
)

@Serializable
data class UserStatsDTO(
    @Serializable(with = UUIDSerializer::class) val userId: UUID,
    val courses: List<CourseStatsDTO>
)

@Serializable
data class CourseStatsDTO(
    @Serializable(with = UUIDSerializer::class) val courseId: UUID,
    val courseProgress: CourseProgressDTO?,
    val themes: List<ThemeProgressDTO>
)

@Serializable
data class BadgeRuleDTO(
    @Serializable(with = UUIDSerializer::class) val badgeId: UUID,
    val type: String,
    @Serializable(with = UUIDSerializer::class) val courseId: UUID? = null,
    val streakDays: Int? = null,
    val active: Boolean
)

@Serializable
data class AverageProgressDTO(
    val solvedTasksAvg: Double,
    val totalTasksAvg: Double,
    val percentAvg: Double,
    val averageTimeMsAvg: Double,
    val participants: Int,
    @Serializable(with = InstantSerializer::class) val lastUpdatedAt: Instant?
)

@Serializable
data class CourseAverageStatsDTO(
    @Serializable(with = UUIDSerializer::class) val courseId: UUID,
    val courseAverage: AverageProgressDTO?,
    val themesAverage: List<ThemeAverageDTO>
)

@Serializable
data class PlatformStatsDTO(
    val totalCourses: Int,
    val totalUsersWithProgress: Int,
    val courseLevelAverage: AverageProgressDTO?,
    val themeLevelAverage: AverageProgressDTO?,
    @Serializable(with = InstantSerializer::class) val generatedAt: Instant
)

@Serializable
data class ThemeProgressDTO(
    @Serializable(with = UUIDSerializer::class) val userId: UUID,
    @Serializable(with = UUIDSerializer::class) val themeId: UUID,
    val solvedTasks: Int,
    val totalTasks: Int,
    val percent: Double,
    val averageTimeMs: Int,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant
)

@Serializable
data class ThemeAverageDTO(
    @Serializable(with = UUIDSerializer::class) val themeId: UUID,
    @Serializable(with = UUIDSerializer::class) val courseId: UUID,
    val solvedTasksAvg: Double,
    val totalTasksAvg: Double,
    val percentAvg: Double,
    val averageTimeMsAvg: Double,
    val participants: Int,
    @Serializable(with = InstantSerializer::class) val lastUpdatedAt: Instant?
)

@Serializable
data class UserAwardedBadgeDTO(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    @Serializable(with = UUIDSerializer::class) val userId: UUID,
    @Serializable(with = UUIDSerializer::class) val badgeId: UUID,
    @Serializable(with = UUIDSerializer::class) val courseId: UUID? = null,
    @Serializable(with = InstantSerializer::class) val awardedAt: Instant,
    val rule: BadgeRuleDTO
)

@Serializable
data class UserAttemptDTO(
    @Serializable(with = UUIDSerializer::class) val attemptId: UUID,
    @Serializable(with = UUIDSerializer::class) val taskId: UUID,
    val taskQuestion: String,
    val taskBody: TaskBody,
    val attemptsCount: Int,
    val timeSpentMs: Long,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant
)

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: UUID) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
}

object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
}