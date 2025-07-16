package com.inRussian.models.users

import kotlinx.serialization.Serializable


@Serializable
enum class PeriodSpent {
    MONTH_MINUS, // MONTH-
    SIX_MONTHS_MINUS, // 6MONTHS-
    YEAR_MINUS, // YEAR-
    YEAR_PLUS, // YEAR+
    FIVE_YEAR_PLUS, // 5YEAR+
    NEVER
}
