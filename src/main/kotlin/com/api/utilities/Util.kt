package com.api.utilities

import io.restassured.response.Response
import org.apache.log4j.LogManager
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.time.LocalTime

class Util {
    private val logger = LogManager.getLogger(Util::class.java)
    private  var executionTime: String? = null
    fun saveApiResponse(serviceResponse: Response, apiJsonDataOutputPath: String): Boolean {
        val directory = File(apiJsonDataOutputPath).parent
        return if (makeDirectory(directory)!!) writeToFile(serviceResponse.prettyPrint(), apiJsonDataOutputPath)
        else false
    }



    private fun makeDirectory(dirPath: String?): Boolean? {
        return if (dirPath != null) {
            try {
                val dir = File(dirPath)
                dir.mkdirs()
                logger.info("Creating directory for storing JSON File:$dirPath")
                true
            } catch (e: Exception) {
                logger.info(" Exception while creating [" + dirPath + "] directory path  : " + e.message)
                false
            }
        } else {
            logger.info("Path to create directory is sent as Null")
            null
        }
    }

    private fun writeToFile(content: String?, filePath: String): Boolean {
        return try {
            val apiJsonOutput = FileWriter(filePath)
            apiJsonOutput.write(content)
            apiJsonOutput.flush()
            apiJsonOutput.close()
            logger.info("New File saved to path : $filePath")
            true
        } catch (e: IOException) {
            e.printStackTrace()
            logger.info("Exception found while writing File at $filePath: $e")
            false
        }
    }

    fun getFolderRootPath(): String? {
        return System.getProperty("user.dir")
    }

}