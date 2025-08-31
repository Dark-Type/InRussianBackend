package com.inRussian.models.auth

import kotlinx.serialization.Serializable

@Serializable data class RecoveryRequest(val email: String)
@Serializable data class RecoveryCheckRequest(val email: String, val code: String)
@Serializable data class RecoveryCheckResponse(val ok: Boolean, val reason: String? = null)
@Serializable data class PasswordResetRequest(val email: String, val code: String, val newPassword: String)