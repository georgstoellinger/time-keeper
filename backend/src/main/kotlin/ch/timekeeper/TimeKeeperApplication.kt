package ch.timekeeper

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TimeKeeperApplication

fun main(args: Array<String>) {
    runApplication<TimeKeeperApplication>(*args)
}
