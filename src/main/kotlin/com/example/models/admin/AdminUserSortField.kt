package com.example.models.admin

import kotlinx.serialization.Serializable

@Serializable
enum class AdminUserSortField {
    CREATED_AT, UPDATED_AT, EMAIL, ROLE, LAST_ACTIVITY
}