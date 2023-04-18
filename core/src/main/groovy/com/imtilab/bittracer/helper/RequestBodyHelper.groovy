package com.imtilab.bittracer.helper

import groovy.json.JsonBuilder
import com.imtilab.bittracer.test.ECSGSpecification
import com.imtilab.bittracer.constant.CSVColumn
import com.imtilab.bittracer.constant.Constants
import com.imtilab.bittracer.constant.PropertiesKey
import com.imtilab.bittracer.test.GraphQLSpecification
import com.imtilab.bittracer.utils.ApplicationResource
import com.imtilab.bittracer.utils.CSVUtils
import com.imtilab.bittracer.utils.CommonUtils
import com.imtilab.bittracer.utils.DateUtil
import com.imtilab.bittracer.utils.JSONDataUtils
import com.imtilab.bittracer.utils.ResourceReader
import com.imtilab.bittracer.utils.XmlUtils
import net.sf.json.JSONArray
import net.sf.json.JSONObject
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang3.StringUtils

/**
 * Request body can be prepare in types of combination of columns:
 * Method 1) combination of request_body, request_body_replacement, request_body_include and request_body_exclude
 * Method 2) combination of rqb_ prefixed column. rqb_ prefix indicate property of a json
 */
class RequestBodyHelper {
    static JSONDataUtils jsonDataUtils = new JSONDataUtils()
    ApplicationResource applicationResource

    def prepareRequestBody(def headers, def row, Class aClass, ApplicationResource applicationResource) {
        this.applicationResource = applicationResource
        List rqb_columns = headers.findAll { it -> it.startsWith(CSVColumn.REQUEST_BODY_PREFIX.getValue()) }
        List request_body_columns = headers.findAll { it -> it.equals(CSVColumn.REQUEST_BODY.getValue()) }
        if (request_body_columns.size() > 0 && rqb_columns.size() > 0) {
            throw new Exception("Either use [ $CSVColumn.REQUEST_BODY, $CSVColumn.REQUEST_BODY_VALUE_REPLACEMENT, $CSVColumn.REQUEST_BODY_INCLUDE, $CSVColumn.REQUEST_BODY_EXCLUDE_KEY ] or $CSVColumn.REQUEST_BODY_PREFIX prefix columns")
        } else if (request_body_columns.size() > 0) {
            resolveRequestBody(headers, row, aClass)
        } else if (rqb_columns.size() > 0) {
            prepareBodyFromPrefixColumn(headers, row)
        }
    }


    /****************************************************************************************
     *                                     Method 1                                         *
     ****************************************************************************************/

    /**
     *
     * @param row
     * @param aClass
     * @return JSONObject/JSONArray/XML as String
     */
    private String resolveRequestBody(def headers, def row, Class aClass) {
        Map valueToReplace = CSVUtils.readKeyValuePairsAsMap(row, CSVColumn.REQUEST_BODY_VALUE_REPLACEMENT.getValue(), applicationResource)
        Map replacements = valueToReplace == null || valueToReplace.isEmpty() ? valueToReplace : applicationResource.getData() + valueToReplace
        String input = resolveRequestBodyValueOrPath(CSVUtils.getRowValue(row, CSVColumn.REQUEST_BODY.getValue()), aClass, replacements)
        List excludeKeys = getRequestBodyExcludeKeys(row)
        //Replace UUID
        if (input && input.contains(Constants.UUID_AUTO_GENERATE)) {
            input = StringUtils.replace(input, Constants.UUID_AUTO_GENERATE, CommonUtils.getRandomUUID())
        }

        if (aClass.getSuperclass().equals(GraphQLSpecification.class)) {
            return prepareQuery(headers, row, input)
        } else if (input?.trim()) {
            def requestBody
            // JsonArray
            if (input.startsWith('[') && input.endsWith(']')) {
                requestBody = JSONArray.fromObject(input)
                //XML content
            } else if (XmlUtils.isXmlContent(input)) {
                List xmlArrayNames = CSVUtils.resolveXmlArrayNames(row, CSVColumn.REQUEST_BODY_XML_ARRAY_NAMES.getValue(), applicationResource)
                requestBody = XmlUtils.convertXmlToJson(input, xmlArrayNames)
                requestBody = removeAndReplaceRequestBody(row, requestBody, excludeKeys)
                return XmlUtils.convertJsonToXml(requestBody)
                //JsonObject
            } else {
                requestBody = JSONObject.fromObject(input)
            }
            return removeAndReplaceRequestBody(row, requestBody, excludeKeys).toString()
        } else {
            input
        }
    }

    /**
     * Take csv cell-value for json or xml and return data as String.
     *
     * @param input
     * @return String
     */
    private String resolveRequestBodyValueOrPath(String input, Class aClass = ECSGSpecification.class, Map valueToReplace = [:]) {
        if (CommonUtils.isNullString(input)) {
            null
        } else if (CommonUtils.isEmptyString(input)) {
            StringUtils.EMPTY
        } else if (input.trim().startsWithAny(Constants.CURLY_OPENING_BRACKETS, Constants.SQUARE_OPENING_BRACKETS)
                || XmlUtils.isXmlContent(input.trim())) {
            def value =ResourceReader.setDataFromProperties(input, applicationResource)
            value = CSVUtils.getFormattedValue(value, applicationResource)
            value
        } else if (aClass.getSuperclass().equals(GraphQLSpecification.class)) {
            if (input.endsWith(".graphql")) {
                URL url = ClassLoader.getSystemClassLoader().getResource(input)
                if(url == null) {
                    String packageDir = aClass.getPackage().getName().tokenize('.').join("/")
                    url = aClass.getResource("/request-input/"+ Constants.AUTO_CONVERTER_PATH + packageDir + "/" + input)
                }
                if (valueToReplace.size() > 0) {
                    ResourceReader.getTextFromUrlAndReplaceWithValues(url, valueToReplace)
                } else {
                    url.text
                }
            } else {
                input
            }
        } else {
            Map replacements = valueToReplace + applicationResource.getData()
            ResourceReader.getTextFromUrlAndReplaceWithValues(ResourceReader.getUrl(input), replacements)
        }
    }

    /**
     * This will prepare query for graphql
     * @param row
     * @return
     */
    private String prepareQuery(def headers, def row, String query) {
        def map = [:]
        String variables
        if(row.columns.variables != null){
            variables = new JsonBuilder(CommonUtils.getCellValueAsMap(CommonUtils.getRowValue(row, CSVColumn.VARIABLES.getValue()), applicationResource, true)).toPrettyString()
        } else {
            variables = new JsonBuilder(getVariables(headers, row)).toPrettyString()
        }
        map.put("variables", variables)
        map.put("query", query)
        new JsonBuilder(map).toPrettyString()
    }

    private Map getVariables(def headers, def row) {
        CSVUtils.getPrefixContainedColumnValue(headers, row, CSVColumn.VARIABLE_PREFIX.getValue(), applicationResource, true)
    }

    private List getRequestBodyExcludeKeys(def row) {
        String excludeKeys = CSVUtils.getRowValue(row, CSVColumn.REQUEST_BODY_EXCLUDE_KEY.getValue())
        if (StringUtils.isNotEmpty(excludeKeys)) {
            excludeKeys = ResourceReader.setDataFromProperties(excludeKeys, applicationResource)
            Arrays.asList(excludeKeys.trim().split(Constants.STATEMENT_SEPARATOR))
        } else {
            []
        }
    }

    /**
     * Take row, requestBody and excludedKeys;
     * then remove excluded keys and replace data from requestBody.
     *
     * @param row
     * @param requestBody
     * @param excludeKeys
     * @return JSONObject
     */
    private def removeAndReplaceRequestBody(def row, def requestBody, List excludeKeys) {
        // Remove keys from request body
        if (CollectionUtils.isNotEmpty(excludeKeys)) {
            requestBody = jsonDataUtils.removeAllKeys(requestBody, excludeKeys)
        }
        // Replace values in request body
        Map replaceMap = CSVUtils.readKeyValuePairsAsMap(row, CSVColumn.REQUEST_BODY_REPLACEMENTS.getValue(), applicationResource)
        requestBody = CSVUtils.replaceJSONValuesByKeys(requestBody, replaceMap)
        // Append values in request body
        Map appendMap = CSVUtils.readKeyValuePairsAsMap(row, CSVColumn.REQUEST_BODY_INCLUDE.getValue(), applicationResource)
        requestBody = CSVUtils.appendJSONValuesByKeys(requestBody, appendMap)
        requestBody
    }

    /****************************************************************************************
     *                                     Method 2                                         *
     ****************************************************************************************/
    /**
     * 2) prepare body from rqb_ prefix column
     * @param headers
     * @param row
     * @return
     */
    private def prepareBodyFromPrefixColumn(def headers, def row) {
        def body = null
        Map keysValueMap = CSVUtils.getPrefixContainedColumnValue(headers, row, CSVColumn.REQUEST_BODY_PREFIX.getValue(), applicationResource, true, true).sort()
        keysValueMap.each { keysValue ->
            String[] keys = keysValue.getKey().tokenize(".")
            def value = keysValue.getValue()
            int keyLevel = keys.size()
            StringBuffer keyBuffer = new StringBuffer('')
            for (int i = 0; i < keys.size(); i++) {

                // prepare key
                if (i == 0) {
                    keyBuffer.append(keys[i])
                } else {
                    keyBuffer.append(".").append(keys[i])
                }
                String key = keyBuffer.toString()

                // initialize body
                if (body == null && key.startsWith('[')) {
                    body = JSONArray.fromObject('[]')
                } else if (body == null) {
                    body = JSONObject.fromObject('{}')
                }

                // for leaf level key add value
                if (i + 1 >= keyLevel) {
                    // if key is an instance an array
                    if (key.endsWith("]")) {
                        body = appendIntoArray(body, key, value, true)
                    } else {
                        body = jsonDataUtils.appendDataByKey(body, key, value)
                    }
                    // append key if it is not present in json
                } else if (!jsonDataUtils.isValidKey(body, key)) {
                    if (key.endsWith(']')) {
                        def (arrayKey, arrayIndex) = splitKeyToGetData(key)
                        if (arrayKey && !jsonDataUtils.isValidKey(body, arrayKey)) {
                            body = jsonDataUtils.appendDataByKey(body, arrayKey, JSONArray.fromObject('[{}]'))
                        } else {
                            body = appendIntoArray(body, key, null)
                        }
                    } else {
                        body = jsonDataUtils.appendDataByKey(body, key, JSONObject.fromObject('{}'))
                    }
                }
            }
        }
        body.toString()
    }

    private appendIntoArray(def body, String key, def value, boolean isLastKey = false) {
        def (arrayKey, arrayIndex) = splitKeyToGetData(key)
        JSONArray array
        if (arrayKey) {
            array = jsonDataUtils.getDataByKey(body, arrayKey)
        } else {
            array = body
        }
        if (array.size() <= arrayIndex) {
            array.add(JSONObject.fromObject('{}'))
        }
        if (isLastKey) {
            String endKey = key.substring(key.lastIndexOf(".") + 1)
            array[arrayIndex][endKey] = value
        }

        if (arrayKey) {
            body = jsonDataUtils.replaceDataByKey(body, arrayKey, array)
        } else {
            body = array
        }
        body
    }

    private def splitKeyToGetData(String key) {
        int startBracketIndex = key.lastIndexOf("[")
        int endBracketIndex = key.lastIndexOf("]")
        String arrayKey = key.substring(0, startBracketIndex)
        Integer index = key.substring(startBracketIndex + 1, endBracketIndex) as Integer
        [arrayKey, index]
    }

    private def appendToMap(def map, String keyString, def value) {
        String[] keys = keyString.split("\\.", 2)
        if (keys.size() == 1) {
            map[keys[0]] = value
        } else {
            appendToMap(map[keys[0]], keys[1], value)
        }
        map
    }
}
