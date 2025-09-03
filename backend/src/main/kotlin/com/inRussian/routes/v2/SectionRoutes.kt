package com.inRussian.routes.v2

import com.inRussian.requests.v2.AttemptRequest
import com.inRussian.services.v2.BadgesQueryService
import com.inRussian.services.v2.ProgressService
import com.inRussian.services.v2.QueueService
import com.inRussian.services.v2.SolveService
import com.inRussian.services.v2.StatsService
import com.inRussian.services.v2.UserAttemptService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import java.util.UUID

fun Route.sectionRoutes(
    queueService: QueueService,
    progressService: ProgressService
) {
    authenticate("auth-jwt") {
        get("/sections/{sectionId}/tasks/next") {
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.payload.getClaim("userId").asString()
            val sectionId = call.parameters["sectionId"]?.let(UUID::fromString)
            if (sectionId == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid sectionId")
                return@get
            }

            val next = queueService.getOrSeedNextTask(userId.let(UUID::fromString), sectionId)
            if (next == null) {
                call.respond(HttpStatusCode.NoContent)
                return@get
            }
            call.respond(next)
        }

        get("/sections/{sectionId}/progress") {
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.payload.getClaim("userId").asString()
            val sectionId = call.parameters["sectionId"]?.let(UUID::fromString)
            if (sectionId == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid sectionId")
                return@get
            }
            val progress = progressService.sectionProgress(UUID.fromString(userId), sectionId)
            call.respond(progress)
        }
    }
}

fun Route.courseRoutes(
    progressService: ProgressService
) {
    authenticate("auth-jwt") {
        get("/courses/{courseId}/progress") {
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.payload.getClaim("userId").asString()
            val courseId = call.parameters["courseId"]?.let(UUID::fromString)
            if (courseId == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid courseId")
                return@get
            }
            val progress = progressService.courseProgress(UUID.fromString(userId), courseId)
            call.respond(progress)
        }
    }
}

fun Route.attemptRoutes(
    solveService: SolveService
) {
    authenticate("auth-jwt") {
        post("/attempts") {
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
                    userId = userId.let(UUID::fromString),
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
        get("/me/badges") {
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.payload.getClaim("userId").asString()
            val badges = badgesQueryService.listUserBadges(userId.let(UUID::fromString))
            call.respond(badges)
        }

        get("/badges/{badgeId}") {
            val badgeId = call.parameters["badgeId"].let(UUID::fromString)
            if (badgeId == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid badgeId")
                return@get
            }
            val rules = badgesQueryService.getBadgeRules(badgeId)
            if (rules.isEmpty()) {
                call.respond(HttpStatusCode.NotFound, "Badge rules not found")
            } else {
                call.respond(rules)
            }
        }
    }
}

fun Route.statsRoutes(
    statsService: StatsService
) {
    authenticate("auth-jwt") {
        get("/users/{userId}/stats") {
            val userId = call.parameters["userId"]?.let(UUID::fromString)
            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid userId")
                return@get
            }
            val stats = statsService.userStats(userId)
            call.respond(stats)
        }

        get("/course/{courseId}/stats") {
            val courseId = call.parameters["courseId"]?.let(UUID::fromString)
            if (courseId == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid courseId")
                return@get
            }
            val stats = statsService.courseAverageStats(courseId)
            call.respond(stats)
        }

        get("/platform/stats") {
            val stats = statsService.platformStats()
            call.respond(stats)
        }
    }
}
fun Route.userAttemptRoutes(
    userAttemptService: UserAttemptService
) {
    authenticate("auth-jwt") {
        get("/sections/{sectionId}/my-attempts") {
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.payload.getClaim("userId").asString()
            val sectionId = call.parameters["sectionId"]?.let(UUID::fromString)

            if (sectionId == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid sectionId")
                return@get
            }

            val attempts = userAttemptService.getUserSectionAttempts(
                UUID.fromString(userId),
                sectionId
            )
            call.respond(attempts)
        }
    }
}