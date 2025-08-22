package com.inRussian.models

import java.util.UUID

data class EmailRequest(
    val id: String = UUID.randomUUID().toString(),
)
