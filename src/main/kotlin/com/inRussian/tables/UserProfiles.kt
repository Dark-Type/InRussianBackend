package com.inRussian.tables

import com.inRussian.models.users.Gender
import com.inRussian.models.users.PeriodSpent
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

object UserProfiles : Table("user_profiles") {
    val userId = reference("user_id", Users)
    val surname = varchar("surname", 100)
    val name = varchar("name", 100)
    val patronymic = varchar("patronymic", 100).nullable()
    val gender = enumerationByName("gender", 10, Gender::class)
    val dob = date("dob")
    val dor = date("dor")
    val citizenship = varchar("citizenship", 100).nullable()
    val nationality = varchar("nationality", 100).nullable()
    val countryOfResidence = varchar("country_of_residence", 100).nullable()
    val cityOfResidence = varchar("city_of_residence", 100).nullable()
    val countryDuringEducation = varchar("country_during_education", 100).nullable()
    val periodSpent = enumerationByName("period_spent", 20, PeriodSpent::class).nullable()
    val kindOfActivity = varchar("kind_of_activity", 255).nullable()
    val education = varchar("education", 255).nullable()
    val purposeOfRegister = varchar("purpose_of_register", 255).nullable()
    override val primaryKey = PrimaryKey(userId)
}