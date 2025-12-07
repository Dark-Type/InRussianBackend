import io.ktor.resources.*
import kotlinx.serialization.Serializable

@Serializable
@Resource("/admin")
class AdminResource {

    @Serializable
    @Resource("/users")
    data class Users(
        val parent: AdminResource = AdminResource(),
        val page: Int? = null,
        val size: Int? = null,
        val role: String? = null,
        val createdFrom: String? = null,
        val createdTo: String? = null,
        val sortBy: String? = null,
        val sortOrder: String? = null
    )

    @Serializable
    @Resource("/users/count")
    data class UsersCount(
        val parent: AdminResource = AdminResource(),
        val role: String? = null,
        val createdFrom: String? = null,
        val createdTo: String? = null
    )

    @Serializable
    @Resource("/users/staff")
    data class Staff(
        val parent: AdminResource = AdminResource()
    )

    @Serializable
    @Resource("/users/{userId}")
    data class UserById(
        val parent: AdminResource = AdminResource(),
        val userId: String
    )

    @Serializable
    @Resource("/users/{userId}/status")
    data class UserStatus(
        val parent: AdminResource = AdminResource(),
        val userId: String
    )

    @Serializable
    @Resource("/statistics/students/overall")
    data class StudentsOverall(
        val parent: AdminResource = AdminResource()
    )

    @Serializable
    @Resource("/statistics/students/course/{courseId}")
    data class StudentsByCourse(
        val parent: AdminResource = AdminResource(),
        val courseId: String
    )

    @Serializable
    @Resource("/statistics/course/{courseId}")
    data class CourseStatistics(
        val parent: AdminResource = AdminResource(),
        val courseId: String
    )

    @Serializable
    @Resource("/statistics/overall")
    data class OverallStatistics(
        val parent: AdminResource = AdminResource()
    )
}