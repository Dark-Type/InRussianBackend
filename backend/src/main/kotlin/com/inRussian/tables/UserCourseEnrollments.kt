package com.inRussian.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp


private class DecimalAsDouble(private val precision: Int, private val scale: Int) : ColumnType() {
    override fun sqlType(): String = "DECIMAL($precision, $scale)"

    override fun valueFromDB(value: Any): Double = when (value) {
        is Double -> value
        is Float -> value.toDouble()
        is Int -> value.toDouble()
        is Long -> value.toDouble()
        is BigDecimal -> value.toDouble()
        is String -> value.toDouble()
        else -> error("Unexpected value for DecimalAsDouble: $value (${value::class})")
    }

    override fun notNullValueToDB(value: Any): Any = when (value) {
        is Double -> BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP)
        is BigDecimal -> value.setScale(scale, RoundingMode.HALF_UP)
        is String -> BigDecimal(value).setScale(scale, RoundingMode.HALF_UP)
        else -> error("Unexpected value for DecimalAsDouble: $value (${value::class})")
    }

    override fun nonNullValueToString(value: Any): String =
        notNullValueToDB(value).toString()
}

object UserCourseEnrollments : Table("user_course_enrollments") {
    val userId = reference("user_id", Users)
    val courseId = reference("course_id", Courses)
    val enrolledAt = timestamp("enrolled_at").defaultExpression(CurrentTimestamp)
    val completedAt = timestamp("completed_at").nullable()
    val currentSectionId = reference("current_section_id", Sections).nullable()
    val currentThemeId = reference("current_theme_id", Themes).nullable()

    // Expose as Double in Kotlin, store as DECIMAL(5,2) in DB
    val progress: Column<Double> = registerColumn("progress", DecimalAsDouble(5, 2)).default(0.0)

    override val primaryKey = PrimaryKey(userId, courseId)
}