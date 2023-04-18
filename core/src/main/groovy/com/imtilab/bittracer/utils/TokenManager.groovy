package com.imtilab.bittracer.utils

import groovy.util.logging.Slf4j
import com.imtilab.bittracer.constant.Constants
import com.imtilab.bittracer.constant.PropertiesKey
import com.imtilab.bittracer.model.ApiRequest
import com.imtilab.bittracer.model.Token
import kong.unirest.HttpResponse
import net.sf.json.JSONObject
import org.apache.http.client.config.RequestConfig

import java.time.Duration
import java.time.LocalDateTime

@Slf4j
class TokenManager {
    private static String TOKEN_BASE_URL = ConfigurationResource.instance().getConfigProp(PropertiesKey.TOKEN_ISSUE_BASE_URL)
    private static String ISSUE_PATH = ConfigurationResource.instance().getConfigProp(PropertiesKey.TOKEN_ISSUE_PATH)
    private static String CANCEL_PATH = ConfigurationResource.instance().getConfigProp(PropertiesKey.TOKEN_CANCEL_PATH)

    // proxy configuration
    private RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(ConfigurationResource.instance().getConfigProp(PropertiesKey.CONNECT_TIMEOUT) as Integer)
            .setProxy(HTTPUtils.createProxy())
            .build()

    /**
     * Take tokenSet name, then issue token and return
     * @param tokenSet
     * @return Token
     */
    static Token issueToken(String tokenSet) {
        Map requestBodyMap = ConfigurationResource.instance.yamlDataMap(tokenSet)
        if (requestBodyMap == null) {
            return requestBodyMap
        }
        Token token
        int maxTryCount = 3
        while (maxTryCount > 0) {
            HttpResponse httpResponse = post(TOKEN_BASE_URL + ISSUE_PATH, prepareHeaders(), requestBodyMap)
            token = prepareToken(httpResponse)
            if (httpResponse.getStatus() == Constants.STATUS_CODE_200) {
                break
            }
            maxTryCount--
        }
        // these two will be required to cancel the issued token
        token.setClientId(requestBodyMap.get(Constants.CLIENT_ID))
        token.setClientSecret(requestBodyMap.get(Constants.CLIENT_SECRET))
        return token
    }

    /**
     * Take an issued token, cancel the token and return success status true/false
     * @param token
     * @return boolean
     */
    static boolean cancelToken(Token token) {
        String accessToken = token.accessToken
        if (accessToken == null) {
            return null
        }
        Map requestBodyMap = [:]
        requestBodyMap.put(Constants.CLIENT_ID, token.clientId)
        requestBodyMap.put(Constants.CLIENT_SECRET, token.clientSecret)
        requestBodyMap.put(Constants.ACCESS_TOKEN, accessToken)
        HttpResponse httpResponse = post(TOKEN_BASE_URL + CANCEL_PATH, prepareHeaders(), requestBodyMap)
        return httpResponse.getStatus() == Constants.STATUS_CODE_200
    }

    /**
     * Takes a token and check if it is expired or not
     * @param token
     * @return boolean
     */
    static boolean isExpiredToken(Token token) {
        Long remain = Duration.between(token.issuedAt, LocalDateTime.now()).getSeconds()
        Long departureAfter = ConfigurationResource.instance().getConfigProp(PropertiesKey.DEPARTURE_AFTER) as long
        return (token.expiresIn - remain <= departureAfter)
    }

    /**
     * Take url, headers and body; Then execute post request and return http response
     * @param apiUrl
     * @param headerGroup
     * @param query
     * @return CloseableHttpResponse
     */
    static HttpResponse post(String apiUrl, Map headerGroup, Map requestBodyMap) {
        ApiRequest tokenRequest = ApiRequest.builder()
                .methodName("post")
                .url(apiUrl)
                .requestHeaders(headerGroup)
                .queryParameters(requestBodyMap)
                .build()
        log.info("Token Request curl: {}", CurlBuilder.build(tokenRequest))
        String host = null
        Integer port = null
        if (HTTPUtils.isProxyEnable()) {
            host = HTTPUtils.getHost()
            port = HTTPUtils.getPort()
        }
        HttpResponse responseToken = RequestExecutor.executeRequest(tokenRequest, host, port)
        log.info("Token Response: {}", responseToken.getBody())
        responseToken
    }

    /**
     * Prepare token form response
     * @param httpResponse
     * @return Token
     */
    private static Token prepareToken(HttpResponse httpResponse) {
        String responseBody = httpResponse.getBody()
        def json = JSONObject.fromObject(responseBody)
        Token.builder()
                .statusCode(httpResponse.getStatus())
                .accessToken(json.get(Constants.ACCESS_TOKEN))
                .refreshToken(json.get(Constants.REFRESH_TOKEN))
                .tokenType(json.get(Constants.TOKEN_TYPE))
                .expiresIn(json.get(Constants.EXPIRES_IN))
                .scope(json.get(Constants.SCOPE))
                .errorDescription(json.get(Constants.ERROR_DESCRIPTION))
                .error(json.get(Constants.ERROR))
                .issuedAt(LocalDateTime.now())
                .build()
    }

    /**
     * Prepare headerGroup with headers and return headerGroup
     * @return HeaderGroup
     */
    private static Map prepareHeaders() {
//        HeaderGroup headerGroup = new HeaderGroup()
        def headers = [:]
        headers.put("Accept", "*/*")
        headers.put("Connection", "keep-alive")
        headers.put("content-type", "application/x-www-form-urlencoded")
//        headers.each { key, value ->
//            headerGroup.addHeader(new BasicHeader(key, value))
//        }
//        return headerGroup
        headers
    }
}
