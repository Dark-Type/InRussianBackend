package com.inRussian.routes.v3.admin

import io.ktor.resources.*
import kotlinx.serialization.Serializable

@Serializable
@Resource("/admin")
class AdminResource {

    @Serializable
    @Resource("/admin/users")
    data class Users(
        val page: Int? = null,
        val size: Int? = null,
        val role: String? = null,
        val createdFrom: String? = null,
        val createdTo: String? = null,
        val sortBy: String? = null,
        val sortOrder: String? = null
    )

    @Serializable
    @Resource("/admin/users/count")
    data class UsersCount(
        val role: String? = null,
        val createdFrom: String? = null,
        val createdTo: String? = null
    )

    @Serializable
    @Resource("/admin/users/staff")
    class Staff

    @Serializable
    @Resource("/admin/users/{userId}")
    data class UserById(val userId: String)

    @Serializable
    @Resource("/admin/users/{userId}/status")
    data class UserStatus(val userId: String)

    @Serializable
    @Resource("/admin/statistics/students/overall")
    class StudentsOverall

    @Serializable
    @Resource("/admin/statistics/students/course/{courseId}")
    data class StudentsByCourse(val courseId: String)

    @Serializable
    @Resource("/admin/statistics/course/{courseId}")
    data class CourseStatistics(val courseId: String)

    @Serializable
    @Resource("/admin/statistics/overall")
    class OverallStatistics
}
