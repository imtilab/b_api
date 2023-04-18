package com.imtilab.bittracer.model

import groovy.transform.builder.Builder

@Builder
class EvidenceContext {
    String archivePath
    ApiRequest apiRequest
    ApiResponse apiResponse
    String expectedResponse
    String successLog
    String errorLog
    String timestamp

    @Override
    String toString() {
        return "EvidenceContext{" +
                "archivePath='" + archivePath + '\'' +
                ", apiRequest=" + apiRequest +
                ", apiResponse=" + apiResponse +
                ", expectedResponse='" + expectedResponse + '\'' +
                ", successLog='" + successLog + '\'' +
                ", errorLog='" + errorLog + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}'
    }
}
