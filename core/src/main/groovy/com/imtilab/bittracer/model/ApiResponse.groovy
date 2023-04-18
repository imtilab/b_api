package com.imtilab.bittracer.model

import groovy.transform.builder.Builder
import net.sf.json.JSONObject

@Builder
class ApiResponse {
    int status
    def responseBody
    def responseHeaders = [:]

    @Override
    String toString() {
        return "ApiResponse{" +
                "status=" + status +
                ", responseBody=" + responseBody +
                ", responseHeaders=" + responseHeaders +
                '}';
    }
}