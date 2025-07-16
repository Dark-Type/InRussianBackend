package com.inRussian.responses.admin

import com.inRussian.requests.admin.AdminUserFilterRequest
import com.inRussian.models.admin.AdminUserListItem
import kotlinx.serialization.Serializable

@Serializable
data class AdminUsersResponse(
    val users: List<AdminUserListItem>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int,
    val filters: AdminUserFilterRequest
)