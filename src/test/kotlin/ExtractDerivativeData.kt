import com.api.constants.ProjectProperties
import com.api.utilities.*
import com.opencsv.CSVReader
import fileWriterClass.ExcelFileWriter
import io.restassured.response.Response
import org.apache.log4j.LogManager
import org.testng.Assert
import org.testng.annotations.DataProvider
import org.testng.annotations.Factory
import org.testng.annotations.Test
import java.io.File
import java.io.FileReader
import java.io.Reader


class ExtractDerivativeData constructor() {
    private val logger = LogManager.getLogger(ExtractDerivativeData::class.java)
    private val httpRequestSpecification: HttpRequestSpecification = HttpRequestSpecification()
    private val inputDatafilePath = "${Util().getFolderRootPath()}/src/test/resources/InputData.csv"
    private var jsonDataFilePath = ""
    private var util: Util = Util()
    private val callDataMap: HashMap<Int, DerivativeData> = HashMap()
    private val putDataMap: HashMap<Int, DerivativeData> = HashMap()
    private val marketCallDirectionDataMap: java.util.HashMap<Int, MarketDirectionData> = java.util.HashMap()
    private val marketPutDirectionDataMap: java.util.HashMap<Int, MarketDirectionData> = java.util.HashMap()
    private lateinit var excelFileWriter: ExcelFileWriter
    private lateinit var optionExpiryDate: String
    private lateinit var equitySymbol: String
    private lateinit var strikePriceStringArray: List<String>
    private var strikePricePointDifference: Int = 0
    lateinit var strikePriceArray: IntArray
    private lateinit var testDataMap: HashMap<String, String>
    lateinit var testData: Array<Array<java.util.HashMap<String, String>>>
    private var strikePriceVariation = 0

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
            for ((index, value) in data.withIndex()) {
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

    @Test()
    fun derivativeData() {
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
        strikePriceArray = getStrikePriceArray(DataType.MARKET_DIRECTION, strikePriceStringArray[0].toInt())
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
        }/${TimeUtil.getExecutionTimeStamp()}_${equitySymbol}_${strikePriceStringArray[0]}.json"
        util.saveApiResponse(derivativeDataResponse, jsonDataFilePath)

        ParseJsonData(jsonDataFilePath).getDerivativeData(
            jsonDataFilePath,
            optionExpiryDate,
            strikePriceArray,
            callDataMap,
            putDataMap
        )
        // write to excel
        excelFileWriter = ExcelFileWriter(optionExpiryDate, equitySymbol, strikePriceStringArray[0])
        try {
            for (strikePrice in strikePriceArray) {
                excelFileWriter.writeDataToFile(optionExpiryDate, strikePrice, callDataMap, putDataMap)
            }
            excelFileWriter.addDataToDefaultSheet(strikePriceStringArray, callDataMap, putDataMap)

        } catch (e: Exception) {
            logger.debug("Exception while writing data, Exception Message : ${e.stackTrace}")

        }
    }

    @Test(dependsOnMethods = ["derivativeData"])
    fun marketDirectionData() {

        var atTheMoneyPrice = ParseJsonData(jsonDataFilePath).getAtTheMoneyPrice()!!

        val quotient = atTheMoneyPrice / strikePricePointDifference
        val remainderValue = atTheMoneyPrice % strikePricePointDifference
        val atTheMoneyStrikePrice = if (remainderValue != 0 && remainderValue < strikePricePointDifference / 2) {
            quotient * strikePricePointDifference
        } else {
            (quotient + 1) * strikePricePointDifference

        }
        strikePriceArray = getStrikePriceArray(DataType.MARKET_DIRECTION, atTheMoneyStrikePrice)
        ParseJsonData(jsonDataFilePath).getMarketDirectionDerivativeData(
            optionExpiryDate, atTheMoneyStrikePrice, marketCallDirectionDataMap, marketPutDirectionDataMap
        )
        try {
            excelFileWriter.addMarketDirectionRawData(
                atTheMoneyPrice,
                marketCallDirectionDataMap,
                marketPutDirectionDataMap
            )
            excelFileWriter.writeMarketDirectionSummaryData(strikePriceArray)
        } catch (e: Exception) {
            logger.debug("Exception while writing data, Exception Message : ${e.stackTrace}")
        }
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









