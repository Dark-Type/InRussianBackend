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

    fun validateCredential(credential: JWTCredential, requiredRole: UserRole? = null): JWTPrincipal? {
        val userId = credential.payload.getClaim("userId").asString()
        val role = credential.payload.getClaim("role").asString()
        val status = credential.payload.getClaim("status").asString()
        if (credential.payload.audience.contains(jwtAudience)
            && userId != null
            && role != null
            && status == UserStatus.ACTIVE.name
            && (requiredRole == null || role == requiredRole.name)
        ) {
            return JWTPrincipal(credential.payload)
        }
        return null
    }

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
                validateCredential(credential, requiredRole = null)
            }

            challenge { _, _ ->
                call.respondText(
                    "Token is not valid, expired or account is not active",
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
                validateCredential(credential, requiredRole = UserRole.ADMIN)
            }
            challenge { _, _ ->
                call.respondText("Admin access required or account is not active", status = HttpStatusCode.Forbidden)
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
                validateCredential(credential, requiredRole = UserRole.EXPERT)
            }
            challenge { _, _ ->
                call.respondText("Expert access required or account is not active", status = HttpStatusCode.Forbidden)
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
                validateCredential(credential, requiredRole = UserRole.CONTENT_MODERATOR)
            }
            challenge { _, _ ->
                call.respondText(
                    "Content moderator access required or account is not active",
                    status = HttpStatusCode.Forbidden
                )
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
                validateCredential(credential, requiredRole = UserRole.STUDENT)
            }
            challenge { _, _ ->
                call.respondText("Student access required or account is not active", status = HttpStatusCode.Forbidden)
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