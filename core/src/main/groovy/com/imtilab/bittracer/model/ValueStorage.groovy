package com.imtilab.bittracer.model

class ValueStorage {
    private def requestBody
    private def responseBody
    private def expectedBody
    private def upstreamBody

    def getRequestBody() {
        return requestBody
    }

    void setRequestBody(requestBody) {
        this.requestBody = requestBody
    }

    def getResponseBody() {
        return responseBody
    }

    void setResponseBody(responseBody) {
        this.responseBody = responseBody
    }

    def getExpectedBody() {
        return expectedBody
    }

    void setExpectedBody(expectedBody) {
        this.expectedBody = expectedBody
    }

    def getUpstreamBody() {
        return upstreamBody
    }

    void setUpstreamBody(upstreamBody) {
        this.upstreamBody = upstreamBody
    }

    void reset(){
        requestBody = {}
        responseBody = {}
        expectedBody = {}
        upstreamBody = {}
    }
}
