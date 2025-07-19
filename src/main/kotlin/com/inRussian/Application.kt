package com.inRussian

import com.inRussian.config.*
import com.inRussian.repositories.*
import com.inRussian.routes.adminRoutes
import com.inRussian.routes.authRoutes
import com.inRussian.routes.profileRoutes
import com.inRussian.services.AdminService
import com.inRussian.services.AuthService
import com.inRussian.services.ProfileService
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    println("Configuring application module...")
    val userRepository : UserRepository = ExposedUserRepository()
    val profileRepository : UserProfileRepository = ExposedUserProfileRepository()
    val staffRepository : StaffProfileRepository = ExposedStaffProfileRepository()
    val authService = AuthService(userRepository, application = this)
    val profileService = ProfileService(profileRepository, staffRepository, userRepository)
    val adminRepository: AdminRepository = ExposedAdminRepository(userRepository)
    val adminService = AdminService(adminRepository, userRepository, authService)

    configureSerialization()
    configureDatabase()
    configureHTTP()
    configureMonitoring()
    configureSecurity()
    routing {
        authRoutes(authService)
        profileRoutes(profileService)
        adminRoutes(adminService)
    }

    println("Application module configured successfully!")
}