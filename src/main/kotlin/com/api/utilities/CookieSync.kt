package com.api.utilities

import io.restassured.filter.cookie.CookieFilter
import org.apache.log4j.LogManager

object CookieSync {
    private lateinit var cookieFilter: CookieFilter
    val logger = LogManager.getLogger(CookieSync::class.java)
    @JvmStatic fun getCookies() : CookieFilter{
        if(!this::cookieFilter.isInitialized){
            logger.debug("Inside CookieSync > getCookies() ")
            cookieFilter = CookieFilter()
        }
        logger.debug("Returning cookies")
        return cookieFilter
    }
}