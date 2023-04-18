package com.imtilab.bittracer.constant

import groovy.util.logging.Slf4j
import com.imtilab.bittracer.model.ApiRequest
import com.imtilab.bittracer.utils.HTTPUtils
import com.imtilab.bittracer.utils.RequestExecutor
import kong.unirest.HttpResponse

@Slf4j
enum Method {
    GET,
    POST,
    PUT,
    DELETE

    static String proxyHost = null
    static Integer proxyPort = null

    static {
        if (HTTPUtils.isProxyEnable()) {
            proxyHost = HTTPUtils.getHost()
            proxyPort = HTTPUtils.getPort()
        }
    }

    HttpResponse execute(ApiRequest request) {
        RequestExecutor.executeRequest(request, proxyHost, proxyPort)
    }

    @Override
    String toString() {
        this.name().toLowerCase()
    }
}
