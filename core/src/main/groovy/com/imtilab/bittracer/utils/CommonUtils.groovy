package com.imtilab.bittracer.utils

import groovy.util.logging.Slf4j
import com.imtilab.bittracer.constant.Constants
import org.apache.commons.lang3.StringUtils

@Slf4j
class CommonUtils {
    /**
     * Convert data into primitive wrapper type
     *
     * convertToPrimitive(null)     = null
     * convertToPrimitive("true")   = true
     * convertToPrimitive(""true"") = "true"
     * convertToPrimitive("123")    = 123
     * convertToPrimitive(""123"")  = "123"
     *
     * @param data
     * @return primitive data {String/Long/Double/Boolean}
     */
    static def convertToPrimitive(String data) {
        if (StringUtils.isEmpty(data)) {
            data
        }

        if (data.isLong()) {
            Long.parseLong(data)
        } else if (data.isDouble()) {
            Double.parseDouble(data)
        } else if (data.equalsIgnoreCase("true") || data.equalsIgnoreCase("false")) {
            Boolean.parseBoolean(data)
        } else if (data.matches("^\"(.*?)\"\$")) {
            data = StringUtils.replaceOnce(data, "\"", "")
            StringUtils.removeEnd(data, "\"")
        } else if (data.equalsIgnoreCase("null")) {
            null
        } else {
            data
        }
    }

    /**
     * Convert data into given type
     * @param data
     * @return primitive data {String/Long/Double/Boolean}
     */
    static def convertToType(String data, String type) {
        if (StringUtils.isEmpty(data)) {
            data
        }

        if (data.isLong() && type=="Long") {
            Long.parseLong(data)
        } else if (data.isDouble() && type=="Double") {
            Double.parseDouble(data)
        } else if ((data.equalsIgnoreCase("true") || data.equalsIgnoreCase("false")) && type=="Boolean") {
            Boolean.parseBoolean(data)
        } else if (data.matches("^\"(.*?)\"\$")  && type=="String") {
            data = StringUtils.replaceOnce(data, "\"", "")
            StringUtils.removeEnd(data, "\"")
        } else if (data.equalsIgnoreCase("null")  && type=="null") {
            null
        } else {
            data
        }
    }

    /**
     * If data is an instance of multiple collection then return as a single collection
     * @param data
     * @return
     */
    static def makeSingleList(def data) {
        def list = new ArrayList()
        if (data instanceof Collection) {
            data.each {
                list.addAll(makeSingleList(it))
            }
            list
        } else {
            data
        }
    }

    /**
     * Generate UUID
     * @return
     */
    static String getRandomUUID() {
        return UUID.randomUUID().toString()
    }

    static def isNullString(def value) {
        String text = value.toString()
        text == null || text.trim().equalsIgnoreCase('null')
    }

    static def isEmptyString(def value) {
        String text = value.toString()
        text != null && text.equals("\"\"")
    }

    static boolean isExponentialNumber(def value){
        value && value.toString().matches(Constants.EXPONENTIAL_REGEX)
    }

    static String exponentialToDecimal(def value){
        new BigDecimal(value)
    }

    static Map getCellValueAsMap(String input, ApplicationResource applicationResource, boolean isTypeConversionEnable = false) {
        Map map = [:]
        if (StringUtils.isNotEmpty(input) && !isNullString(input)) {
            String[] keys = input.split(Constants.NEW_LINE_COMMA)
            String key, value
            keys.each {
                if (it?.trim()) {
                    try {
                        if (it.endsWith(Constants.COLON)) {
                            key = it - Constants.COLON
                            map.put(key, StringUtils.EMPTY)
                        } else {
                            (key, value) = it.split(Constants.COLON, 2)
                            def formattedValue = ResourceReader.setDataFromProperties(value, applicationResource)
                            if (isTypeConversionEnable) {
                                formattedValue = CSVUtils.getFormattedValue(formattedValue, applicationResource)
                            }
                            map.put(key, formattedValue)
                        }
                    } catch (Exception ex) {
                        throw new Exception("Key mapping syntax error near :" + it)
                    }
                }
            }
        }
        map
    }

    static def getRowValue(def row, String property) {
        try {
            if (row.propertyMissing(property)) {
                Eval.x(row, 'x.' + property)
            }
        } catch (MissingPropertyException e) {
            null
        }
    }
}
