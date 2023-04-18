package com.imtilab.bittracer.utils

import com.imtilab.bittracer.model.ApiRequest

class CurlBuilder {
    static String curlTemplate = '''
curl --location --request ${method} '${url}${params}' ${headers}${requestBody}'''

    static def params = { Map queryParameters ->
        String params = ""
        if (!queryParameters.isEmpty()) {
            params = "?"
            queryParameters.each { params = params + it.key + "=" + it.value + "&" }
        }
        params
    }

    static def headers = { Map requestHeaders ->
        String headers = ""
        if (!requestHeaders.isEmpty()) {
            requestHeaders.each { headers = headers + "\\\n--header '${it.key}: ${it.value}'" }
            if(!requestHeaders.containsKey('Content-Type')){
                headers = headers + "\\\n--header 'Content-Type: application/json' \\"
            }
        }
        headers
    }

    static String build(ApiRequest apiRequest) {
        Map map = [:]
        map.put('method', apiRequest.methodName.toUpperCase())
        map.put('url', apiRequest.url)
        map.put('params', params(apiRequest.queryParameters))
        map.put('headers', headers(apiRequest.requestHeaders))
        map.put('requestBody', apiRequest.requestBody ? "\n--data '"+apiRequest.requestBody+"'" : "")
        ResourceReader.getTextAndReplaceWithValues(curlTemplate,map)
    }
}
