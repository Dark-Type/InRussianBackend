package com.inRussian.config


import com.inRussian.repositories.AdminRepository
import com.inRussian.repositories.ContentRepository
import com.inRussian.repositories.ExposedAdminRepository
import com.inRussian.repositories.ExposedContentRepository
import com.inRussian.repositories.ExposedPasswordRecoveryTokenRepository
import com.inRussian.repositories.ExposedStaffProfileRepository
import com.inRussian.repositories.ExposedStudentRepository
import com.inRussian.repositories.ExposedUserProfileRepository
import com.inRussian.repositories.ExposedUserRepository
import com.inRussian.repositories.MediaRepository
import com.inRussian.repositories.PasswordRecoveryTokenRepository
import com.inRussian.repositories.StaffProfileRepository
import com.inRussian.repositories.StudentRepository
import com.inRussian.repositories.TaskRepository
import com.inRussian.repositories.UserProfileRepository
import com.inRussian.repositories.UserRepository
import com.inRussian.repositories.v2.AttemptRepository
import com.inRussian.repositories.v2.BadgeRepository
import com.inRussian.repositories.v2.ProgressRepository
import com.inRussian.repositories.v2.QueueRepository
import com.inRussian.repositories.v2.SectionCompletionRepository
import com.inRussian.repositories.v2.StatsRepository
import com.inRussian.repositories.v2.TaskStateRepository

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
import com.inRussian.routes.passwordRecoveryRoutes
import com.inRussian.routes.taskRoutes
import com.inRussian.routes.v2.attemptRoutes
import com.inRussian.routes.v2.badgeRoutes
import com.inRussian.routes.v2.courseRoutes
import com.inRussian.routes.v2.sectionRoutes
import com.inRussian.routes.v2.statsRoutes
import com.inRussian.routes.v2.userAttemptRoutes
import com.inRussian.services.MediaService
import com.inRussian.services.mailer.GmailMailer
import com.inRussian.services.mailer.Mailer
import com.inRussian.services.v2.BadgeService
import com.inRussian.services.v2.BadgesQueryService
import com.inRussian.services.v2.ProgressService
import com.inRussian.services.v2.QueueService
import com.inRussian.services.v2.SolveService
import com.inRussian.services.v2.StatsService
import com.inRussian.services.v2.UserAttemptService


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
    val attemptRepo = AttemptRepository()
    val stateRepo = TaskStateRepository()
    val queueRepo = QueueRepository()
    val progressRepo = ProgressRepository()
    val badgeRepo = BadgeRepository()
    val completionRepo = SectionCompletionRepository()
    val statsRepo = StatsRepository()
    val badgesQueryService = BadgesQueryService()


    // Services
    val queueService = QueueService(queueRepo)
    val progressService = ProgressService(progressRepo)
    val badgeService = BadgeService(badgeRepo)
    val solveService = SolveService(
        attemptRepo = attemptRepo,
        stateRepo = stateRepo,
        queueRepo = queueRepo,
        progressRepo = progressRepo,
        badgeService = badgeService,
        completionRepo = completionRepo
    )
    val statsService = StatsService(statsRepo, progressRepo)
    val mailer: Mailer = GmailMailer(
        host = environment.config.property("mailer.host").getString(),
        port = environment.config.property("mailer.port").getString().toInt(),
        username = environment.config.property("mailer.username").getString(),
        appPassword = environment.config.property("mailer.appPassword").getString(),
        from = environment.config.property("mailer.from").getString(),
        useTls = environment.config.property("mailer.useTls").getString().toBoolean()
    )
    val recoveryRepo: PasswordRecoveryTokenRepository = ExposedPasswordRecoveryTokenRepository()

    val userAttemptService = UserAttemptService(
        attemptRepo = attemptRepo
    )

    val studentRepository: StudentRepository = ExposedStudentRepository()
    val studentService: StudentService = StudentServiceImpl(studentRepository)
    routing {
        authRoutes(authService)
        passwordRecoveryRoutes(mailer, recoveryRepo, userRepository)
        adminRoutes(adminService)
        profileRoutes(profileService)
        contentRoutes(contentService)
        contentManagerRoutes(contentService)
        expertRoutes(expertService)
        studentRoutes(studentService)
        mediaRoutes(mediaService)
        taskRoutes(TaskRepository())
        sectionRoutes(queueService, progressService)
        courseRoutes(progressService)
        attemptRoutes(solveService)
        badgeRoutes(badgesQueryService)
        statsRoutes(statsService)
        userAttemptRoutes(userAttemptService)
    }

}
