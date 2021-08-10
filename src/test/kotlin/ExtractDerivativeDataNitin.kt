import com.api.constants.ProjectProperties
import com.api.utilities.*
import com.opencsv.CSVReader
import fileWriterClass.ExcelFileWriter
import io.restassured.response.Response
import org.apache.log4j.LogManager
import java.util.*
import org.testng.Assert
import org.testng.annotations.DataProvider
import org.testng.annotations.Factory
import org.testng.annotations.Test
import java.io.File
import java.io.FileReader
import java.io.Reader


class ExtractDerivativeDataNitin constructor() {
    private val logger = LogManager.getLogger(ExtractDerivativeDataNitin::class.java)
    private val httpRequestSpecification: HttpRequestSpecification = HttpRequestSpecification()
    private val fileName = "${Util().getFolderRootPath()}/src/test/resources/InputData.csv"
    private var util: Util = Util()
    private val marketCallDirectionDataMap: HashMap<Int, MarketDirectionData> = HashMap()
    private val marketPutDirectionDataMap: HashMap<Int, MarketDirectionData> = HashMap()
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
        if (File(fileName).exists()) {
            val reader: Reader = FileReader(fileName)
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
    constructor(testData: HashMap<String, String>) : this() {
        testDataMap = testData
        optionExpiryDate = testData["expiryDate"].toString()
        equitySymbol = testData["symbol"].toString()
        strikePriceStringArray = testData["strikePrice"].toString().split(":")
        strikePricePointDifference = testData["pointDifference"]!!.toInt()
        strikePriceVariation = testData["strikePriceVariation"]!!.toInt()
    }

    @Test()
    fun derivativeData() {
        var count = 0
        lateinit var derivativeDataResponse: Response
        do {
            logger.debug("Iteration ${count++}")
            val nseResponse =
                httpRequestSpecification.getRequestSpecification(RequestType.NSE).get(ServiceEndpoints.NSE_DERIVATIVE)
            logger.debug("NSE response code : ${nseResponse.statusCode}")
            derivativeDataResponse = httpRequestSpecification.getRequestSpecification(RequestType.DERIVATIVE_EQUITY)
                .get(ServiceEndpoints.EQUITY_DERIVATIVE + equitySymbol)
            logger.debug("$equitySymbol derivative response code : ${derivativeDataResponse.statusCode}")
        } while (derivativeDataResponse.statusCode != 200 && count < 300)
//        logger.debug("${ProjectProperties.SYMBOL} derivative req response : ${derivativeIndexResponse.prettyPrint()}")
        var filePath = "${util.getFolderRootPath()}${ProjectProperties.ROOT_JSON_FOLDER_PATH}/${
            optionExpiryDate.replace("-", "")
        }/${TimeUtil.getExecutionTimeStamp()}_${equitySymbol}_${strikePriceStringArray[0]}.json"
//        filePath = "/Users/saini/Documents/workspace/projects/NSE_Nitin/log/05Aug2021/104_NIFTY_15750.json"

        util.saveApiResponse(derivativeDataResponse, filePath)
        var atTheMoneyPrice = ParseJsonData(filePath).getAtTheMoneyPrice()!!

        val quotient = atTheMoneyPrice / strikePricePointDifference
        val remainderValue = atTheMoneyPrice % strikePricePointDifference
        val atTheMoneyStrikePrice = if (remainderValue != 0 && remainderValue < strikePricePointDifference / 2) {
            quotient * strikePricePointDifference
        } else {
            (quotient + 1) * strikePricePointDifference

        }
        strikePriceArray = getStrikePriceArray(DataType.MARKET_DIRECTION, atTheMoneyStrikePrice)
        ParseJsonData(filePath).getMarketDirectionDerivativeData( optionExpiryDate, atTheMoneyStrikePrice, marketCallDirectionDataMap, marketPutDirectionDataMap
        )
//        excelFileWriter = ExcelFileWriter(optionExpiryDate, equitySymbol,atTheMoneyPrice.toString())
        try {
            excelFileWriter.addMarketDirectionRawData(
                atTheMoneyPrice,
                marketCallDirectionDataMap,
                marketPutDirectionDataMap
            )
            excelFileWriter.writeMarketDirectionSummaryData(strikePriceArray)
        }catch (e:KotlinNullPointerException){
            derivativeData()
        }
    }

    private fun getStrikePriceArray(dateType: DataType, strikeValue: Int): IntArray {
        val strikePriceArray:IntArray

        return when(dateType){
            DataType.MARKET_DIRECTION ->{
                strikePriceArray = IntArray((strikePriceVariation * 2) + 1)
                var firstStrikePrice = strikeValue - strikePricePointDifference * strikePriceVariation
                for (i in 0..(strikePriceVariation * 2)) {
                    strikePriceArray[i] = firstStrikePrice
                    firstStrikePrice += strikePricePointDifference
                }
                strikePriceArray
            }
            DataType.TARGETED_STRIKE_PRICE ->{
                strikePriceArray = IntArray((strikePriceVariation * 2) + 1)
                var firstStrikePrice = strikeValue - strikePricePointDifference * strikePriceVariation
                for (i in 0..(strikePriceVariation * 2)) {
                    strikePriceArray[i] = firstStrikePrice
                    firstStrikePrice += strikePricePointDifference
                }
                strikePriceArray
            }
            else -> {
                strikePriceArray = IntArray((strikePriceVariation * 2) + 1)
                var firstStrikePrice = strikeValue - strikePricePointDifference * strikePriceVariation
                for (i in 0..(strikePriceVariation * 2)) {
                    strikePriceArray[i] = firstStrikePrice
                    firstStrikePrice += strikePricePointDifference
                }
                strikePriceArray
            }
        }

    }
}









