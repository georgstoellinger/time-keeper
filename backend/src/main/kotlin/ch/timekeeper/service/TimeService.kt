package ch.timekeeper.service

import ch.timekeeper.domain.TimeEntry
import ch.timekeeper.domain.WorkDay
import ch.timekeeper.repository.WorkDayRepository
import ch.timekeeper.web.NotFoundException
import ch.timekeeper.web.dto.DayDto
import ch.timekeeper.web.dto.DayRequest
import ch.timekeeper.web.dto.EntryDto
import ch.timekeeper.web.dto.EntryRequest
import ch.timekeeper.web.dto.WeekDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Service
class TimeService(
    private val workDayRepository: WorkDayRepository,
    private val categoryService: CategoryService,
) {

    @Transactional(readOnly = true)
    fun getDay(date: LocalDate): DayDto =
        workDayRepository.findByDate(date)?.toDto() ?: emptyDay(date)

    @Transactional
    fun updateDay(date: LocalDate, request: DayRequest): DayDto {
        val day = findOrCreate(date)
        day.notes = request.notes?.takeIf { it.isNotBlank() }
        return day.toDto()
    }

    @Transactional
    fun addEntry(date: LocalDate, request: EntryRequest): DayDto {
        validateTimes(request)
        val day = findOrCreate(date)
        val category = categoryService.getEntity(request.categoryId)
        val entry = TimeEntry(
            workDay = day,
            startTime = request.startTime,
            endTime = request.endTime,
            category = category,
            notes = request.notes?.takeIf { it.isNotBlank() },
        )
        day.entries.add(entry)
        workDayRepository.save(day)
        return day.toDto()
    }

    @Transactional
    fun updateEntry(date: LocalDate, entryId: Long, request: EntryRequest): DayDto {
        validateTimes(request)
        val day = findDayEntity(date)
        val entry = day.entries.firstOrNull { it.id == entryId }
            ?: throw NotFoundException("Entry $entryId not found on $date")
        entry.startTime = request.startTime
        entry.endTime = request.endTime
        entry.category = categoryService.getEntity(request.categoryId)
        entry.notes = request.notes?.takeIf { it.isNotBlank() }
        return day.toDto()
    }

    @Transactional
    fun deleteEntry(date: LocalDate, entryId: Long): DayDto {
        val day = findDayEntity(date)
        val removed = day.entries.removeIf { it.id == entryId }
        if (!removed) throw NotFoundException("Entry $entryId not found on $date")
        workDayRepository.save(day)
        return day.toDto()
    }

    @Transactional(readOnly = true)
    fun getWeek(date: LocalDate): WeekDto {
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = monday.plusDays(6)
        val existing = workDayRepository
            .findAllByDateBetweenOrderByDateAsc(monday, sunday)
            .associateBy { it.date }

        val days = (0L..6L).map { offset ->
            val day = monday.plusDays(offset)
            existing[day]?.toDto() ?: emptyDay(day)
        }
        val totalMinutes = days.sumOf { it.totalMinutes }
        return WeekDto(
            weekStart = monday,
            weekEnd = sunday,
            totalMinutes = totalMinutes,
            totalHours = Durations.toHours(totalMinutes),
            days = days,
        )
    }

    private fun findOrCreate(date: LocalDate): WorkDay =
        workDayRepository.findByDate(date) ?: workDayRepository.save(WorkDay(date = date))

    private fun findDayEntity(date: LocalDate): WorkDay =
        workDayRepository.findByDate(date) ?: throw NotFoundException("No times tracked on $date")

    private fun validateTimes(request: EntryRequest) {
        require(request.endTime.isAfter(request.startTime)) {
            "End time (${request.endTime}) must be after start time (${request.startTime})"
        }
    }

    private fun emptyDay(date: LocalDate) = DayDto(
        id = null,
        date = date,
        notes = null,
        totalMinutes = 0,
        totalHours = Durations.toHours(0),
        entries = emptyList(),
    )
}

private fun WorkDay.toDto(): DayDto {
    val entryDtos = entries.sortedBy { it.startTime }.map { it.toDto() }
    val totalMinutes = entryDtos.sumOf { it.durationMinutes }
    return DayDto(
        id = id,
        date = date,
        notes = notes,
        totalMinutes = totalMinutes,
        totalHours = Durations.toHours(totalMinutes),
        entries = entryDtos,
    )
}

private fun TimeEntry.toDto(): EntryDto {
    val minutes = Duration.between(startTime, endTime).toMinutes()
    return EntryDto(
        id = id!!,
        startTime = startTime,
        endTime = endTime,
        durationMinutes = minutes,
        durationHours = Durations.toHours(minutes),
        categoryId = category.id!!,
        categoryName = category.name,
        categoryColor = category.color,
        notes = notes,
    )
}
