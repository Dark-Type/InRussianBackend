package com.inRussian.models.users

import kotlinx.serialization.Serializable


@Serializable
enum class PeriodSpent {
    MONTH_MINUS, MONTH_SIX_MONTHS_MINUS,  SIX_MONTHS, YEAR_MINUS, YEAR_PLUS,  FIVE_YEARS_PLUS, NEVER
}
