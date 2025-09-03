package com.inRussian.models.v2


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
    val sectionId: UUID,
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
data class SectionProgressDTO(
    @Serializable(with = UUIDSerializer::class) val userId: UUID,
    @Serializable(with = UUIDSerializer::class) val sectionId: UUID,
    val solvedTasks: Int,
    val totalTasks: Int,
    val percent: Double,
    val averageTimeMs: Int,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant
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
    @Serializable(with = UUIDSerializer::class) val sectionId: UUID? = null,
    @Serializable(with = UUIDSerializer::class) val courseId: UUID? = null
)

@Serializable
data class SolveResult(
    val removedFromQueue: Boolean,
    val movedToEnd: Boolean,
    val sectionProgress: SectionProgressDTO? = null,
    val courseProgress: CourseProgressDTO? = null,
    val newlyAwardedBadges: List<AwardedBadgeDTO> = emptyList(),
    val sectionCompleted: Boolean
)

@Serializable
data class NextTaskResult(
    @Serializable(with = UUIDSerializer::class) val taskId: UUID,
    @Serializable(with = UUIDSerializer::class) val sectionId: UUID,
    @Serializable(with = UUIDSerializer::class) val themeId: UUID
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
    val sections: List<SectionProgressDTO>
)
@Serializable
data class BadgeRuleDTO(
    @Serializable(with = UUIDSerializer::class) val badgeId: UUID,
    val type: String,
    @Serializable(with = UUIDSerializer::class) val sectionId: UUID? = null,
    @Serializable(with = UUIDSerializer::class) val courseId: UUID? = null,
    val streakDays: Int? = null,
    val active: Boolean
)


@Serializable
data class UserAwardedBadgeDTO(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    @Serializable(with = UUIDSerializer::class) val userId: UUID,
    @Serializable(with = UUIDSerializer::class) val badgeId: UUID,
    @Serializable(with = UUIDSerializer::class) val sectionId: UUID? = null,
    @Serializable(with = UUIDSerializer::class) val courseId: UUID? = null,
    @Serializable(with = InstantSerializer::class) val awardedAt: Instant,
    val rule: BadgeRuleDTO
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