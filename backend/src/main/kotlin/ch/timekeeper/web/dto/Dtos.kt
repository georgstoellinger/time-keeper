package ch.timekeeper.web.dto

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

data class CategoryDto(
    val id: Long,
    val name: String,
    val color: String?,
)

data class CategoryRequest(
    val name: String,
    val color: String? = null,
)

data class EntryDto(
    val id: Long,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val durationMinutes: Long,
    val durationHours: BigDecimal,
    val categoryId: Long,
    val categoryName: String,
    val categoryColor: String?,
    val notes: String?,
)

data class EntryRequest(
    val startTime: LocalTime,
    val endTime: LocalTime,
    val categoryId: Long,
    val notes: String? = null,
)

data class DayDto(
    val id: Long?,
    val date: LocalDate,
    val notes: String?,
    val totalMinutes: Long,
    val totalHours: BigDecimal,
    val entries: List<EntryDto>,
)

data class DayRequest(
    val notes: String? = null,
)

data class WeekDto(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val totalMinutes: Long,
    val totalHours: BigDecimal,
    val days: List<DayDto>,
)
