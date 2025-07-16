package com.inRussian.requests.admin


import com.inRussian.models.admin.AdminUserSortField
import com.inRussian.models.admin.SortDirection
import com.inRussian.models.users.*
import kotlinx.serialization.Serializable

@Serializable
data class AdminUserFilterRequest(
    val page: Int = 1,
    val pageSize: Int = 20,
    val role: UserRole? = null,
    val systemLanguage: SystemLanguage? = null,
    val sortBy: AdminUserSortField = AdminUserSortField.CREATED_AT,
    val sortDirection: SortDirection = SortDirection.DESC,
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val searchQuery: String? = null
)

