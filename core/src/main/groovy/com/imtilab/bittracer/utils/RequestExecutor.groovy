package com.imtilab.bittracer.utils

import groovy.util.logging.Slf4j
import com.imtilab.bittracer.constant.Constants
import com.imtilab.bittracer.model.ApiRequest
import com.imtilab.bittracer.model.ApiResponse
import kong.unirest.HttpResponse
import kong.unirest.Unirest
import kong.unirest.UnirestInstance
import net.sf.json.JSONArray
import net.sf.json.JSONObject

@Slf4j
class RequestExecutor {

    private RequestExecutor() {}

    static HttpResponse executeRequest(ApiRequest apiRequest, String proxyHost, Integer proxyPort, Boolean isSSLEnable = false) {
        HttpResponse httpResponse
        UnirestInstance unirestInstance = Unirest.spawnInstance()
        configProxy(unirestInstance, proxyHost, proxyPort)
        if (!isSSLEnable) {
            unirestInstance.config().verifySsl(false)
        }
        String method = apiRequest.getMethodName()
        String url = apiRequest.getUrl()
        Map requestHeaders = apiRequest.getRequestHeaders()
        Map queryParameters = apiRequest.getQueryParameters()
        String requestBody = apiRequest.getRequestBody()
        switch (method) {
            case 'GET':
            case 'get':
                httpResponse = get(unirestInstance, url, requestHeaders, queryParameters)
                break
            case 'POST':
            case 'post':
                httpResponse = post(unirestInstance, url, requestHeaders, queryParameters, requestBody)
                break
            case 'PUT':
            case 'put':
                httpResponse = put(unirestInstance, url, requestHeaders, queryParameters, requestBody)
                break
            case 'DELETE':
            case 'delete':
                httpResponse = delete(unirestInstance, url, requestHeaders, queryParameters, requestBody)
                break
            default:
                throw new Exception("Invalid request method: ${method}")
        }
        unirestInstance.shutDown()
        httpResponse
    }

    private static HttpResponse get(UnirestInstance unirestInstance, String url, Map requestHeaders, Map queryParameters) throws IOException {
        def response = unirestInstance.get(url)
                .headers(requestHeaders)
                .queryString(queryParameters)
                .asString()
        response
    }

    private static HttpResponse post(UnirestInstance unirestInstance, String url, Map requestHeaders, Map queryParameters, String requestBody) throws IOException {
        def response = unirestInstance.post(url)
                .headers(requestHeaders)
                .queryString(queryParameters)
                .body(requestBody)
                .asString()
        response
    }

    private static HttpResponse put(UnirestInstance unirestInstance, String url, Map requestHeaders, Map queryParameters, String requestBody) throws IOException {
        def response = unirestInstance.put(url)
                .headers(requestHeaders)
                .queryString(queryParameters)
                .body(requestBody)
                .asString()
        response
    }

    private static HttpResponse delete(UnirestInstance unirestInstance, String url, Map requestHeaders, Map queryParameters, String requestBody) throws IOException {
        def response = unirestInstance.delete(url)
                .headers(requestHeaders)
                .queryString(queryParameters)
                .body(requestBody)
                .asString()
        response
    }

    static ApiResponse prepareResponse(HttpResponse httpResponse, List xmlArrayNames = []) {

        def responseBody = httpResponse.getBody()
        responseBody = responseBody.replaceAll(Constants.UTF8_BOM,"")
        try {
            // XML content
            if (XmlUtils.isXmlContent(responseBody)) {
                responseBody = XmlUtils.convertXmlToJson(responseBody, xmlArrayNames)
                // JSON formatted content
            } else if (responseBody != null && responseBody.startsWith("[")) {
                responseBody = JSONArray.fromObject(responseBody)
            } else {
                responseBody = JSONObject.fromObject(responseBody)
            }
        } catch (Exception err) {
            log.error Constants.JSON_RESPONSE_ERROR
            log.error err.getMessage()
            //throw new Exception(Constants.JSON_RESPONSE_ERROR)
        }
        ApiResponse.builder()
                .status(httpResponse.getStatus())
                .responseBody(responseBody)
                .responseHeaders(httpResponse.getHeaders())
                .build()
    }

    private static configProxy(UnirestInstance unirestInstance,String host, Integer port) {
        unirestInstance.config().connectTimeout(5000).socketTimeout(14000)
        if (host)
            unirestInstance.config().proxy(host, port)
    }
}
