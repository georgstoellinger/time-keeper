package ch.timekeeper.web

import ch.timekeeper.service.TimeService
import ch.timekeeper.web.dto.DayDto
import ch.timekeeper.web.dto.DayRequest
import ch.timekeeper.web.dto.EntryRequest
import ch.timekeeper.web.dto.WeekDto
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api")
class DayController(
    private val timeService: TimeService,
) {

    @GetMapping("/days/{date}")
    fun getDay(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
    ): DayDto = timeService.getDay(date)

    @PutMapping("/days/{date}")
    fun updateDay(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @RequestBody request: DayRequest,
    ): DayDto = timeService.updateDay(date, request)

    @PostMapping("/days/{date}/entries")
    @ResponseStatus(HttpStatus.CREATED)
    fun addEntry(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @RequestBody request: EntryRequest,
    ): DayDto = timeService.addEntry(date, request)

    @PutMapping("/days/{date}/entries/{entryId}")
    fun updateEntry(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @PathVariable entryId: Long,
        @RequestBody request: EntryRequest,
    ): DayDto = timeService.updateEntry(date, entryId, request)

    @DeleteMapping("/days/{date}/entries/{entryId}")
    fun deleteEntry(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @PathVariable entryId: Long,
    ): DayDto = timeService.deleteEntry(date, entryId)

    @GetMapping("/week")
    fun getWeek(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
    ): WeekDto = timeService.getWeek(date)
}
