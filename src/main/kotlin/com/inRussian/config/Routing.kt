package com.inRussian.config


import com.inRussian.repositories.AdminRepository
import com.inRussian.repositories.ContentRepository
import com.inRussian.repositories.ExposedAdminRepository
import com.inRussian.repositories.ExposedContentRepository
import com.inRussian.repositories.ExposedStaffProfileRepository
import com.inRussian.repositories.ExposedStudentRepository
import com.inRussian.repositories.ExposedUserProfileRepository
import com.inRussian.repositories.ExposedUserRepository
import com.inRussian.repositories.MediaRepository
import com.inRussian.repositories.StaffProfileRepository
import com.inRussian.repositories.StudentRepository
import com.inRussian.repositories.UserProfileRepository
import com.inRussian.repositories.UserRepository

import com.inRussian.routes.authRoutes
import com.inRussian.routes.contentRoutes
import com.inRussian.routes.expertRoutes
import com.inRussian.routes.profileRoutes
import com.inRussian.routes.studentRoutes
import com.inRussian.services.AdminService
import com.inRussian.services.AuthService
import com.inRussian.services.ContentService
import com.inRussian.services.ExpertService
import com.inRussian.services.ExpertServiceImpl
import com.inRussian.services.ProfileService
import com.inRussian.services.StudentService
import com.inRussian.services.StudentServiceImpl
import io.ktor.server.application.*

import io.ktor.server.routing.*

import com.inRussian.routes.adminRoutes
import com.inRussian.routes.contentManagerRoutes
import com.inRussian.routes.mediaRoutes
import com.inRussian.services.MediaService


fun Application.configureRouting() {
    val mediaRepository = MediaRepository()
    val mediaService = MediaService(mediaRepository)
    val userRepository: UserRepository = ExposedUserRepository()
    val profileRepository: UserProfileRepository = ExposedUserProfileRepository()
    val staffRepository: StaffProfileRepository = ExposedStaffProfileRepository()
    val authService = AuthService(userRepository, application = this)
    val profileService = ProfileService(profileRepository, staffRepository, userRepository)
    val adminRepository: AdminRepository = ExposedAdminRepository(userRepository, profileRepository, staffRepository)
    val adminService = AdminService(adminRepository, userRepository, authService)
    val contentRepository: ContentRepository = ExposedContentRepository()
    val contentService = ContentService(contentRepository)
    val expertService: ExpertService = ExpertServiceImpl(
        adminRepository = adminRepository,
    )

    val studentRepository: StudentRepository = ExposedStudentRepository()
    val studentService: StudentService = StudentServiceImpl(studentRepository)
    routing {
        authRoutes(authService)
        adminRoutes(adminService)
        profileRoutes(profileService)
        contentRoutes(contentService)
        contentManagerRoutes(contentService)
        expertRoutes(expertService)
        studentRoutes(studentService)
        mediaRoutes(mediaService)
    }

}
