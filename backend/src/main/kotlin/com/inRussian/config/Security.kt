package com.inRussian.config


import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.inRussian.models.users.UserRole
import com.inRussian.models.users.UserStatus
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import java.util.*


fun Application.configureSecurity() {
    val jwtSecret = environment.config.propertyOrNull("jwt.secret")?.getString() ?: "your-secret-key"
    val jwtAudience = environment.config.propertyOrNull("jwt.audience")?.getString() ?: "inrussian-api"
    val jwtDomain = environment.config.propertyOrNull("jwt.domain")?.getString() ?: "http://localhost:8080/"
    val jwtRealm = "InRussian API"

    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtRealm
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtDomain)
                    .build()
            )

            validate { credential ->

                val userId = credential.payload.getClaim("userId").asString()
                val userRole = credential.payload.getClaim("role").asString()

                if (credential.payload.audience.contains(jwtAudience) && userId != null && userRole != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }

            challenge { defaultScheme, realm ->
                call.respondText(
                    "Token is not valid or has expired",
                    status = HttpStatusCode.Unauthorized
                )
            }
        }
        jwt("admin-jwt") {
            realm = jwtRealm
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtDomain)
                    .build()
            )
            validate { credential ->
                val userRole = credential.payload.getClaim("role").asString()
                if (credential.payload.audience.contains(jwtAudience) && userRole == UserRole.ADMIN.name) {
                    JWTPrincipal(credential.payload)
                } else null
            }
            challenge { defaultScheme, realm ->
                call.respondText("Admin access required", status = HttpStatusCode.Forbidden)
            }
        }

        jwt("expert-jwt") {
            realm = jwtRealm
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtDomain)
                    .build()
            )
            validate { credential ->
                val userRole = credential.payload.getClaim("role").asString()
                if (credential.payload.audience.contains(jwtAudience) && userRole == UserRole.EXPERT.name) {
                    JWTPrincipal(credential.payload)
                } else null
            }
            challenge { defaultScheme, realm ->
                call.respondText("Expert access required", status = HttpStatusCode.Forbidden)
            }
        }

        jwt("content-jwt") {
            realm = jwtRealm
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtDomain)
                    .build()
            )
            validate { credential ->
                val userRole = credential.payload.getClaim("role").asString()
                if (credential.payload.audience.contains(jwtAudience) && userRole == UserRole.CONTENT_MODERATOR.name) {
                    JWTPrincipal(credential.payload)
                } else null
            }
            challenge { defaultScheme, realm ->
                call.respondText("Content moderator access required", status = HttpStatusCode.Forbidden)
            }
        }

        jwt("student-jwt") {
            realm = jwtRealm
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtDomain)
                    .build()
            )
            validate { credential ->
                val userRole = credential.payload.getClaim("role").asString()
                if (credential.payload.audience.contains(jwtAudience) && userRole == UserRole.STUDENT.name) {
                    JWTPrincipal(credential.payload)
                } else null
            }
            challenge { defaultScheme, realm ->
                call.respondText("Student access required", status = HttpStatusCode.Forbidden)
            }
        }
    }
}

object JWTConfig {
    fun generateAccessToken(
        userId: String,
        email: String,
        role: UserRole,
        status: UserStatus,
        secret: String,
        audience: String,
        issuer: String,
        expiresInMinutes: Long = 120
    ): String {
        println("=== GENERATING ACCESS TOKEN ===")
        println("UserId: $userId")
        println("Role: ${role.name}")
        println("Status: ${status.name}")
        println("Secret: ${secret.take(10)}...")
        println("Audience: $audience")
        println("Issuer: $issuer")

        val token = JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withClaim("role", role.name)
            .withClaim("status", status.name)
            .withExpiresAt(Date(System.currentTimeMillis() + expiresInMinutes * 60000))
            .withIssuedAt(Date())
            .sign(Algorithm.HMAC256(secret))

        println("Generated token (first 50 chars): ${token.take(50)}...")
        return token
    }

    fun generateRefreshToken(
        userId: String,
        secret: String,
        audience: String,
        issuer: String,
        expiresInDays: Long = 14
    ): String {
        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("userId", userId)
            .withClaim("type", "refresh")
            .withExpiresAt(Date(System.currentTimeMillis() + expiresInDays * 24 * 60 * 60000))
            .withIssuedAt(Date())
            .sign(Algorithm.HMAC256(secret))
    }
}

fun JWTPrincipal.getUserId(): String? = payload.getClaim("userId").asString()
fun JWTPrincipal.getUserRole(): UserRole? = payload.getClaim("role").asString()?.let { UserRole.valueOf(it) }
fun JWTPrincipal.getUserEmail(): String? = payload.getClaim("email").asString()
fun JWTPrincipal.getUserStatus(): UserStatus? = payload.getClaim("status").asString()?.let { UserStatus.valueOf(it) }