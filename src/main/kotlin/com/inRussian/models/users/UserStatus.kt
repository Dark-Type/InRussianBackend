package com.inRussian.models.users

import kotlinx.serialization.Serializable

enum class UserStatus {
    ACTIVE,
    SUSPENDED,
    DEACTIVATED,
    PENDING_VERIFICATION
}