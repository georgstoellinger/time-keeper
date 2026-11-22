package ch.timekeeper.service

import java.math.BigDecimal
import java.math.RoundingMode

object Durations {
    private val SIXTY = BigDecimal(60)

    fun toHours(minutes: Long): BigDecimal =
        BigDecimal(minutes).divide(SIXTY, 2, RoundingMode.HALF_UP)
}
