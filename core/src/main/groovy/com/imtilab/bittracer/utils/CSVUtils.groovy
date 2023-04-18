package com.imtilab.bittracer.utils

import com.imtilab.bittracer.constant.CSVColumn
import com.imtilab.bittracer.constant.Constants
import net.sf.json.JSONArray
import net.sf.json.JSONObject
import org.apache.commons.lang3.StringUtils

class CSVUtils {
    static JSONDataUtils jsonDataUtils = new JSONDataUtils()
    /**
     * Read value from row by column name
     * @param row
     * @param property
     * @return
     */
    static def getRowValue(def row, String property) {
        try {
            if (row.propertyMissing(property)) {
                Eval.x(row, 'x.' + property)
            }
        } catch (MissingPropertyException e) {
            null
        }
    }

    /**
     * Read key-value pair values as map
     * @param row
     * @param property
     * @return
     */
    static def readKeyValuePairsAsMap(def row, String property, ApplicationResource applicationResource) {
        Map map = [:]
        String statements = getRowValue(row, property)
        if (statements?.trim()) {
            def keyValuePairList = statements.trim().split(Constants.STATEMENT_SEPARATOR)
            for (pair in keyValuePairList) {
                String key, value
                try {
                    (key, value) = pair.split(Constants.VALUE_SPLITTER)
                } catch (ArrayIndexOutOfBoundsException ex) {
                    throw new Exception("Syntex error near : ${pair}")
                }
                key = ResourceReader.setDataFromProperties(key, applicationResource)
                def formattedValue = getFormattedValue(value, applicationResource)
                map.put(key, formattedValue)
            }
        }
        map
    }

    static Map getPrefixContainedColumnValue(def headers, def row, String prefix, ApplicationResource applicationResource, boolean isTypeConversionEnable = false, boolean isPutEmptyValue = true) {
        Map map = [:]
        List columns = headers.findAll { it -> it.startsWith(prefix) }
        columns.each { column ->
            String key = column.replaceFirst(prefix, StringUtils.EMPTY)
            def value = row.toMap().get(column)
            // Ignore value
            if (value.equalsIgnoreCase(CSVColumn.IGNORE.getValue())) {
                return
            }
            // Replace UUID
            if (value && value.contains(Constants.UUID_AUTO_GENERATE)) {
                value = StringUtils.replace(value, Constants.UUID_AUTO_GENERATE, CommonUtils.getRandomUUID())
            }

            // Store value into map
            if (value && value instanceof String) {
                value = ResourceReader.setDataFromProperties(value, applicationResource)
                // Convert into primitive type
                if (isTypeConversionEnable) {
                    value = getFormattedValue(value, applicationResource)
                }
                map.put(key, value)
            } else if (isPutEmptyValue)
                map.put(key.trim(), value)
        }
        return map
    }

    /**
     * Convert value into expected type
     * @param value
     * @return
     */
    static def getFormattedValue(String value, ApplicationResource applicationResource) {
        value = value.trim()
        def formattedValue
        if (value.startsWith("\${") && value.endsWith("}")) {
            value = value.replace("\${", "")
            value = value.replace("}", "")
            formattedValue = applicationResource.getDataProp(value)
        } else {
            value = ResourceReader.setDataFromProperties(value, applicationResource)
            if (value.startsWith("{") && value.endsWith("}")) {
                formattedValue = JSONObject.fromObject(value)
            } else if (value.startsWith("[") && value.endsWith("]")) {
                formattedValue = JSONArray.fromObject(value)
            } else {
                formattedValue = CommonUtils.convertToPrimitive(value)
            }
        }
        formattedValue
    }

    static def replaceJSONValuesByKeys(def jsonObject, Map keyValuePairs) {
        keyValuePairs.each { pair ->
            jsonObject = jsonDataUtils.replaceDataByKey(jsonObject, pair.key.trim() as String, pair.value)
        }
        jsonObject
    }

    static def appendJSONValuesByKeys(def jsonObject, Map keyValuePairs) {
        keyValuePairs.each { pair ->
            jsonObject = jsonDataUtils.appendDataByKey(jsonObject, pair.key.trim() as String, pair.value)
        }
        jsonObject
    }

    /**
     * Take row and csvColumnName; then return list of xmlArrayNames.
     *
     * @param row
     * @param csvColumnName
     * @return List of String
     */
    static List resolveXmlArrayNames(def row, String csvColumnName, ApplicationResource applicationResource) {
        String xmlArrayNameInput = resolveFromValueOrPathForTxt(getRowValue(row, csvColumnName), applicationResource)
        List xmlArrayNames = []
        if (xmlArrayNameInput?.trim()) {
            xmlArrayNames = xmlArrayNameInput.split(Constants.NEW_LINE_COMMA).collect {
                it.trim()
            }
        }
        xmlArrayNames
    }

    /**
     * Take csv cell-value for txt (Ex. xml-array-name) and return data as String.
     *
     * @param input
     * @return String
     */
    static String resolveFromValueOrPathForTxt(String input,ApplicationResource applicationResource, Map data = [:]) {
        if (CommonUtils.isNullString(input)) {
            null
        } else if (CommonUtils.isEmptyString(input)) {
            StringUtils.EMPTY
        } else if (input.endsWith(Constants.DOT_TXT)) {
            ResourceReader.getTextFromUrlAndReplaceWithValues(ResourceReader.getUrl(input), applicationResource.getData() + data)
        } else {
            ResourceReader.getTextAndReplaceWithValues(input,  applicationResource.getData() + data)
        }
    }

    /**
     * Take csv cell-value for txt (Ex. xml-array-name) and return data as String.
     *
     * @param input
     * @return String
     */
    static String resolveFromValueOrPathForTxtInput(String input) {
        if (CommonUtils.isNullString(input)) {
            null
        } else if (CommonUtils.isEmptyString(input)) {
            StringUtils.EMPTY
        } else if (input.endsWith(Constants.DOT_TXT)) {
            ResourceReader.getUrl(input).getText()
        } else {
            input
        }
    }

}
