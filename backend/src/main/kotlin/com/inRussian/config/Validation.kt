package com.inRussian.config

import com.inRussian.responses.common.ErrorResponse
import com.inRussian.responses.common.ValidationErrorDetail
import com.inRussian.utils.exceptions.AccountDeactivatedException
import com.inRussian.utils.exceptions.AccountSuspendedException
import com.inRussian.utils.exceptions.AdminAlreadyExistsException
import com.inRussian.utils.exceptions.ConfigurationException
import com.inRussian.utils.exceptions.DatabaseException
import com.inRussian.utils.exceptions.InvalidCredentialsException
import com.inRussian.utils.exceptions.InvalidTokenException
import com.inRussian.utils.exceptions.UserAlreadyExistsException
import com.inRussian.utils.exceptions.UserNotFoundException
import com.inRussian.utils.validation.ValidationException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.configureValidation() {
    install(StatusPages) {
        exception<ValidationException> { call, cause ->
            call.respond(
                HttpStatusCode.UnprocessableEntity,
                ErrorResponse(
                    success = false,
                    error = "validation_failed",
                    code = "VALIDATION_ERROR",
                    timestamp = System.currentTimeMillis(),
                    details = cause.errors.map {
                        ValidationErrorDetail(
                            field = it.field,
                            code = it.code,
                            message = it.message
                        )
                    }
                )
            )
        }

        exception<UserAlreadyExistsException> { call, cause ->
            call.respond(
                HttpStatusCode.Conflict,
                ErrorResponse(success = false, error = cause.message, code = cause.code, timestamp = System.currentTimeMillis())
            )
        }

        exception<UserNotFoundException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(success = false, error = cause.message, code = cause.code, timestamp = System.currentTimeMillis())
            )
        }

        exception<InvalidCredentialsException> { call, cause ->
            call.respond(
                HttpStatusCode.Unauthorized,
                ErrorResponse(success = false, error = cause.message, code = cause.code, timestamp = System.currentTimeMillis())
            )
        }

        exception<AccountSuspendedException> { call, cause ->
            call.respond(
                HttpStatusCode.Forbidden,
                ErrorResponse(success = false, error = cause.message, code = cause.code, timestamp = System.currentTimeMillis())
            )
        }

        exception<AccountDeactivatedException> { call, cause ->
            call.respond(
                HttpStatusCode.Forbidden,
                ErrorResponse(success = false, error = cause.message, code = cause.code, timestamp = System.currentTimeMillis())
            )
        }

        exception<InvalidTokenException> { call, cause ->
            call.respond(
                HttpStatusCode.Unauthorized,
                ErrorResponse(success = false, error = cause.message, code = cause.code, timestamp = System.currentTimeMillis())
            )
        }

        exception<AdminAlreadyExistsException> { call, cause ->
            call.respond(
                HttpStatusCode.Conflict,
                ErrorResponse(success = false, error = cause.message, code = cause.code, timestamp = System.currentTimeMillis())
            )
        }

        exception<ConfigurationException> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(success = false, error = cause.message, code = cause.code, timestamp = System.currentTimeMillis())
            )
        }

        exception<DatabaseException> { call, cause ->
            call.application.environment.log.error("Database error", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(success = false, error = "Database error occurred", code = cause.code, timestamp = System.currentTimeMillis())
            )
        }

        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(success = false, error = cause.message ?: "Invalid argument", code = "BAD_REQUEST", timestamp = System.currentTimeMillis())
            )
        }

        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(success = false, error = "Internal server error", code = "INTERNAL_ERROR", timestamp = System.currentTimeMillis())
            )
        }
    }
}