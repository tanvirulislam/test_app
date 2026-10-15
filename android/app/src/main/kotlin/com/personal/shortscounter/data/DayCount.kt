package com.personal.shortscounter.data

import java.time.LocalDate

data class DayCount(val date: LocalDate, val count: Int) {
    fun toMap(): Map<String, Any> = mapOf("date" to date.toString(), "count" to count)
}
