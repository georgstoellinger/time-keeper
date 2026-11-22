package ch.timekeeper.repository

import ch.timekeeper.domain.Category
import ch.timekeeper.domain.WorkDay
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface CategoryRepository : JpaRepository<Category, Long> {
    fun existsByNameIgnoreCase(name: String): Boolean
    fun findByNameIgnoreCase(name: String): Category?
}

interface WorkDayRepository : JpaRepository<WorkDay, Long> {
    fun findByDate(date: LocalDate): WorkDay?
    fun findAllByDateBetweenOrderByDateAsc(from: LocalDate, to: LocalDate): List<WorkDay>
}
