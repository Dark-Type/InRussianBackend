package com.inRussian.routes.v2

import com.inRussian.requests.v2.AttemptRequest
import com.inRussian.services.v2.*
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable
import java.util.UUID

fun Route.themeRoutes(
    queueService: QueueService,
    progressService: ProgressService
) {
    authenticate("auth-jwt") {
        get<ThemeResource.NextTask> { res ->
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.payload.getClaim("userId").asString()

            val themeId = res.parent.themeId.toUuidOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid themeId")

            val next = queueService.getOrSeedNextTask(userId.toUuid(), themeId)
            if (next == null) call.respond(HttpStatusCode.NoContent) else call.respond(next)
        }

        get<ThemeResource.Progress> { res ->
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.payload.getClaim("userId").asString()

            val themeId = res.parent.themeId.toUuidOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid themeId")

            val progress = progressService.themeProgress(userId.toUuid(), themeId)
            call.respond(progress)
        }
    }
}

fun Route.courseRoutes(
    progressService: ProgressService
) {
    authenticate("auth-jwt") {
        get<CourseResource.Progress> { res ->
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.payload.getClaim("userId").asString()

            val courseId = res.parent.courseId.toUuidOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid courseId")

            val progress = progressService.courseProgress(userId.toUuid(), courseId)
            call.respond(progress)
        }
    }
}

fun Route.attemptRoutes(
    solveService: SolveService
) {
    authenticate("auth-jwt") {
        post<AttemptResource.Create> {
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.payload.getClaim("userId").asString()
            val body = call.receive<AttemptRequest>()

            if (body.attemptsCount < 1 || body.timeSpentMs < 0) {
                call.respond(HttpStatusCode.BadRequest, "Invalid attemptsCount or timeSpentMs")
                return@post
            }

            val result = solveService.submitSolved(
                SolveService.SubmitParams(
                    attemptId = body.attemptId,
                    userId = UUID.fromString(userId),
                    taskId = body.taskId,
                    attemptsCount = body.attemptsCount,
                    timeSpentMs = body.timeSpentMs
                )
            )

            call.respond(result)
        }
    }
}

fun Route.badgeRoutes(
    badgesQueryService: BadgesQueryService
) {
    authenticate("auth-jwt") {
        get<BadgeResource.MyBadges> {
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.payload.getClaim("userId").asString()
            val badges = badgesQueryService.listUserBadges(userId.toUuid())
            call.respond(badges)
        }

        get<BadgeResource.ById> { res ->
            val badgeId = res.badgeId.toUuidOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid badgeId")

            val rules = badgesQueryService.getBadgeRules(badgeId)
            if (rules.isEmpty()) call.respond(HttpStatusCode.NotFound, "Badge rules not found")
            else call.respond(rules)
        }
    }
}

fun Route.statsRoutes(
    statsService: StatsService
) {
    authenticate("auth-jwt") {
        get<UserStatsResource> { res ->
            val userId = res.userId.toUuidOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid userId")
            val stats = statsService.userStats(userId)
            call.respond(stats)
        }

        get<CourseStatsResource> { res ->
            val courseId = res.courseId.toUuidOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid courseId")
            val stats = statsService.courseAverageStats(courseId)
            call.respond(stats)
        }

        get<PlatformStatsResource> {
            val stats = statsService.platformStats()
            call.respond(stats)
        }
    }
}

fun Route.userAttemptRoutes(
    userAttemptService: UserAttemptService
) {
    authenticate("auth-jwt") {
        get<UserAttemptResource.MyAttempts> { res ->
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.payload.getClaim("userId").asString()

            val themeId = res.parent.themeId.toUuidOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid themeId")

            val attempts = userAttemptService.getUserThemeAttempts(userId.toUuid(), themeId)
            call.respond(attempts)
        }
    }
}

// -------- Resources --------

@Serializable
@Resource("/themes/{themeId}")
class ThemeResource(val themeId: String) {
    @Serializable @Resource("tasks/next")
    class NextTask(val parent: ThemeResource)

    @Serializable @Resource("progress")
    class Progress(val parent: ThemeResource)
}

@Serializable
@Resource("/courses/{courseId}")
class CourseResource(val courseId: String) {
    @Serializable @Resource("progress")
    class Progress(val parent: CourseResource)
}

@Serializable
@Resource("/attempts")
class AttemptResource {
    @Serializable @Resource("")
    class Create(val parent: AttemptResource = AttemptResource())
}

@Serializable
@Resource("/me/badges")
class BadgeResource {
    @Serializable @Resource("")
    class MyBadges(val parent: BadgeResource = BadgeResource())

    @Serializable @Resource("/badges/{badgeId}")
    class ById(val badgeId: String)
}

@Serializable
@Resource("/users/{userId}/stats")
class UserStatsResource(val userId: String)

@Serializable
@Resource("/course/{courseId}/stats")
class CourseStatsResource(val courseId: String)

@Serializable
@Resource("/platform/stats")
class PlatformStatsResource

@Serializable
@Resource("/themes/{themeId}/my-attempts")
class UserAttemptResource(val themeId: String) {
    @Serializable @Resource("")
    class MyAttempts(val parent: UserAttemptResource)
}

// -------- Helpers --------

private fun String.toUuid(): UUID = UUID.fromString(this)
private fun String.toUuidOrNull(): UUID? = runCatching { UUID.fromString(this) }.getOrNull()