package com.inRussian.models.auth

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class PasswordUpdateToken @OptIn(ExperimentalTime::class) constructor(val token: String, val userId: String, val expiresAt: Instant, var usedAt: Instant?)