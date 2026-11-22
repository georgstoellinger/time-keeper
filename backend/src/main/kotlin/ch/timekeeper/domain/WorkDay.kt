package ch.timekeeper.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "work_day")
class WorkDay(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "date", nullable = false, unique = true)
    var date: LocalDate,

    @Column(columnDefinition = "text")
    var notes: String? = null,

    @OneToMany(mappedBy = "workDay", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("startTime ASC")
    var entries: MutableList<TimeEntry> = mutableListOf(),
)
