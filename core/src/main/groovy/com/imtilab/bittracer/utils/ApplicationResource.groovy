package com.imtilab.bittracer.utils

import groovy.util.logging.Slf4j
import com.imtilab.bittracer.constant.Constants
import com.imtilab.bittracer.constant.PropertiesKey

/**
 * Read application resource files
 */
@Slf4j
class ApplicationResource {

    Map data
    Map preRequestData = [:]
    Map commonPreRequestData = [:]
    Map preExecutionStorage = [:]

    def init() {
        if (!data) {
            data = loadData(ResourceReader.loadAsInputStream(Constants.DATA_FILE), true)
        }
    }

    def loadData(InputStream inputStream, boolean enableTypeConversion) {
        def map = [:]
        (map).with {
            putAll(ResourceReader.loadInputStreamToMap(inputStream, enableTypeConversion))
        }
        replaceSystemData(map)
    }

    def replaceSystemData(def map) {
        String webBffApiKey = System.getProperty(PropertiesKey.WEB_BFF_APIKEY)
        String rBicClientApiKey = System.getProperty(PropertiesKey.R_BIC_CLIENT_APIKEY)
        String ichibaTopClientApiKey = System.getProperty(PropertiesKey.ICHIBA_TOP_CLIENT_APIKEY)
        if (webBffApiKey) {
            map.put(PropertiesKey.WEB_BFF_APIKEY, webBffApiKey)
        }
        if (rBicClientApiKey) {
            map.put(PropertiesKey.R_BIC_CLIENT_APIKEY, rBicClientApiKey)
        }
        if (ichibaTopClientApiKey) {
            map.put(PropertiesKey.ICHIBA_TOP_CLIENT_APIKEY, ichibaTopClientApiKey)
        }
        map
    }

    def valueOfdata(String key) {
        def allData = data + preRequestData + commonPreRequestData + preExecutionStorage
        allData.get(key)
    }

    def dataMap() {
        data + preRequestData + commonPreRequestData  + preExecutionStorage
    }

    def setPreReqData(Map data) {
        preRequestData = data
    }

    def setCommonPreReqData(Map data) {
        commonPreRequestData = commonPreRequestData + data
    }

    def setPreExecutedStorageData(Map data) {
        preExecutionStorage = data
    }

    void setPreRequestData(Map data) {
        setPreReqData(data)
    }

    void setCommonPreRequestData(Map data) {
        setCommonPreReqData(data)
    }

    /* Data accessible methods */

     def getDataProp(String propertyName) {
        valueOfdata(propertyName)
    }

    Map getData() {
        dataMap()
    }
}
