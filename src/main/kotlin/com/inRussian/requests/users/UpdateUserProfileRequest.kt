package com.inRussian.requests.users

import com.inRussian.models.users.Gender
import com.inRussian.models.users.PeriodSpent
import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserProfileRequest(
    val surname: String? = null,
    val name: String? = null,
    val patronymic: String? = null,
    val gender: Gender? = null,
    val dob: String? = null,
    val dor: String? = null,
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
