package com.inRussian.config

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Application.configureRouting() {
    install(AutoHeadResponse)
    install(RequestValidation) {
        validate<String> { bodyText ->
            if (!bodyText.startsWith("Hello"))
                ValidationResult.Invalid("Body text should start with 'Hello'")
            else ValidationResult.Valid
        }
    }

    routing {
        // Health check
        get("/health") {
            call.respondText("OK - Server is healthy")
        }



        authenticate("expert-jwt") {
            route("/expert") {
                // Get all student models
                get("/students/models") {
                    // Query params: page, size, sortBy, sortOrder
                }

                // Get student progress for all courses
                get("/students/progress") {
                    // Query params: studentId, courseId, page, size, sortBy, sortOrder
                }

                // Get educational content structure
                get("/courses") {
                    // Get all courses
                }

                get("/courses/{courseId}/sections") {
                    // Get sections for a course
                }

                get("/sections/{sectionId}/themes") {
                    // Get themes for a section
                }

                get("/themes/{themeId}/tasks") {
                    // Get tasks for a theme
                }
            }
        }

        authenticate("content-jwt") {
            route("/content") {
                // Course management
                route("/courses") {
                    post {
                        // Create course
                    }

                    get {
                        // Get all courses with pagination
                    }

                    get("/{courseId}") {
                        // Get specific course
                    }

                    put("/{courseId}") {
                        // Update course
                    }

                    delete("/{courseId}") {
                        // Delete course
                    }
                }

                // Section management
                route("/sections") {
                    post {
                        // Create section
                    }

                    get {
                        // Get all sections
                    }

                    get("/{sectionId}") {
                        // Get specific section
                    }

                    put("/{sectionId}") {
                        // Update section
                    }

                    delete("/{sectionId}") {
                        // Delete section
                    }
                }

                // Theme management
                route("/themes") {
                    post {
                        // Create theme
                    }

                    get {
                        // Get all themes
                    }

                    get("/{themeId}") {
                        // Get specific theme
                    }

                    put("/{themeId}") {
                        // Update theme
                    }

                    delete("/{themeId}") {
                        // Delete theme
                    }
                }

                // Task management
                route("/tasks") {
                    post {
                        // Create task with content, answers, options
                    }

                    get {
                        // Get all tasks
                    }

                    get("/{taskId}") {
                        // Get specific task
                    }

                    put("/{taskId}") {
                        // Update task
                    }

                    delete("/{taskId}") {
                        // Delete task
                    }

                    // Task content management
                    post("/{taskId}/content") {
                        // Add content to task
                    }

                    put("/{taskId}/content/{contentId}") {
                        // Update task content
                    }

                    delete("/{taskId}/content/{contentId}") {
                        // Delete task content
                    }

                    // Answer management
                    post("/{taskId}/answers") {
                        // Add answer options
                    }

                    put("/{taskId}/answers/{answerId}") {
                        // Update answer
                    }

                    delete("/{taskId}/answers/{answerId}") {
                        // Delete answer
                    }
                }

                // User reports for tasks
                get("/reports/tasks") {
                    // Query params: taskId, userId, startDate, endDate, page, size, sortBy, sortOrder
                }
            }
        }

        authenticate("auth-jwt") {
            route("/media") {
                // Upload media
                post {
                    // Create/upload media file
                }

                // Get media by link/id (images, audio)
                get("/{mediaId}") {
                    // Serve media file
                }

                // Separate video endpoint for better performance
                get("/video/{videoId}") {
                    // Serve video file with streaming support
                }

                // Get media metadata
                get("/{mediaId}/info") {
                    // Get media information
                }
            }
        }


        authenticate("student-jwt") {
            route("/student") {
                // Profile management
                route("/profile") {
                    // Fill/update personal data
                    post("/data") {
                        // Fill initial personal data
                    }

                    // Get personal info
                    get {
                        // Get student profile
                    }

                    // Update personal info
                    put {
                        // Update student information
                    }
                }

                // Course interaction
                route("/courses") {
                    // Get all available courses
                    get {
                        // Query params: page, size, enrolled, available
                    }

                    // Get specific course
                    get("/{courseId}") {
                        // Get course details
                    }

                    // Enroll in course
                    post("/{courseId}/enroll") {
                        // Enroll student in course
                    }

                    // Unenroll from course
                    delete("/{courseId}/enroll") {
                        // Remove enrollment
                    }

                    // Get course progress
                    get("/{courseId}/progress") {
                        // Get student progress for course
                    }

                    // Get course sections
                    get("/{courseId}/sections") {
                        // Get sections for enrolled course
                    }
                }

                // Section interaction
                route("/sections") {
                    get("/{sectionId}") {
                        // Get section details
                    }

                    get("/{sectionId}/progress") {
                        // Get section progress
                    }

                    get("/{sectionId}/themes") {
                        // Get themes in section
                    }
                }

                // Theme interaction
                route("/themes") {
                    get("/{themeId}") {
                        // Get theme details
                    }

                    get("/{themeId}/tasks") {
                        // Get tasks in theme
                    }
                }

                // Task interaction
                route("/tasks") {
                    get("/{taskId}") {
                        // Get specific task
                    }

                    // Complete task
                    post("/{taskId}/complete") {
                        // Mark task as completed with answer
                    }

                    // Fail task
                    post("/{taskId}/fail") {
                        // Mark task as failed
                    }

                    // Get task queue
                    get("/queue") {
                        // Get next tasks to complete
                    }

                    // Report task
                    post("/{taskId}/report") {
                        // Report issue with task
                    }
                }

                // General progress
                get("/progress") {
                    // Get overall student progress
                    // Query params: courseId, sectionId
                }
            }
        }

        // Test endpoint (keep for development)
        get("/test") {
            call.respondText("Test endpoint working")
        }
    }
}