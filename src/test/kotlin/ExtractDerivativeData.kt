import com.api.constants.ProjectProperties
import com.api.utilities.*
import com.opencsv.CSVReader
import com.api.utilities.ExcelFileWriter
import io.restassured.response.Response
import org.apache.log4j.LogManager
import org.testng.Assert
import org.testng.annotations.DataProvider
import org.testng.annotations.Factory
import org.testng.annotations.Test
import java.io.File
import java.io.FileReader
import java.io.Reader
import java.math.BigDecimal


class ExtractDerivativeData constructor() {
    private val logger = LogManager.getLogger(ExtractDerivativeData::class.java)
    private val httpRequestSpecification: HttpRequestSpecification = HttpRequestSpecification()
    private val inputDatafilePath = "${Util().getFolderRootPath()}/src/test/resources/InputData.csv"
    private var jsonDataFilePath = ""
    private var util: Util = Util()
    private var callDataMap: HashMap<Int, DerivativeData> = HashMap()
    private var putDataMap: HashMap<Int, DerivativeData> = HashMap()
    private var marketCallDirectionDataMap: java.util.HashMap<Int, MarketDirectionData> = java.util.HashMap()
    private var marketPutDirectionDataMap: java.util.HashMap<Int, MarketDirectionData> = java.util.HashMap()
    private lateinit var excelFileWriter: ExcelFileWriter
    private lateinit var optionExpiryDate: String
    private lateinit var equitySymbol: String
    private lateinit var strikePriceStringArray: List<String>
    private var strikePricePointDifference: Int = 0
    private lateinit var strikePriceArray: IntArray
    private lateinit var marketDirectionStrikePriceArray: IntArray
    private lateinit var testDataMap: HashMap<String, String>
    private lateinit var testData: Array<Array<java.util.HashMap<String, String>>>
    private var strikePriceVariation = 0
    private var atTheMoneyPrice = 0

    @DataProvider(name = "data-provider")
    fun dpMethod(): Array<Array<java.util.HashMap<String, String>>> {
        if (File(inputDatafilePath).exists()) {
            val reader: Reader = FileReader(inputDatafilePath)
            val csvReader = CSVReader(reader)
            val data: List<Array<String>> = csvReader.readAll()
            var dataCount = 0
            for (value in data) {
                if (value.contains(this.javaClass.name)) {
                    dataCount += 1
                }
            }
            testData = Array(dataCount) { Array(1) { java.util.HashMap<String, String>() } }
            dataCount = 0
            for (value in data) {
                if (value.contains(this.javaClass.name)) {
                    val hashMap = HashMap<String, String>()
                    hashMap["symbol"] = value[1]
                    hashMap["strikePrice"] = value[2]
                    hashMap["expiryDate"] = value[3]
                    hashMap["pointDifference"] = value[4]
                    hashMap["strikePriceVariation"] = value[5]
                    testData[dataCount][0] = hashMap
                    dataCount += 1
                }
            }
        } else {
            Assert.fail()
        }
        return testData
    }

    @Factory(dataProvider = "data-provider")
    constructor(currentTestData: HashMap<String, String>) : this() {
        testDataMap = currentTestData
        logger.debug("In Constructor; Test Data : $currentTestData")
    }

    @Test
    fun fetchDerivativeData() {
        optionExpiryDate = testDataMap["expiryDate"].toString()
        equitySymbol = testDataMap["symbol"].toString()
        strikePriceStringArray = testDataMap["strikePrice"].toString().split(":")
        strikePricePointDifference = testDataMap["pointDifference"]!!.toInt()
        strikePriceVariation = testDataMap["strikePriceVariation"]!!.toInt()
        logger.debug("optionExpiryDate : $optionExpiryDate")
        logger.debug("equitySymbol : $equitySymbol")
        logger.debug("strikePriceStringArray : $strikePriceStringArray")
        logger.debug("strikePricePointDifference : $strikePricePointDifference")
        logger.debug("strikePriceVariation : $strikePriceVariation")
        strikePriceArray = getStrikePriceArray(DataType.TARGETED_STRIKE_PRICE, strikePriceStringArray[0].toInt())

        var count = 0
        do {
            count += 1
            logger.debug("extracting data; count : $count")
            callDataMap = HashMap()
            putDataMap = HashMap()
            marketCallDirectionDataMap = HashMap()
            marketPutDirectionDataMap = HashMap()
            getAPIData()
            val parseJsonData = ParseJsonData(jsonDataFilePath)
            parseJsonData.getDerivativeData(
                jsonDataFilePath,
                optionExpiryDate,
                strikePriceArray,
                callDataMap,
                putDataMap
            )
            atTheMoneyPrice = parseJsonData.getAtTheMoneyPrice()!!
            val quotient = atTheMoneyPrice / strikePricePointDifference
            val remainderValue = atTheMoneyPrice % strikePricePointDifference
            val atTheMoneyStrikePrice =
                if (remainderValue == 0) {
                    atTheMoneyPrice
                } else if (remainderValue != 0 && remainderValue < strikePricePointDifference / 2) {
                    quotient * strikePricePointDifference
                } else {
                    (quotient + 1) * strikePricePointDifference
                }
            marketDirectionStrikePriceArray = getStrikePriceArray(DataType.MARKET_DIRECTION, atTheMoneyStrikePrice)
            parseJsonData.getMarketDirectionDerivativeData(
                optionExpiryDate, atTheMoneyStrikePrice, marketCallDirectionDataMap, marketPutDirectionDataMap
            )
            logger.debug("putDataMap.size ${putDataMap.size}")
            logger.debug("callDataMap.size ${callDataMap.size}")
            logger.debug("marketCallDirectionDataMap.size ${marketCallDirectionDataMap.size}")
            logger.debug("marketPutDirectionDataMap.size ${marketPutDirectionDataMap.size}")

        } while (((putDataMap.size != callDataMap.size) || (marketCallDirectionDataMap.size != marketPutDirectionDataMap.size)) && count < 4)
        if ((putDataMap.size != callDataMap.size)) {
            if (callDataMap.size > putDataMap.size) {
                for ((key, value) in callDataMap) {
                    if (putDataMap[key] == null) {
                        logger.debug("No entry found for putDataMap with Key : $key")
                        putDataMap[key] = DerivativeData(0, 0, BigDecimal.valueOf(0), BigDecimal.valueOf(0))
                        logger.debug("Added value for putDataMap with Key : ${putDataMap[key]}")
                    }
                }
            } else {
                for ((key, value) in putDataMap) {
                    if (callDataMap[key] == null) {
                        logger.debug("No entry found for callDataMap with Key : $key")
                        callDataMap[key] = DerivativeData(0, 0, BigDecimal.valueOf(0), BigDecimal.valueOf(0))
                        logger.debug("Added value for callDataMap with Key : ${callDataMap[key]}")
                    }
                }
            }
        }
        if ((marketCallDirectionDataMap.size != marketPutDirectionDataMap.size)) {
            if (marketCallDirectionDataMap.size > marketPutDirectionDataMap.size) {
                for ((key, value) in marketCallDirectionDataMap) {
                    if (marketPutDirectionDataMap[key] == null) {
                        logger.debug("No entry found for marketPutDirectionDataMap with Key : $key")
                        marketPutDirectionDataMap[key] = MarketDirectionData(0)
                        logger.debug("Added value for marketPutDirectionDataMap with Key : ${marketPutDirectionDataMap[key]}")
                    }
                }
            } else {
                for ((key, value) in marketPutDirectionDataMap) {
                    if (marketCallDirectionDataMap[key] == null) {
                        logger.debug("No entry found for marketCallDirectionDataMap with Key : $key")
                        marketCallDirectionDataMap[key] = MarketDirectionData(0)
                        logger.debug("Added value for marketCallDirectionDataMap with Key : ${marketCallDirectionDataMap[key]}")
                    }
                }
            }
        }

    }

    @Test(dependsOnMethods = ["fetchDerivativeData"])
    private fun writeTargetedStrikePriceData() {
        excelFileWriter = ExcelFileWriter(optionExpiryDate, equitySymbol, strikePriceStringArray[0])
        try {
            for (strikePrice in strikePriceArray) {
                excelFileWriter.writeDataToFile(optionExpiryDate, strikePrice, callDataMap, putDataMap)
            }
            excelFileWriter.addDataToDefaultSheet(strikePriceStringArray, callDataMap, putDataMap)
        } catch (e: NullPointerException) {
            logger.debug("Exception while writing targeted strike data, Exception Message : ${e.stackTraceToString()}")
            Assert.fail()
        }
    }

    @Test(dependsOnMethods = ["writeTargetedStrikePriceData"])
    fun marketDirectionData() {
        try {
            excelFileWriter.addMarketDirectionRawData(
                marketDirectionStrikePriceArray,
                atTheMoneyPrice,
                marketCallDirectionDataMap,
                marketPutDirectionDataMap
            )
            excelFileWriter.writeMarketDirectionSummaryData(marketDirectionStrikePriceArray)
        } catch (e: NullPointerException) {
            logger.debug("Exception while writing Market Direction Summary Data, Exception Message : ${e.stackTraceToString()}")
            Assert.fail()
        }
    }

    private fun getAPIData() {
        var count = 1
        lateinit var derivativeDataResponse: Response
        do {
            logger.debug("${count++} iteration for fetching API data")
            val nseResponse =
                httpRequestSpecification.getRequestSpecification(RequestType.NSE).get(ServiceEndpoints.NSE_DERIVATIVE)
            logger.debug("NSE response code : ${nseResponse.statusCode}")
            derivativeDataResponse = httpRequestSpecification.getRequestSpecification(RequestType.DERIVATIVE_EQUITY)
                .get(ServiceEndpoints.EQUITY_DERIVATIVE + equitySymbol)
            logger.debug("$equitySymbol derivative response code : ${derivativeDataResponse.statusCode}")
        } while (derivativeDataResponse.statusCode != 200 && count < 300)
        //        logger.debug("${ProjectProperties.SYMBOL} derivative req response : ${derivativeIndexResponse.prettyPrint()}")
        jsonDataFilePath = "${util.getFolderRootPath()}${ProjectProperties.ROOT_JSON_FOLDER_PATH}/${
            optionExpiryDate.replace("-", "")
        }/${TimeUtil.getDate()}${TimeUtil.getExecutionTimeStamp()}_${equitySymbol}_${strikePriceStringArray[0]}.json"
        util.saveApiResponse(derivativeDataResponse, jsonDataFilePath)
    }


    private fun getStrikePriceArray(dateType: DataType, strikeValue: Int): IntArray {
        val strikePriceArray: IntArray
        return when (dateType) {
            DataType.MARKET_DIRECTION -> {
                strikePriceArray = IntArray((strikePriceVariation * 2) + 1)
                var firstStrikePrice = strikeValue - strikePricePointDifference * strikePriceVariation
                for (i in 0..(strikePriceVariation * 2)) {
                    strikePriceArray[i] = firstStrikePrice
                    firstStrikePrice += strikePricePointDifference
                }
                strikePriceArray
            }
            DataType.TARGETED_STRIKE_PRICE -> {
                strikePriceArray = IntArray((strikePriceVariation * 2) + 1)
                var firstStrikePrice = strikeValue - strikePricePointDifference * strikePriceVariation
                for (i in 0..(strikePriceVariation * 2)) {
                    strikePriceArray[i] = firstStrikePrice
                    firstStrikePrice += strikePricePointDifference
                }
                strikePriceArray
            }
            else -> {
                strikePriceArray = IntArray(strikePriceStringArray.size)
                for ((index, strikePrice) in strikePriceStringArray.withIndex()) {
                    strikePriceArray[index] = strikePrice.toInt()
                }
                strikePriceArray
            }
        }
    }
}