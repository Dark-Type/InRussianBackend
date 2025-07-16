package com.example.models.admin

import com.example.models.user.Gender
import com.example.models.user.PeriodSpent
import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserProfileRequest(
    val surname: String? = null,
    val name: String? = null,
    val patronymic: String? = null,
    val gender: Gender? = null,
    val dob: String? = null, // YYYY-MM-DD
    val dor: String? = null, // YYYY-MM-DD
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