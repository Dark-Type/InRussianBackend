package com.inRussian.config


import com.inRussian.repositories.AttemptRepository
import com.inRussian.repositories.BadgeRepository
import com.inRussian.repositories.ContentStatsRepository
import com.inRussian.repositories.CoursesRepository
import com.inRussian.repositories.EmailTokenRepository
import com.inRussian.repositories.ExposedAttemptRepository
import com.inRussian.repositories.ExposedBadgeRepository
import com.inRussian.repositories.ExposedContentStatsRepository
import com.inRussian.repositories.ExposedCoursesRepository
import com.inRussian.repositories.ExposedEmailTokenRepository
import com.inRussian.repositories.ExposedMediaRepository
import com.inRussian.repositories.ExposedProgressRepository
import com.inRussian.repositories.ExposedProgressStatsRepository
import com.inRussian.repositories.ExposedQueueRepository
import com.inRussian.repositories.ExposedReportsRepository
import com.inRussian.repositories.ExposedTaskStateRepository
import com.inRussian.repositories.ExposedTasksRepository
import com.inRussian.repositories.ExposedThemesRepository
import com.inRussian.repositories.ExposedUserCourseEnrollmentsRepository
import com.inRussian.repositories.ExposedUserCourseStatisticsRepository
import com.inRussian.repositories.ExposedUserLanguageSkillsRepository
import com.inRussian.repositories.ExposedUserProfilesRepository
import com.inRussian.repositories.ExposedUserStatisticsRepository
import com.inRussian.repositories.ExposedUsersRepository
import com.inRussian.repositories.MediaRepository
import com.inRussian.repositories.ProgressRepository
import com.inRussian.repositories.ProgressStatsRepository
import com.inRussian.repositories.QueueRepository
import com.inRussian.repositories.ReportsRepository
import com.inRussian.repositories.RetrySwitchRepository
import com.inRussian.repositories.RetrySwitchRepositoryImplementation
import com.inRussian.repositories.TaskStateRepository
import com.inRussian.repositories.TasksRepository
import com.inRussian.repositories.ThemesRepository
import com.inRussian.repositories.UserCourseEnrollmentsRepository
import com.inRussian.repositories.UserCourseStatisticsRepository
import com.inRussian.repositories.UserLanguageSkillsRepository
import com.inRussian.repositories.UserProfilesRepository
import com.inRussian.repositories.UserStatisticsRepository
import com.inRussian.repositories.UsersRepository
import com.inRussian.routes.studentRoutes
import com.inRussian.routes.taskRoutes
import com.inRussian.routes.v2.attemptRoutes
import com.inRussian.routes.v2.badgeRoutes
import com.inRussian.routes.v2.configurationRoutes
import com.inRussian.routes.v2.courseRoutes
import com.inRussian.routes.v2.statsRoutes
import com.inRussian.routes.v2.themeRoutes
import com.inRussian.routes.v2.userAttemptRoutes
import com.inRussian.routes.v3.expert.expertRoutes
import com.inRussian.routes.v3.admin.adminRoutes
import com.inRussian.routes.v3.auth.authRoutes
import com.inRussian.routes.v3.content.contentRoutes
import com.inRussian.routes.v3.content_manager.contentManagerRoutes
import com.inRussian.routes.v3.mailing.mailingRoutes
import com.inRussian.routes.v3.media.mediaRoutes
import com.inRussian.routes.v3.profile.profileRoutes
import com.inRussian.services.v3.ExpertService
import com.inRussian.services.v3.ExpertServiceImpl
import com.inRussian.services.v3.ContentService
import com.inRussian.services.v3.AdminService
import com.inRussian.services.v3.AuthService
import com.inRussian.services.v3.AuthServiceImplementation
import com.inRussian.services.mailer.GmailMailer
import com.inRussian.services.mailer.Mailer
import com.inRussian.services.v2.BadgeService
import com.inRussian.services.v2.BadgesQueryService
import com.inRussian.services.v2.ProgressService
import com.inRussian.services.v2.QueueService
import com.inRussian.services.v2.RetryService
import com.inRussian.services.v2.SolveService
import com.inRussian.services.v2.StatsService
import com.inRussian.services.v2.UserAttemptService
import com.inRussian.services.v3.MediaService
import com.inRussian.services.v3.ProfileService
import com.inRussian.services.v3.StudentService
import com.inRussian.services.v3.StudentServiceImpl
import io.ktor.server.application.*

import io.ktor.server.routing.*


fun Application.configureRouting() {
    val mediaRepository: MediaRepository = ExposedMediaRepository()
    val mediaService = MediaService(mediaRepository)
    val userRepository: UsersRepository = ExposedUsersRepository()
    val mailer: Mailer = GmailMailer(
        host = environment.config.property("mailer.host").getString(),
        port = environment.config.property("mailer.port").getString().toInt(),
        username = environment.config.property("mailer.username").getString(),
        appPassword = environment.config.property("mailer.appPassword").getString(),
        from = environment.config.property("mailer.from").getString(),
        useTls = environment.config.property("mailer.useTls").getString().toBoolean()
    )
    val recoveryRepo: EmailTokenRepository =
        ExposedEmailTokenRepository()
    val authService: AuthService =
        AuthServiceImplementation(userRepository, application = this, recoveryRepo = recoveryRepo, mailer = mailer)
    val enrollmentsRepository: UserCourseEnrollmentsRepository = ExposedUserCourseEnrollmentsRepository()
    val progressStatsRepository: ProgressStatsRepository = ExposedProgressStatsRepository()
    val userProfilesRepository: UserProfilesRepository = ExposedUserProfilesRepository()
    val userLanguageSkillsRepository: UserLanguageSkillsRepository = ExposedUserLanguageSkillsRepository()
    val profileService = ProfileService(userRepository, userProfilesRepository, userLanguageSkillsRepository)
    val adminService = AdminService(userRepository, authService, enrollmentsRepository, progressStatsRepository)
    val tasksRepository: TasksRepository = ExposedTasksRepository(appJson)
    val themesRepository: ThemesRepository = ExposedThemesRepository(tasksRepository)
    val coursesRepository: CoursesRepository = ExposedCoursesRepository()
    val contentStatsRepository: ContentStatsRepository =
        ExposedContentStatsRepository(tasksRepository, themesRepository)
    val reportsRepository: ReportsRepository = ExposedReportsRepository()
    val userCourseStatisticsRepository: UserCourseStatisticsRepository = ExposedUserCourseStatisticsRepository()
    val userStatisticsRepository: UserStatisticsRepository = ExposedUserStatisticsRepository()
    val contentService: ContentService = ContentService(
        themesRepository = themesRepository,
        tasksRepository = tasksRepository,
        coursesRepository = coursesRepository,
        contentStatsRepository = contentStatsRepository,
        reportsRepository = reportsRepository
    )
    val expertService: ExpertService = ExpertServiceImpl(
        usersRepository = userRepository,
        userCourseEnrollmentsRepository = enrollmentsRepository,
        userCourseStatisticsRepository = userCourseStatisticsRepository,
        userStatisticsRepository = userStatisticsRepository,
    )
    val userCourseEnrollmentsRepository: UserCourseEnrollmentsRepository = ExposedUserCourseEnrollmentsRepository()
    val attemptRepo: AttemptRepository = ExposedAttemptRepository()
    val stateRepo: TaskStateRepository = ExposedTaskStateRepository()
    val queueRepo: QueueRepository = ExposedQueueRepository()
    val progressRepo: ProgressRepository = ExposedProgressRepository()
    val badgeRepo: BadgeRepository = ExposedBadgeRepository()
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
        badgeService = badgeService
    )
    val statsService = StatsService(progressRepo = progressStatsRepository, contentRepo = contentStatsRepository)

    val userAttemptService = UserAttemptService(
        attemptRepo = attemptRepo
    )
    val retryRepository: RetrySwitchRepository =
        RetrySwitchRepositoryImplementation()
    val retryService = RetryService(retryRepository)
//
    val studentService: StudentService = StudentServiceImpl(
        courses = coursesRepository,
        enrollments = userCourseEnrollmentsRepository,
        users = userRepository
    )
    routing {
        authRoutes(authService)
        mailingRoutes(mailer, recoveryRepo, userRepository)
        adminRoutes(adminService)
        profileRoutes(profileService)
        contentRoutes(contentService)
        contentManagerRoutes(contentService)
        expertRoutes(expertService)
        studentRoutes(studentService)
        mediaRoutes(mediaService)
        taskRoutes(tasksRepository)
        courseRoutes(progressService)
        attemptRoutes(solveService)
        badgeRoutes(badgesQueryService)
        statsRoutes(statsService)
        themeRoutes(
            queueService = queueService,
            progressService = progressService
        )
        userAttemptRoutes(userAttemptService)
        configurationRoutes(retryService)
    }

}
