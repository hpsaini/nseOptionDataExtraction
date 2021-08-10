package com.api.utilities

import io.restassured.RestAssured.given
import io.restassured.specification.RequestSpecification
import org.apache.log4j.Logger

class HttpRequestSpecification {
    private val logger = Logger.getLogger(HttpRequestSpecification::class.java)

    fun getRequestSpecification(requestType: RequestType): RequestSpecification {
        logger.info("HttpRequestSpecification - getRequestSpecification, creating new http request for $requestType request type")
        return when (requestType) {
            RequestType.NSE -> given()
                .filter(CookieSync.getCookies())
                .header("Host", "www.nseindia.com")
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472\n" +
                        ".114 Safari/537.36")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Connection", "keep-alive")
                .relaxedHTTPSValidation().redirects().follow(true)

            RequestType.DERIVATIVE_INDEX -> {
                given()
                    .filter(CookieSync.getCookies())
                    .header("Host", "www.nseindia.com")
                    .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472\n" +
                            ".114 Safari/537.36")
                    .header("Accept", "*/*")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Connection", "keep-alive")
                    .relaxedHTTPSValidation().redirects().follow(true)
            }
            RequestType.DERIVATIVE_EQUITY -> {
                given()
                    .filter(CookieSync.getCookies())
                    .header("Host", "www.nseindia.com")
                    .header("User-Agent", "PostmanRuntime/7.28.1")
                    .header("Accept", "*/*")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Connection", "keep-alive")
            }
        }
    }
}
