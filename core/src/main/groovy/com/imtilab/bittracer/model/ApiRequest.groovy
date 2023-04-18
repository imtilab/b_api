package com.imtilab.bittracer.model

import groovy.transform.builder.Builder

@Builder
class ApiRequest implements Serializable {
    String url
    Map<String, String> requestHeaders
    Map<String, String> queryParameters
    String requestBody
    String methodName

    @Override
    String toString() {
        return "ApiRequest{" +
                "url='" + url + '\'' +
                ",\n requestHeaders=" + requestHeaders +
                ",\n queryParameters=" + queryParameters +
                ",\n requestBody='" + requestBody + '\'' +
                '}';
    }
}