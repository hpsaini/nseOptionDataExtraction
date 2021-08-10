package com.api.constants

import org.apache.log4j.Logger
import java.util.*

object ProjectProperties {
    private val logger: Logger = org.apache.log4j.Logger.getLogger(ProjectProperties::class.java)
    @JvmStatic
    val INDEX_STRIKE_PRICE: String = ResourceBundle.getBundle("project").getString("index.strike.price")
    @JvmStatic
    val INDEX_EXPIRY_DATE = ResourceBundle.getBundle("project").getString("index.expiry.date").toString()
    @JvmStatic
    val INDEX_SYMBOL: String = ResourceBundle.getBundle("project").getString("index.derivative.symbol")
    @JvmStatic
    val INDEX_STRIKE_POINT_DIFFERENCE: Int = ResourceBundle.getBundle("project").getString("index.strike.price.point.difference").toInt()
    @JvmStatic
    val EQUITY_SYMBOL: String = ResourceBundle.getBundle("project").getString("equity.derivative.symbol")
    @JvmStatic
    val EQUITY_EXPIRY_DATE = ResourceBundle.getBundle("project").getString("equity.expiry.date").toString()
    @JvmStatic
    val EQUITY_STRIKE_PRICE: String = ResourceBundle.getBundle("project").getString("equity.strike.price")
    @JvmStatic
    val ROOT_JSON_FOLDER_PATH: String = ResourceBundle.getBundle("project").getString("root.json.folder")
    @JvmStatic
    val EQUITY_STRIKE_POINT_DIFFERENCE: Int = ResourceBundle.getBundle("project").getString("equity.strike.price.point.difference").toInt()
    init {
        logger.debug("Inside ProjectProperties - constants")
    }
}