package com.inRussian.config


import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable

fun Application.configureSecurity() {
    // Simple JWT config without OAuth for now
    val jwtSecret = "secret" // Your original secret
    val jwtAudience = "jwt-audience" // Your original audience
    val jwtDomain = "http://jwt-provider-domain/" // Your original domain
    val jwtRealm = "ktor sample app" // Your original realm

    install(Authentication) {
        jwt {
            realm = jwtRealm
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtDomain)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience)) JWTPrincipal(credential.payload) else null
            }
        }
    }

    // Simple session config
    install(Sessions) {
        cookie<MySession>("MY_SESSION") {
            cookie.extensions["SameSite"] = "lax"
            cookie.secure = false // HTTP only for localhost
            cookie.httpOnly = true
        }
    }
}

@Serializable
data class MySession(val count: Int = 0)