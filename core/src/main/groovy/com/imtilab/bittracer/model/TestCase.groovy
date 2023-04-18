package com.imtilab.bittracer.model

import groovy.transform.builder.Builder
import net.sf.json.JSONObject

/**
 * To represent test cases
 */
@Builder
class TestCase {

    String caseNo

    String description

    ApiRequest apiRequest

    String apiVersion

    Integer responseStatus

    boolean collectBackEndResponse

    JSONObject expectedResponse

    def responseKeyExclude

    def advancedValidation

    def advancedValidationV2

    boolean verifyBackendCall

    Map backendRequestCurl

    Map backendRequestCurlValuesToReplace

    Map backendRequestCurlProxyEnable

    Map backendRequestCurlSSLEnable

    Map backendAdvancedValidation

    Map compareBackendKeyMapping

    boolean matchResponseFlag

    boolean verifyLog

    def expectedResponseHeaders = [:]

    String apiCallType

    boolean isIgnore

    List apiResponseXmlArrayNames = []

    Map preRequestData = [:]

    Map postRequestData = [:]

    Map preExecutedData = [:]

    Map classWiseCommonPreRequestData = [:]

    int curlExecutionDelayTimeInSec = 0

    @Override
    String toString() {
        return "TestCase{" +
                "caseNo='" + caseNo + '\'' +
                ", description='" + description + '\'' +
                ", apiRequest=" + apiRequest +
                ", apiVersion='" + apiVersion + '\'' +
                ", responseStatus=" + responseStatus +
                ", collectBackEndResponse=" + collectBackEndResponse +
                ", expectedResponse=" + expectedResponse +
                ", responseKeyExclude=" + responseKeyExclude +
                ", advancedValidation=" + advancedValidation +
                ", advancedValidationV2=" + advancedValidationV2 +
                ", verifyBackendCall=" + verifyBackendCall +
                ", backendRequestCurl=" + backendRequestCurl +
                ", backendRequestCurlValuesToReplace=" + backendRequestCurlValuesToReplace +
                ", backendRequestCurlProxyEnable=" + backendRequestCurlProxyEnable +
                ", backendRequestCurlSSLEnable=" + backendRequestCurlSSLEnable +
                ", backendAdvancedValidation=" + backendAdvancedValidation +
                ", compareBackendKeyMapping=" + compareBackendKeyMapping +
                ", matchResponseFlag=" + matchResponseFlag +
                ", verifyLog=" + verifyLog +
                ", expectedResponseHeaders=" + expectedResponseHeaders +
                ", apiCallType='" + apiCallType + '\'' +
                ", isIgnore=" + isIgnore +
                ", apiResponseXmlArrayNames=" + apiResponseXmlArrayNames +
                ", preRequestData=" + preRequestData +
                ", classWiseCommonPreRequestData=" + classWiseCommonPreRequestData +
                ", postRequestData=" + postRequestData +
                ", curlExecutionDelayTimeInSec=" + curlExecutionDelayTimeInSec +
                '}'
    }
}
