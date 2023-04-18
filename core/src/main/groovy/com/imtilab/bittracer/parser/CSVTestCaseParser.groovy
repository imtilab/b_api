package com.imtilab.bittracer.parser

import com.xlson.groovycsv.CsvParser
import com.xlson.groovycsv.PropertyMapper
import groovy.util.logging.Slf4j
import com.imtilab.bittracer.constant.CSVColumn
import com.imtilab.bittracer.constant.Constants
import com.imtilab.bittracer.constant.PropertiesKey
import com.imtilab.bittracer.helper.RequestBodyHelper
import com.imtilab.bittracer.model.ApiRequest
import com.imtilab.bittracer.model.TestCase
import com.imtilab.bittracer.model.TestRow
import com.imtilab.bittracer.utils.*
import net.sf.json.JSONObject
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang3.StringUtils

import java.util.stream.Stream

/**
 * CSV file to parse
 * Extract test cases
 */
@Slf4j
class CSVTestCaseParser implements Parser {

    private File[] files
    private def headers
    private static Parser parser
    private JSONDataUtils jsonDataUtils
    private String[] caseTypePrefix = getCaseTypePrefix()
    static String testMethod = System.getProperty(PropertiesKey.TEST_CASE_METHOD).trim().toUpperCase()
    private RequestBodyHelper requestBodyHelper
    private ApplicationResource applicationResource
    private int curlExecutionDelayTimeInSec = 0

    private CSVTestCaseParser() {
        jsonDataUtils = new JSONDataUtils()
        requestBodyHelper = new RequestBodyHelper()
    }

    /**
     * To get single instance of CSVTestCaseParser
     * @return CSVTestCaseParser
     */
    static Parser getParser() {
        if (parser == null) {
            parser = new CSVTestCaseParser()
        }
        return parser
    }

    @Override
    void setFiles(List files) {
        if (CollectionUtils.isEmpty(files)) {
            log.error("No csv files Available")
            throw new Exception("No csv files Available")
        }
        this.files = files
    }

    @Override
    void setResources(ApplicationResource resource) {
        applicationResource = resource
    }

    Stream<TestRow> readTestMatrix(Class aClass, boolean isVersionExist) {
        Stream.Builder<TestRow> testRows = Stream.builder()
        String currentApiVersion
        if (isVersionExist) {
            String[] versionAblePackages = System.getProperty(PropertiesKey.VERSION_ABLE_MODULE_PACKAGE_NAME).split(",")
            String packageName = aClass.getPackage().getName()
            for (int i = 0; i < versionAblePackages.size(); i++) {
                if (packageName.contains(versionAblePackages[i])) {
                    String basePackageName = versionAblePackages[i]
                    String packageNameFromVersion = packageName - basePackageName
                    int indexOfVersionEnd = packageNameFromVersion.indexOf(".")
                    currentApiVersion = packageNameFromVersion.substring(0, indexOfVersionEnd)
                    break
                }
            }
        }
        files.each { file ->
            headers = loadFile(file, true).getAt(0).values
            loadFile(file).findAll { filterTestData(it) }.each {
                row ->
                    String apiVersions = getRowValue(row, CSVColumn.API_VERSION.getValue())
                    boolean isIgnore = Boolean.parseBoolean(getRowValue(row, CSVColumn.IS_IGNORE.getValue()))
                    if(!isIgnore){
                        if (StringUtils.isNotEmpty(apiVersions)) {
                            apiVersions.tokenize(' ,').each {
                                apiVersion ->
                                    if (apiVersion.equalsIgnoreCase(currentApiVersion)) {
                                        row.values[row.columns.api_version] = apiVersion
                                        testRows.accept(TestRow.builder()
                                                .headers(headers)
                                                .row(row)
                                                .apiVersion(apiVersion).build())
                                    }
                            }
                        } else {
                            testRows.accept(TestRow.builder()
                                    .headers(headers)
                                    .row(row)
                                    .apiVersion(StringUtils.EMPTY).build())
                        }
                    }
            }
        }
//        if (CollectionUtils.isEmpty(testRows)) {
//            log.error("No test Cases Available")
//            throw new Exception("No test Cases Available")
//        }
        testRows.build()
    }


    /**
     * To get test case from csv file
     * @return List of TestCase
     */
    TestCase buildTestCase(Class aClass, TestRow testRow, Map tokenBox = [:], boolean isVersionExist = false) {
        headers = testRow.headers
        PropertyMapper row = testRow.row
        String apiVersion = getRowValue(row, CSVColumn.API_VERSION.getValue())
        curlExecutionDelayTimeInSec = 0
        Map featureSpecificData = readFeatureSpecificData(aClass)
        Map dataMap = processPreRequestCurlData(row, featureSpecificData) + featureSpecificData
        applicationResource.setPreRequestData(dataMap)
        Map classWiseCommonPreRequestData = processCommonPreRequestCurlData(row, featureSpecificData)
        applicationResource.setCommonPreRequestData(classWiseCommonPreRequestData)
        Map postRequestDataMap = processPostRequestCurlData(row, featureSpecificData)
        ApiRequest apiRequest = ApiRequest.builder()
                .url(setCustomUrl(row, apiVersion))
                .requestHeaders(setHeader(row, tokenBox))
                .queryParameters(setParameter(row, isVersionExist, apiVersion))
                .requestBody(requestBodyHelper.prepareRequestBody(headers, row, aClass, applicationResource))
                .build()
        TestCase.builder()
                .caseNo(row.getAt(CSVColumn.CASE_NO.getValue()))
                .description(row.getAt(CSVColumn.CASE_DESCRIPTION.getValue()))
                .apiVersion(StringUtils.isEmpty(apiVersion) ? StringUtils.EMPTY : apiVersion)
                .apiRequest(apiRequest)
                .responseStatus(row.getAt(CSVColumn.RESPONSE_STATUS.getValue()) ? Integer.parseInt(row.getAt(CSVColumn.RESPONSE_STATUS.getValue())) : null)
                .expectedResponse(resolveExpectedResponse(row))
                .responseKeyExclude(getExcludeKeys(row, CSVColumn.RESPONSE_KEY_EXCLUDE.getValue()))
                .verifyBackendCall(isTrue(getRowValue(row, CSVColumn.VERIFY_BACKEND_CALL.getValue())))
                .advancedValidation(getAdvancedValidation(row))
                .advancedValidationV2(getAdvancedValidationV2(row))
                .backendRequestCurl(getBackendCurl(row))
                .backendRequestCurlValuesToReplace(getCurlValueReplacement(row))
                .backendRequestCurlProxyEnable(getCurlProxyEnable(row))
                .backendRequestCurlSSLEnable(getCurlSSLEnable(row))
                .backendAdvancedValidation(getBackendColumnValuesMapping(row, CSVColumn.BACKEND_ADVANCED_VALIDATION_PREFIX.getValue()))
                .compareBackendKeyMapping(getBackendColumnValuesMapping(row, CSVColumn.BACKEND_COMPARE_KEY_PREFIX.getValue()))
                .matchResponseFlag(isTrue(getRowValue(row, CSVColumn.MATCH_RESPONSE_FLAG.getValue())))
                .collectBackEndResponse(isTrue(getRowValue(row, CSVColumn.COLLECT_BACKEND_RESPONSE.getValue())))
                .verifyLog(isTrue(getRowValue(row, CSVColumn.VERIFY_LOG.getValue())))
                .expectedResponseHeaders(getPrefixContainedColumnValue(row, CSVColumn.RESPONSE_HEADER_PREFIX.getValue(), false, false))
                .apiCallType(getRowValue(row, CSVColumn.API_CALL_TYPE.getValue()))
        //.isIgnore(Boolean.parseBoolean(getRowValue(row, CSVColumn.IS_IGNORE.getValue())))
                .apiResponseXmlArrayNames(CSVUtils.resolveXmlArrayNames(row, CSVColumn.API_RESPONSE_XML_ARRAY_NAMES.getValue(), applicationResource))
                .preRequestData(dataMap)
                .preExecutedData(processPreExecutedData(row))
                .classWiseCommonPreRequestData(classWiseCommonPreRequestData)
                .postRequestData(postRequestDataMap)
                .curlExecutionDelayTimeInSec(curlExecutionDelayTimeInSec)
                .build()
    }

    @Override
    def loadFile(File file, boolean readFirstLine = false) {
        CsvParser.parseCsv(file.getText(), separator: Constants.COMMA, readFirstLine: readFirstLine)
    }

    private def readFeatureSpecificData(Class aClass) {
        String packageName = aClass.getPackage().getName()
        String[] versionAblePackages = System.getProperty(PropertiesKey.VERSION_ABLE_MODULE_PACKAGE_NAME).split(",")
        for (int i = 0; i < versionAblePackages.size(); i++) {
            if (packageName.contains(versionAblePackages[i])) {
                String basePackageName = versionAblePackages[i]
                String packageNameFromVersion = packageName - basePackageName
                int indexOfVersionEnd = packageNameFromVersion.indexOf(".")
                packageName = basePackageName + packageNameFromVersion.substring(indexOfVersionEnd + 1)
                break
            }
        }
        String packageDir = packageName.tokenize('.').join("/")
        URL url = aClass.getResource(Constants.DATA_FOLDER + Constants.AUTO_CONVERTER_PATH + packageDir + "/" + System.getProperty(PropertiesKey.TEST_CASE_ENVIRONMENT) + "/" + Constants.DATA_FILE)
        if (url != null) {
            String dataFilePath = url.getPath()
            File dataFile = new File(dataFilePath)
            return ResourceReader.loadInputStreamToMap(new FileInputStream(dataFile), true)
        }
        return Collections.EMPTY_MAP
    }

    private def processPreRequestCurlData(def row, Map featureSpecificData) {
        Map preRequestCurlValueReplacement = getPreRequestCurlValueReplacement(row, CSVColumn.PRE_REQUEST_CURL_VALUE_REPLACEMENT_PREFIX.getValue())
        Map preRequestCurl = getPreRequestCurl(row, CSVColumn.PRE_REQUEST_CURL_PREFIX.getValue(), featureSpecificData, preRequestCurlValueReplacement, false)

        Map preRequestCurlResponseKey = getPreRequestCurlResponseKey(row, CSVColumn.PRE_REQUEST_CURL_RESPONSE_KEY_PREFIX.getValue())
        Map valueMap = processPreAndPostRequestCurlData(preRequestCurl, preRequestCurlValueReplacement, preRequestCurlResponseKey)
        valueMap
    }

    private def processPostRequestCurlData(def row, Map featureSpecificData) {
        Map postRequestCurlValueReplacementMap = getPreRequestCurlValueReplacement(row, CSVColumn.POST_REQUEST_CURL_VALUE_REPLACEMENT_PREFIX.getValue())
        Map postRequestCurlMap = getPreRequestCurl(row, CSVColumn.POST_REQUEST_CURL_PREFIX.getValue(), featureSpecificData, postRequestCurlValueReplacementMap, true)

        Map postRequestCurlResponseKeyMap = getPreRequestCurlResponseKey(row, CSVColumn.POST_REQUEST_CURL_RESPONSE_KEY_PREFIX.getValue())
        Map map = [:]
        map.put("requestCurlMap", postRequestCurlMap)
        map.put("requestCurlValueReplacementMap", postRequestCurlValueReplacementMap)
        map.put("requestCurlResponseKeyMap", postRequestCurlResponseKeyMap)
        map
    }

    private def processCommonPreRequestCurlData(def row, Map featureSpecificData) {
        Map commonPreRequestCurlValueReplacementMap = getPreRequestCurlValueReplacement(row, CSVColumn.COMMON_PRE_REQUEST_CURL_VALUE_REPLACEMENT_PREFIX.getValue())
        Map commonPreRequestCurlMap = getPreRequestCurl(row, CSVColumn.COMMON_PRE_REQUEST_CURL_PREFIX.getValue(), featureSpecificData, commonPreRequestCurlValueReplacementMap, false)
        Map commonPreRequestCurlResponseKeyMap = getPreRequestCurlResponseKey(row, CSVColumn.COMMON_PRE_REQUEST_CURL_RESPONSE_KEY_PREFIX.getValue())
        Map valueMap = processCommonRequestCurlData(commonPreRequestCurlMap, commonPreRequestCurlValueReplacementMap, commonPreRequestCurlResponseKeyMap)
        valueMap
    }

    private def processPreAndPostRequestCurlData(Map requestCurlMap,
                                                 Map requestCurlValueReplacementMap,
                                                 Map requestCurlResponseKeyMap) {
        Map valueMap = [:]
        requestCurlMap.each {
            Map valueToReplace = requestCurlValueReplacementMap.get(it.key)
            Map replacements = valueToReplace == null ? applicationResource.getData() : applicationResource.getData() + valueToReplace
            String curl = CSVUtils.resolveFromValueOrPathForTxt(it.value, applicationResource, replacements)
            curl = resolveAutoGenStartAndEndDate(curl)
            def preReqCurlResponse = HTTPUtils.executeCurl(applicationResource, curl, null)
            Map preReqKeyValue = requestCurlResponseKeyMap.get(it.key)
            preReqKeyValue.each { reqKey, preReqResponseKey ->
                def value
                try {
                    String key, postFilter
                    if (preReqResponseKey.contains("::"))
                        (key, postFilter) = preReqResponseKey.split("::")
                    else
                        key = preReqResponseKey
                    value = JsonPathUtils.getDataByKey(preReqCurlResponse, key, postFilter)
                } catch (e) {
                }
                valueMap.put(reqKey, value)
            }
        }
        valueMap
    }

    private def processCommonRequestCurlData(Map commonRequestCurlMap,
                                             Map commonRequestCurlValueReplacementMap,
                                             Map commonRequestCurlResponseKeyMap) {
        Map valueMap = [:]
        commonRequestCurlMap.each {
            Map preReqKeyValue = commonRequestCurlResponseKeyMap.get(it.key)
            int counter = 0
            preReqKeyValue.each {
                def prevRespValue = applicationResource.getDataProp(it.key)
                if (prevRespValue) {
                    counter++
                }
            }
            if (preReqKeyValue != null && counter != preReqKeyValue.size()) {
                // all key-value not stored from any prev curl response
                Map valueToReplace = commonRequestCurlValueReplacementMap.get(it.key)
                Map replacements = valueToReplace == null ? applicationResource.getData() : applicationResource.getData() + valueToReplace
                String curl = CSVUtils.resolveFromValueOrPathForTxt(it.value, applicationResource, replacements)
                curl = resolveAutoGenStartAndEndDate(curl)
                def preReqCurlResponse = HTTPUtils.executeCurl(applicationResource, curl, null)
                preReqKeyValue.each { reqKey, preReqResponseKey ->
                    def value
                    try {
                        String key, postFilter
                        if (preReqResponseKey.contains("::"))
                            (key, postFilter) = preReqResponseKey.split("::")
                        else
                            key = preReqResponseKey
                        value = JsonPathUtils.getDataByKey(preReqCurlResponse, key, postFilter)
                    } catch (e) {
                    }
                    valueMap.put(reqKey, value)
                }
            }
        }
        valueMap
    }

    void executePostRequestCurlDataMap(Map map, def responseBody) {
        Map postRequestCurlMap = map.get("requestCurlMap")
        Map postRequestCurlValueReplacementMap = map.get("requestCurlValueReplacementMap")
        Map postRequestCurlResponseKeyMap = map.get("requestCurlResponseKeyMap")

        postRequestCurlMap.each {
            Map reqKeyValue = postRequestCurlResponseKeyMap.get(it.key)
            Map valueMap = [:]
            reqKeyValue.each { reqKey, responseKey ->
                def value
                try {
                    String key, postFilter
                    if (responseKey.contains("::"))
                        (key, postFilter) = responseKey.split("::")
                    else
                        key = responseKey
                    value = JsonPathUtils.getDataByKey(responseBody, key, postFilter)
                } catch (e) {
                }
                valueMap.put(reqKey, value)
            }
            Map valueToReplace = postRequestCurlValueReplacementMap.get(it.key)
            Map replacements = valueToReplace == null ? applicationResource.getData() : applicationResource.getData() + valueToReplace
            if (valueMap.size() > 0) {
                replacements = replacements + valueMap
            }
            String curl = CSVUtils.resolveFromValueOrPathForTxt(it.value, applicationResource, replacements)
            def preReqCurlResponse = HTTPUtils.executeCurl(applicationResource, curl, null)
        }
    }

    private Map processPreExecutedData(def row) {
        Map preExecutedData = [:]
        String input = CSVUtils.getRowValue(row, CSVColumn.PRE_EXECUTED_DATA.value)
        if (StringUtils.isBlank(input))
            return preExecutedData
        input.split(Constants.NEW_LINE).each { line ->
            String key, value
            (key, value) = line.split("--", 2)
            preExecutedData.put(key, value)
        }
        preExecutedData
    }

    private Map getCurlValueReplacement(def row) {
        Map curlMap = [:]
        List requestHeaders = findByPrefix(CSVColumn.CURL_VALUE_REPLACEMENTS_PREFIX.getValue())
        requestHeaders.each { header ->
            String key = header.replaceFirst(CSVColumn.CURL_VALUE_REPLACEMENTS_PREFIX.getValue(), StringUtils.EMPTY)
            def curl = row.toMap().get(header)
            Map valueMap = [:]
            if (curl?.trim()) {
                String[] pair = curl.split(Constants.STATEMENT_SEPARATOR)
                pair.each {
                    String variable, replacementValue
                    try {
                        (variable, replacementValue) = it.split(Constants.VALUE_SPLITTER)
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        throw new Exception("Syntex error near : ${pair}")
                    }
                    def value = ResourceReader.setDataFromProperties(replacementValue, applicationResource)
                    valueMap.put(variable, CommonUtils.convertToPrimitive(value))
                }
            }
            curlMap.put(key, valueMap)
        }
        curlMap
    }

    private Map getCurlSSLEnable(def row) {
        Map curlMap = [:]
        List requestHeaders = findByPrefix(CSVColumn.CURL_SSL_ENABLE_PREFIX.getValue())
        requestHeaders.each { header ->
            String key = header.replaceFirst(CSVColumn.CURL_SSL_ENABLE_PREFIX.getValue(), StringUtils.EMPTY)
            def curl = row.toMap().get(header)
            if (curl == null || curl.equalsIgnoreCase(CSVColumn.IGNORE.getValue()) || curl.equals(StringUtils.EMPTY)) {
                return
            }
            curlMap.put(key, Boolean.parseBoolean(curl))
        }
        curlMap
    }

    private Map getCurlProxyEnable(def row) {
        Map curlMap = [:]
        List requestHeaders = findByPrefix(CSVColumn.CURL_PROXY_ENABLE_PREFIX.getValue())
        requestHeaders.each { header ->
            String key = header.replaceFirst(CSVColumn.CURL_PROXY_ENABLE_PREFIX.getValue(), StringUtils.EMPTY)
            def curl = row.toMap().get(header)
            if (curl == null || curl.equalsIgnoreCase(CSVColumn.IGNORE.getValue()) || curl.equals(StringUtils.EMPTY)) {
                return
            }
            curlMap.put(key, Boolean.parseBoolean(curl))
        }
        curlMap
    }

    private Map getBackendColumnValuesMapping(def row, String columnPrefix) {
        Map backendMap = [:]
        List requestHeaders = findByPrefix(columnPrefix)
        requestHeaders.each { header ->
            String key = header.replaceFirst(columnPrefix, StringUtils.EMPTY)
            def value = row.toMap().get(header)
            if (value.equalsIgnoreCase(CSVColumn.IGNORE.getValue()) || value.equals(StringUtils.EMPTY)) {
                return
            }
            backendMap.put(key, ResourceReader.setDataFromProperties(value, applicationResource))
        }
        return backendMap
    }

    private Map getBackendCurl(def row) {
        Map curlMap = [:]
        List requestHeaders = findByPrefix(CSVColumn.BACKEND_CURL_PREFIX.getValue())
        requestHeaders.each { header ->
            String key = header.replaceFirst(CSVColumn.BACKEND_CURL_PREFIX.getValue(), StringUtils.EMPTY)
            def curl = row.toMap().get(header)
            if (curl == null || curl.equalsIgnoreCase(CSVColumn.IGNORE.getValue()) || curl.equals(StringUtils.EMPTY)) {
                return
            }
            curlMap.put(key, CSVUtils.resolveFromValueOrPathForTxtInput(curl))
        }
        return curlMap
    }

    private Map getPreRequestCurl(def row, def csvColumnKey, Map featureSpecificData, Map processedData = [:], boolean isPostRequest) {
        Map curlMap = [:]
        List requestHeaders = findByPrefix(csvColumnKey)
        requestHeaders.each { header ->
            String key = header.replaceFirst(csvColumnKey, StringUtils.EMPTY)
            def curl = row.toMap().get(header)
            if (curl == null || curl.equalsIgnoreCase(CSVColumn.IGNORE.getValue()) || curl.equals(StringUtils.EMPTY)) {
                return
            }
            if (isPostRequest) {
                curlMap.put(key, CSVUtils.resolveFromValueOrPathForTxtInput(curl))
            } else {
                Map data = processedData.get(key)
                if (data != null)
                    curlMap.put(key, CSVUtils.resolveFromValueOrPathForTxt(curl, applicationResource, featureSpecificData + data))
                else
                    curlMap.put(key, CSVUtils.resolveFromValueOrPathForTxt(curl, applicationResource, featureSpecificData))
            }
        }
        return curlMap
    }

    private Map getPreRequestCurlValueReplacement(def row, def csvColumnKey) {
        Map curlMap = [:]
        List requestHeaders = findByPrefix(csvColumnKey)
        requestHeaders.each { header ->
            String key = header.replaceFirst(csvColumnKey, StringUtils.EMPTY)
            def curl = row.toMap().get(header)
            if (curl == null || curl.equalsIgnoreCase(CSVColumn.IGNORE.getValue()) || curl.equals(StringUtils.EMPTY)) {
                return
            }
            Map valueMap = [:]
            if (curl?.trim()) {
                String[] pair = curl.split(Constants.STATEMENT_SEPARATOR)
                pair.each {
                    String variable, replacementValue
                    try {
                        (variable, replacementValue) = it.split(Constants.VALUE_SPLITTER)
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        throw new Exception("Syntex error near : ${pair}")
                    }
                    def value = ResourceReader.setDataFromProperties(replacementValue, applicationResource)
                    valueMap.put(variable, CommonUtils.convertToPrimitive(value))
                }
            }
            curlMap.put(key, valueMap)
        }
        curlMap
    }

    private Map getPreRequestCurlResponseKey(def row, def csvColumnKey) {
        Map curlMap = [:]
        List requestHeaders = findByPrefix(csvColumnKey)
        requestHeaders.each { header ->
            String key = header.replaceFirst(csvColumnKey, StringUtils.EMPTY)
            def curl = row.toMap().get(header)
            if (curl == null || curl.equalsIgnoreCase(CSVColumn.IGNORE.getValue()) || curl.equals(StringUtils.EMPTY)) {
                return
            }
            Map valueMap = [:]
            if (curl?.trim()) {
                String[] pair = curl.split(Constants.STATEMENT_SEPARATOR)
                pair.each {
                    String variable, replacementValueKey
                    try {
                        (variable, replacementValueKey) = it.split(Constants.VALUE_SPLITTER)
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        throw new Exception("Syntex error near : ${pair}")
                    }
                    valueMap.put(variable, replacementValueKey)
                }
            }
            curlMap.put(key, valueMap)
        }
        curlMap
    }

    private Map getPrefixContainedColumnValue(def row, String prefix, boolean isTypeConversionEnable = false, boolean isPutEmptyValue = true) {
        Map map = [:]
        List columns = findByPrefix(prefix)
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
                if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false"))
                    value = value.toLowerCase()
                value = ResourceReader.setDataFromProperties(value, applicationResource)
                // Convert into primitive type
                if (isTypeConversionEnable) {
                    value = CommonUtils.convertToPrimitive(value)
                }
                map.put(key, value)
            } else if (isPutEmptyValue)
                map.put(key.trim(), value)
        }
        return map
    }

    private Map setHeader(def row, Map tokenBox) {
        Map requestHeaders
        // single column 'request_headers' based, not multiple' rqh_' based
        if (row.columns.request_headers != null) {
            requestHeaders = CommonUtils.getCellValueAsMap(getRowValue(row, CSVColumn.REQUEST_HEADERS.getValue()), applicationResource)
        } else {
            requestHeaders = getPrefixContainedColumnValue(row, CSVColumn.REQUEST_HEADER_PREFIX.getValue())
        }
        addToken(requestHeaders, row, tokenBox)
        Map headers = [:]
        requestHeaders.each {
            def value = it.value
            if (CommonUtils.isExponentialNumber(value)) {
                value = CommonUtils.exponentialToDecimal(value).toString()
            }
            if (value.startsWith(Constants.GENERATE_FROM_SET)) {
                headers.put(it.key, TokenUtils.getTokenByTokenSet(value - Constants.GENERATE_FROM_SET, tokenBox))
            } else {
                headers.put(it.key, ResourceReader.setDataFromProperties(value, applicationResource))
            }
        }
        headers
    }

    private def addToken(Map requestHeaders, def row, Map tokenBox) {
        Map map = getPrefixContainedColumnValue(row, CSVColumn.GENERATE_TOKEN_PREFIX.getValue())
        String tokenSet = map.values().size() ? map.values().getAt(0) : StringUtils.EMPTY
        Map headers = [:]
        if (StringUtils.isNotEmpty(tokenSet)) {
            String authKey = map.keySet().size() ? map.keySet().getAt(0) : StringUtils.EMPTY
            headers.put(authKey, TokenUtils.getTokenByTokenSet(tokenSet, tokenBox))
            requestHeaders << headers
        }
    }

    private Map setParameter(def row, boolean isVersionExist, String version) {
        Map parameterMap
        // single column 'query_parameters' based, not multiple' rqp_' based
        if (row.columns.query_parameters != null) {
            parameterMap = CommonUtils.getCellValueAsMap(getRowValue(row, CSVColumn.QUERY_PARAMETERS.getValue()), applicationResource)
        } else {
            parameterMap = getPrefixContainedColumnValue(row, CSVColumn.REQUEST_PARAMETER_PREFIX.getValue())
        }
        Map params = [:]
        parameterMap.each {
            params.put(it.key, ResourceReader.setDataFromProperties(it.value, applicationResource))
        }
        if (!isVersionExist && StringUtils.isNotEmpty(version))
            params.put(Constants.API_VERSION_KEY, version)
        return params
    }

    /**
     * Take csv cell-value for json or xml and return data as String.
     *
     * @param input
     * @return String
     */
    private String resolveFromValueOrPath(String input) {
        if (CommonUtils.isNullString(input)) {
            null
        } else if (CommonUtils.isEmptyString(input)) {
            StringUtils.EMPTY
        } else if (input.trim().startsWithAny(Constants.CURLY_OPENING_BRACKETS, Constants.SQUARE_OPENING_BRACKETS)
                || XmlUtils.isXmlContent(input.trim())) {
            ResourceReader.setDataFromProperties(input, applicationResource)
        } else {
            ResourceReader.getTextFromUrlAndReplaceWithValues(ResourceReader.getUrl(input), applicationResource.getData())
        }
    }

    private def getExcludeKeys(def row, String property) {
        def keyString = getRowValue(row, property)
        keyString ? keyString.tokenize(StringUtils.SPACE + Constants.STATEMENT_SEPARATOR) : []
    }

    private def getAdvancedValidation(def row) {
        String rules = getRowValue(row, CSVColumn.ADVANCED_VALIDATION.getValue())
        if (StringUtils.isNotEmpty(rules)) {
            rules = ResourceReader.setDataFromProperties(rules, applicationResource)
        }
        rules
    }

    private def getRowValue(def row, String property) {
        try {
            if (row.propertyMissing(property)) {
                Eval.x(row, 'x.' + property)
            }
        } catch (MissingPropertyException e) {
            null
        }
    }

    /**
     * Take csv row and return expectedResponse as JSONObject.
     *
     * @param row
     * @return JSONObject
     */
    private def resolveExpectedResponse(def row) {
        String input = resolveFromValueOrPath(getRowValue(row, CSVColumn.EXPECTED_RESPONSE.getValue()))
        def expectedResponse
        //XML content
        if (input != null && XmlUtils.isXmlContent(input)) {
            List xmlArrayNames = CSVUtils.resolveXmlArrayNames(row, CSVColumn.EXPECTED_RESPONSE_XML_ARRAY_NAMES.getValue(), applicationResource)
            expectedResponse = XmlUtils.convertXmlToJson(input, xmlArrayNames)
            //JsonObject
        } else {
            expectedResponse = JSONObject.fromObject(input)
        }

        Map keyValuePairs = CSVUtils.readKeyValuePairsAsMap(row, CSVColumn.EXPECTED_RESPONSE_REPLACEMENTS.getValue(), applicationResource)
        CSVUtils.replaceJSONValuesByKeys(expectedResponse, keyValuePairs)
    }

    private boolean isTrue(String value) {
        return value ? Boolean.parseBoolean(value) : false
    }

    private def findByPrefix(String prefix) {
        return headers.findAll { it -> it.startsWith(prefix) }
    }

    private boolean filterTestData(def data) {
        isTestEnvironmentMatch(data) && isCaseTypeMatch(data) && isTestMethodMatch(data)
    }

    static boolean isTestEnvironmentMatch(def data) {
        String taggedEnvironment = data.tagged_environment
        String testEnvironment = System.getProperty(PropertiesKey.TEST_CASE_ENVIRONMENT)
        !taggedEnvironment || taggedEnvironment.toUpperCase().replaceAll(/\s*/, "").split(/,/).contains(testEnvironment.toUpperCase())
    }

    private boolean isCaseTypeMatch(def data) {
        String caseNo = data.case_no
        caseNo.toUpperCase().startsWithAny(caseTypePrefix)
    }

    static boolean isTestMethodMatch(def data) {
        if (testMethod.equals(Constants.TEST_METHOD_ALL)) {
            true
        } else {
            String methods = data.test_method
            methods ? methods.toUpperCase().split(",")*.trim().contains(testMethod) : false
        }
    }

    private List getCaseTypePrefix() {
        String[] caseTypes = System.getProperty(PropertiesKey.TEST_CASE_TYPE).toUpperCase().split(",")
        List typesPrefix = []
        caseTypes.each {
            if (StringUtils.isNotEmpty(it)) {
                typesPrefix.add(it.trim().getAt(0))
            }
        }
        typesPrefix
    }

    private def getAdvancedValidationV2(def row) {
        String rules = getRowValue(row, CSVColumn.ADVANCED_VALIDATION_V2.getValue())
        if (StringUtils.isNotEmpty(rules)) {
            rules = ResourceReader.setDataFromProperties(rules, applicationResource)
        }
        rules
    }

    private String setCustomUrl(def row, String apiVersion) {
        String url = getRowValue(row, CSVColumn.CUSTOM_URL.getValue())
        if (CommonUtils.isNullString(url)) {
            url
        } else {
            if (!CommonUtils.isNullString(apiVersion)) {
                url = url.replace(Constants.API_VERSION_VARIABLE, apiVersion)
            }
            ResourceReader.setDataFromProperties(url, applicationResource)
        }
    }

    private def resolveAutoGenStartAndEndDate(def curl){
        // Replace START and END date
        if (curl && curl.contains(Constants.START_DATE_AUTO_GENERATE)) {
            List startEndDateList = DateUtil.generateStartAndEndDate()
            def startDateTime = startEndDateList.get(0)
            def endDateTime = startEndDateList.get(1)
            curl = StringUtils.replace(curl, Constants.START_DATE_AUTO_GENERATE, startDateTime)
            curl = StringUtils.replace(curl, Constants.END_DATE_AUTO_GENERATE, endDateTime)
            curlExecutionDelayTimeInSec = startEndDateList.get(2)
            applicationResource.setCommonPreRequestData([
                    "START_DATE_AUTO_GENERATE": startDateTime,
                    "END_DATE_AUTO_GENERATE": endDateTime
            ])
        }
        curl
    }
}
