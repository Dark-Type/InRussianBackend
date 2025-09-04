package com.inRussian.services

import com.inRussian.models.content.*
import com.inRussian.repositories.*

interface StudentService {
    suspend fun getCoursesByUserLanguage(userId: String): Result<List<Course>>
    suspend fun enrollInCourse(userId: String, courseId: String): Result<Boolean>
    suspend fun getUserEnrollments(userId: String): Result<List<UserCourseEnrollmentItem>>
    suspend fun unenrollFromCourse(userId: String, courseId: String): Result<Boolean>
    suspend fun getCourseEnrollment(userId: String, courseId: String): Result<UserCourseEnrollmentItem?>


}

class StudentServiceImpl(
    private val repository: StudentRepository
) : StudentService {

    override suspend fun getCoursesByUserLanguage(userId: String) =
        runCatching { repository.getCoursesByUserLanguage(userId) }

    override suspend fun enrollInCourse(userId: String, courseId: String) =
        runCatching { repository.enrollInCourse(userId, courseId) }

    override suspend fun getUserEnrollments(userId: String) =
        runCatching { repository.getUserEnrollments(userId) }

    override suspend fun unenrollFromCourse(userId: String, courseId: String) =
        runCatching { repository.unenrollFromCourse(userId, courseId) }

    override suspend fun getCourseEnrollment(userId: String, courseId: String) =
        runCatching { repository.getCourseEnrollment(userId, courseId) }



}