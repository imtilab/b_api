package com.imtilab.bittracer.utils

import groovy.util.logging.Slf4j
import com.imtilab.bittracer.constant.Constants
import com.imtilab.bittracer.constant.PropertiesKey
import com.imtilab.bittracer.model.ApiRequest
import com.imtilab.bittracer.model.ApiResponse
import com.imtilab.bittracer.utils.curlparser.CurlVisitor
import kong.unirest.HttpResponse
import kong.unirest.Unirest
import kong.unirest.UnirestInstance
import net.sf.json.JSONArray
import net.sf.json.JSONObject
import org.apache.http.HttpHost

/**
 *  Provide all the http methods
 */
@Slf4j
class HTTPUtils {

    private static HttpHost createProxy() {
        if (isProxyEnable()) {
            new HttpHost(getHost(), getPort(), "http")
        }
    }

    static boolean isProxyEnable() {
        Boolean.parseBoolean(ConfigurationResource.instance().getConfigProp(PropertiesKey.PROXY_ENABLE).toString())
    }

    static String getHost() {
        ConfigurationResource.instance().getConfigProp(PropertiesKey.PROXY_HOST).toString()
    }

    static Integer getPort() {
        Integer.parseInt(ConfigurationResource.instance().getConfigProp(PropertiesKey.PROXY_PORT).toString())
    }

    private static String getProxy(Boolean isProxyEnable) {
        if (isProxyEnable) {
            " --proxy 'http://" + getHost() + ":" + getPort() + "' -sS"
        } else {
            ""
        }
    }
    /**
     * Execute curl and send response
     * @param curl
     * @param isProxyEnable
     * @param isSSLEnable
     * @return
     */
    static def executeCurl(ApplicationResource applicationResource, String curl, Boolean isProxyEnable, Boolean isSSLEnable = true) {
        String host = null
        Integer port = null
        if ((isProxyEnable == null && HTTPUtils.isProxyEnable()) || isProxyEnable) {
            host = getHost()
            port = getPort()
        }
        if (isSSLEnable == null) {
            isSSLEnable = true
        }
        curl = ResourceReader.setDataFromProperties(curl, applicationResource)
        curl = curl.trim()
        log.info("Curl Request:\n{}", curl)
        ApiRequest curlRequest = CurlVisitor.parseCurl(curl)
        HttpResponse response = RequestExecutor.executeRequest(curlRequest, host, port, isSSLEnable)
        ApiResponse apiResponse = RequestExecutor.prepareResponse(response)
        log.info("Response Body: \n{}", apiResponse.responseBody != null ? apiResponse.responseBody.toString() : apiResponse.responseBody)
        apiResponse.responseBody
    }
}
