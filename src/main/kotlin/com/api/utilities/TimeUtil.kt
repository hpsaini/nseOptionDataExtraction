package com.api.utilities

import java.time.LocalDateTime
import java.time.LocalTime

object TimeUtil {
    private lateinit var executionTime: String


    fun getExecutionTimeStamp(): String {
        if(!::executionTime.isInitialized){
            val localTime = LocalTime.now()
            executionTime = "${localTime.hour}${localTime.minute}"
        }
        return executionTime
    }
    fun getDate():String{
        val localDate = LocalDateTime.now()
        return "${localDate.dayOfMonth}${localDate.month}"
    }

}