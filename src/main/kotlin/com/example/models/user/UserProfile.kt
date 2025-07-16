package com.example.models.user

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val userId: String,
    val surname: String,
    val name: String,
    val patronymic: String? = null,
    val gender: Gender,
    val dob: String, // LocalDate as String (YYYY-MM-DD)
    val dor: String, // LocalDate as String (YYYY-MM-DD)
    val citizenship: String? = null,
    val nationality: String? = null,
    val countryOfResidence: String? = null,
    val cityOfResidence: String? = null,
    val countryDuringEducation: String? = null,
    val periodSpent: PeriodSpent? = null,
    val kindOfActivity: String? = null,
    val education: String? = null,
    val purposeOfRegister: String? = null
)
