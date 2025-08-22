package com.inRussian.routes

import com.inRussian.models.EmailRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.http.isSuccess
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.json.Json
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.response.respond
import io.ktor.server.routing.application

fun Route.sendMailRoutes() {
    val mailerClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = false
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }
    val token = application.environment.config.property("mailerSend.token").getString()
    val baseUrl =  environment.config.propertyOrNull("mailerSend.baseUrl")?.getString()
        ?: System.getenv("MAILERSEND_BASE_URL")

    route("/mail") {
        post("/send-mail") {
            val req = call.receive<EmailRequest>()
            val response = sendSingleEmail(
                client = mailerClient,
                url = "${baseUrl}email",
                token = token,
                emailRequest = req
            )

            println("Response status: ${response.status}")
            println("Response headers: ${response.headers}")
            val responseBody = response.bodyAsText()
            println("Response body: $responseBody")

            if (response.status.isSuccess()) {
                println("Email sent")
                call.respond(HttpStatusCode.OK, "Email sent")
            } else {
                println("Error sending email")
                call.respond(HttpStatusCode.InternalServerError, "Failed to send email")
            }
        }
    }
}

suspend fun sendSingleEmail(client: HttpClient, url: String, token: String, emailRequest: EmailRequest) =
    client.post(url) {
        headers {
            append(Authorization, "Bearer $token")
        }

        contentType(ContentType.Application.Json)
        setBody(emailRequest)
    }



