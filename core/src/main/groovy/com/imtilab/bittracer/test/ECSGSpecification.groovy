package com.imtilab.bittracer.test

//import com.imtilab.bittracer.antlr.expression.ValidationParser
import com.xlson.groovycsv.CsvParser
import groovy.io.FileType
import groovy.util.logging.Slf4j

import com.imtilab.bittracer.constant.CSVColumn
import com.imtilab.bittracer.constant.Constants
import com.imtilab.bittracer.constant.PropertiesKey
import com.imtilab.bittracer.model.ApiRequest
import com.imtilab.bittracer.model.ApiResponse
import com.imtilab.bittracer.model.EvidenceContext
import com.imtilab.bittracer.model.MetaData
import com.imtilab.bittracer.model.TestCase
import com.imtilab.bittracer.model.TestRow
import com.imtilab.bittracer.model.ValueStorage
import com.imtilab.bittracer.modulepostexecution.PostExecuteCases
import com.imtilab.bittracer.modulepreexecution.PreExecuteCases
import com.imtilab.bittracer.modulepreexecution.PreExecutionStorage
import com.imtilab.bittracer.parser.CSVTestCaseParser
import com.imtilab.bittracer.parser.Parser
import com.imtilab.bittracer.test.TestSpecification
import com.imtilab.bittracer.test.evidence.ActualResponseEvidenceWriter
import com.imtilab.bittracer.test.evidence.EvidenceWriter
import com.imtilab.bittracer.test.evidence.ExpectedResponseEvidenceWriter
import com.imtilab.bittracer.test.evidence.RequestEvidenceWriter
import com.imtilab.bittracer.test.validation.RuleValidation
import com.imtilab.bittracer.test.validationv2.ValidationSimpleLexer
import com.imtilab.bittracer.test.validationv2.ValidationVisitor
import com.imtilab.bittracer.utils.ApplicationResource
import com.imtilab.bittracer.utils.CSVUtils
import com.imtilab.bittracer.utils.CommonUtils
import com.imtilab.bittracer.utils.ConfigurationResource
import com.imtilab.bittracer.utils.CurlBuilder
import com.imtilab.bittracer.utils.DateUtil
import com.imtilab.bittracer.utils.ExpressionEvaluation
import com.imtilab.bittracer.utils.FileUtils
import com.imtilab.bittracer.utils.HTTPUtils
import com.imtilab.bittracer.utils.JSONDataUtils
import com.imtilab.bittracer.utils.JsonPathUtils
import com.imtilab.bittracer.utils.RequestExecutor
import com.imtilab.bittracer.utils.ResourceReader
import com.imtilab.bittracer.utils.TokenManager
import com.imtilab.bittracer.utils.TokenUtils
import com.imtilab.bittracer.utils.XmlUtils
import kong.unirest.HttpResponse
import kong.unirest.UnirestException
import net.sf.json.JSONArray
import net.sf.json.JSONObject
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree
import org.apache.commons.lang3.StringUtils
import org.apache.http.conn.ConnectTimeoutException
import org.junit.jupiter.api.Assumptions
import spock.lang.Retry
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.lang.annotation.Annotation
import java.util.concurrent.TimeUnit

/**
 * To execute test cases
 */
@Slf4j
abstract class ECSGSpecification extends Specification implements TestSpecification {
    static String proxyHost = null
    static Integer proxyPort = null

    @Shared
    ValidationVisitor validationVisitor = new ValidationVisitor()
    @Shared
    PreExecutionStorage preExecutionStorage = new PreExecutionStorage()
    @Shared
    ApplicationResource applicationResource = new ApplicationResource()
    // all issued valid tokens storage
    @Shared
    Map tokenBox = [:]
    ValueStorage valueStorage = new ValueStorage()

    static {
        if (HTTPUtils.isProxyEnable()) {
            proxyHost = HTTPUtils.getHost()
            proxyPort = HTTPUtils.getPort()
        }
    }
    EvidenceWriter actualResponseEvidenceWriter = new ActualResponseEvidenceWriter()
    EvidenceWriter expectedResponseEvidenceWriter = new ExpectedResponseEvidenceWriter()
    EvidenceWriter requestEvidenceWriter = new RequestEvidenceWriter()
    RuleValidation ruleValidation = new RuleValidation()
    JSONDataUtils jsonDataUtils = new JSONDataUtils()
    boolean isRequestCurlPrintEnable = (Boolean.parseBoolean(ConfigurationResource.instance().getConfigProp(PropertiesKey.IS_CURL_PRINT_ENABLE).toString()))
    boolean isRequestIndividualPrintEnable = (Boolean.parseBoolean(ConfigurationResource.instance().getConfigProp(PropertiesKey.IS_INDIVIDUAL_SECTION_PRINT_ENABLE).toString()))
    @Shared
    Parser csvTestCaseParser = CSVTestCaseParser.getParser()
    ApiResponse apiResponse
    def isRetry = { ApiResponse response ->
        if (response == null) {
            return false
        } else if (isTimeOut(response.status)) {
            return true
        } else if (jsonDataUtils.isValidKey(response.responseBody, "error.code")) {
            return isTimeOut(jsonDataUtils.getDataByKey(response.responseBody, "error.code"))
        }
        return false
    }

    def isTimeOut = { int status -> status == 503 }

    def setupSpec() {
        applicationResource.init()
        csvTestCaseParser.setResources(applicationResource)
        getPreRequisiteExecution()
        setIgnoreParams()
        csvTestCaseParser.setFiles(getTestDataFile())
    }

    def cleanupSpec() {
        tokenBox.clear()
        //cancelAllTokens()
        postRequestExecution()
        System.gc()
    }

    @Unroll
    @Retry(exceptions = [UnirestException.class, ConnectTimeoutException.class, SocketTimeoutException.class],
            count = 2, delay = 10)
    def "#testRow.apiVersion-#testRow.row.case_no: #testRow.row.case_description"(TestRow testRow) {
        log.info("\n\n=============================================")
        log.info("\tStart test: {}", testRow.row.case_no)
        log.info("=============================================")
        log.info("Description: {}", testRow.row.case_description)
        log.info("Precondition: {}", CSVUtils.getRowValue(testRow.row, CSVColumn.PRECONDITION.getValue()))
        //boolean isIgnore = Boolean.parseBoolean(CSVUtils.getRowValue(testRow.row, CSVColumn.IS_IGNORE.getValue()))
        //Assumptions.assumeFalse(isIgnore)
        valueStorage.reset()
        TestCase testCase = csvTestCaseParser.buildTestCase(getClass(), testRow, tokenBox, endPoint().contains(Constants.API_VERSION_VARIABLE))

        applicationResource.setPreRequestData(testCase.preRequestData)

        given: "Server URL"
        boolean isLayerEnable = Boolean.parseBoolean(ConfigurationResource.instance().getConfigProp(PropertiesKey.IS_LAYER_ENABLE))
        String baseUrl = isLayerEnable ? ConfigurationResource.instance().getConfigProp(PropertiesKey.LAYER_URL) : ConfigurationResource.instance().getConfigProp(PropertiesKey.API_BASE_URL)
        String endpoint = endPoint()

        if (StringUtils.isNotEmpty(testCase.apiVersion)) {
            endpoint = endpoint.replace(Constants.API_VERSION_VARIABLE, testCase.apiVersion)
        }

        if (StringUtils.isNotEmpty(testCase.apiCallType)) {
            endpoint = endpoint.replace(Constants.API_CALL_TYPE, testCase.apiCallType)
        }
        if (testCase.apiRequest.url == null)
            testCase.apiRequest.setUrl(baseUrl + endpoint)
        if (isLayerEnable) {
            def s = getLayerHeaders()
            testCase.apiRequest.requestHeaders += s
        }

        when: "Send API request"
        testCase.apiRequest.methodName = method().toString()
        printRequestLog(testCase.apiRequest)
        if (testCase.curlExecutionDelayTimeInSec > 0) {
            log.info(" --------------------------------------------------")
            log.info("| Waiting ${testCase.curlExecutionDelayTimeInSec} seconds before executing api request. |")
            log.info(" --------------------------------------------------")
            TimeUnit.SECONDS.sleep(testCase.curlExecutionDelayTimeInSec)
        }
        HttpResponse httpResponse = method().execute(testCase.apiRequest)
        apiResponse = RequestExecutor.prepareResponse(httpResponse, testCase.apiResponseXmlArrayNames)
        log.info("\nResponse Status: {}", apiResponse.status)
        log.info("\nResponse Headers: \n{}", apiResponse.responseHeaders.toString())
        log.info("Response Body: \n{}", apiResponse.responseBody != null ? apiResponse.responseBody.toString() : apiResponse.responseBody)
        valueStorage.setRequestBody(StringUtils.isEmpty(testCase.apiRequest.requestBody) ? testCase.apiRequest.requestBody : convertRequestBody(testCase.apiRequest.requestBody))
        valueStorage.setResponseBody(apiResponse.responseBody)
        /* TODO: Will be uncomment when get permission on directory */
        // collectEvidence(testCase, apiResponse)

        //execute post request curl: normally used for logout
        csvTestCaseParser.executePostRequestCurlDataMap(testCase.getPostRequestData(), apiResponse.responseBody)

        then: "Validate response"
        assert apiResponse.status.equals(testCase.responseStatus)


        and: "Match actual response with expected response"
        if (testCase.matchResponseFlag) {
            jsonDataUtils.isMatchIgnoreExcludedKeys(apiResponse.responseBody, testCase.expectedResponse, testCase.responseKeyExclude)
        }

        and: "Advanced rules validation"
        if (apiResponse.status.equals(testCase.responseStatus))
            ruleValidation.validateRules(apiResponse.responseBody, testCase.expectedResponse, testCase.getAdvancedValidation(), valueStorage)

        if (testCase.advancedValidationV2 == null) {
            and: "Compare with backend"
            verifyBackedCurl(testCase.backendRequestCurl, apiResponse.responseBody, testCase.backendAdvancedValidation, testCase.compareBackendKeyMapping, testCase.backendRequestCurlValuesToReplace, testCase.backendRequestCurlProxyEnable, testCase.backendRequestCurlSSLEnable)
        } else {
            and: "Advanced rules validation for v2"
            callBackend(testCase.backendRequestCurl, testCase.backendRequestCurlValuesToReplace, testCase.backendRequestCurlProxyEnable, testCase.backendRequestCurlSSLEnable)
            validateStatements(testCase.advancedValidationV2)
        }


        and: "Compare response headers"
        if (testCase.expectedResponseHeaders)
            verifyResponseHeaders(apiResponse.responseHeaders, testCase.expectedResponseHeaders, testCase.apiRequest)

        where:
        testRow << csvTestCaseParser.readTestMatrix(getClass(), endPoint().contains(Constants.API_VERSION_VARIABLE))
    }

    private def getLayerHeaders() {
        Map<String, String> headers = Eval.me(ConfigurationResource.instance().getConfigProp(PropertiesKey.LAYER_HEADER_TO_INCLUDE))
        headers.each { header ->
            String value = header.getValue().trim()
            if (value.startsWith(Constants.GENERATE_TOKEN)) {
                headers[header.getKey()] = TokenUtils.getTokenByTokenSet(value - Constants.GENERATE_TOKEN, tokenBox)
            } else {
                headers[header.getKey()] = value
            }
        }
    }

    def verifyBackedCurl(def backendRequestCurl, def responseBody, def backendAdvancedValidation, def compareBackendKeyMapping, def backendRequestCurlValuesToReplace, def backendRequestCurlProxyEnable, def backendRequestCurlSSLEnable) {
        if (backendRequestCurl.size() == backendAdvancedValidation.size()) {
            backendRequestCurl.each {
                Map valueToReplace = backendRequestCurlValuesToReplace.get(it.key)
                Map replacements = valueToReplace == null ? applicationResource.getData() : applicationResource.getData() + valueToReplace
                String curl = CSVUtils.resolveFromValueOrPathForTxt(it.value, applicationResource, replacements)
                def backEndResponse = HTTPUtils.executeCurl(applicationResource, curl, backendRequestCurlProxyEnable.get(it.key), backendRequestCurlSSLEnable.get(it.key))
                valueStorage.setUpstreamBody(backEndResponse)
                def rules = backendAdvancedValidation.get(it.key)
                if (rules == null) {
                    throw new Exception("No back end rules found for ${it.key}")
                }
                String backendKeys = compareBackendKeyMapping.get(it.key)
                ruleValidation.validateRules(responseBody, backEndResponse, rules, prepareBackendKeyMap(backendKeys), valueStorage)
            }
        } else {
            throw new Exception("Mismatch!! ${CSVColumn.BACKEND_CURL_PREFIX.getValue()} and ${CSVColumn.BACKEND_ADVANCED_VALIDATION_PREFIX.getValue()} column size not match")
        }
    }

    Map prepareBackendKeyMap(String input) {
        Map backendKeyMap = [:]
        if (StringUtils.isNotEmpty(input)) {
            String[] keys = input.split(Constants.STATEMENT_SEPARATOR)
            String key, value
            keys.each {
                try {
                    (key, value) = it.split(Constants.COLON)
                } catch (Exception ex) {
                    throw new Exception("Backend key mapping syntax error near :" + it)
                }
                backendKeyMap.put(key, value)
            }
        }
        backendKeyMap
    }

    def verifyResponseHeaders(def actualHeaders, def expectedHeaders, ApiRequest apiRequest) {
        Map headers = getHeaderAsMap(actualHeaders)
        expectedHeaders.each { key, expectedHeader ->
            def actualHeader = headers.get(key)
            assert actualHeader == resolveExpectedHeader(expectedHeader, apiRequest)
        }
    }

    def resolveExpectedHeader(def input, ApiRequest apiRequest) {
        def expectedHeader
        if (input instanceof String) {
            if (input.startsWith(CSVColumn.REQUEST_HEADER_PREFIX.getValue())) {
                expectedHeader = apiRequest.requestHeaders.get(input - CSVColumn.REQUEST_HEADER_PREFIX.getValue())
            } else if (input.startsWith(CSVColumn.REQUEST_PARAMETER_PREFIX.getValue())) {
                expectedHeader = apiRequest.requestHeaders.get(input - CSVColumn.REQUEST_PARAMETER_PREFIX.getValue())
            } else if (input.startsWith(CSVColumn.REQUEST_BODY.getValue() + '_')) {
                String prefix = CSVColumn.REQUEST_BODY.getValue() + '_'
                expectedHeader = ExpressionEvaluation.evaluate(valueStorage.getRequestBody(), input - prefix, valueStorage)
            } else {
                expectedHeader = input
            }
        } else {
            expectedHeader = input
        }
        CommonUtils.isNullString(expectedHeader) ? null : expectedHeader.toString()
    }

    Map getHeaderAsMap(def actualHeaders) {
        Map headers = [:]
        actualHeaders.each {
            headers.put(it.getName(), it.getValue())
        }
        headers
    }

    /**
     * Cancel all the issued tokens
     */
    private void cancelAllTokens() {
        tokenBox.each { key, value ->
            TokenManager.cancelToken(value)
        }
    }

    private def resolveTestDataPath() {
        String packageName = getClass().getPackage().getName()
        if (endPoint().contains(Constants.API_VERSION_VARIABLE)) {
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
        }
        String packageDir = packageName.tokenize('.').join("/")
        Constants.TEST_CSV_FOLDER + packageDir
    }

    private List getTestDataFile() {
        Class aClass = getClass()
        String path = aClass.getResource(resolveTestDataPath()).getPath()
        File file = new File(path)
        //test data file name pattern "CLASSNAME_XXXXX.csv"
        String fileNamePattern = aClass.getSimpleName() + Constants.UNDER_SCORE
        List testDataFiles = []
        file.eachFile FileType.FILES, {
            String fileName = it.getName()
            if (fileName.startsWith(fileNamePattern) && fileName.endsWith(Constants.CSV_EXTENSION))
                testDataFiles << it
        }
        testDataFiles
    }

    private void setIgnoreParams() {
        MetaData.environment = System.getProperty(PropertiesKey.TEST_CASE_ENVIRONMENT)
        Class aClass = getClass()
        String path = aClass.getResource(resolveTestDataPath()).getPath()
        File file = FileUtils.getIgnoreInFile(path,aClass.getSimpleName())
        if (file.isFile()) {
            Iterator paramIterator = CsvParser.parseCsv(file.getText(), columnNames: [Constants.ENVIRONMENT_STG, Constants.ENVIRONMENT_TEST_PROD, Constants.ENVIRONMENT_PROD], separator: Constants.COMMA, readFirstLine: false)
            paramIterator.each {
                if (org.apache.commons.lang.StringUtils.isNotBlank(it[MetaData.environment]))
                    MetaData.ignoreTests.add(it[MetaData.environment].trim())
            }
        }
    }

    private void collectEvidence(testCase, apiResponse) {
        EvidenceContext context = EvidenceContext.builder()
                .archivePath(evidenceBucket(testCase.apiVersion + "/" + testCase.caseNo))
                .apiRequest(testCase.apiRequest)
                .apiResponse(apiResponse)
                .expectedResponse(testCase.expectedResponse.toString())
                .timestamp(DateUtil.getTimestamp())
                .build()
        requestEvidenceWriter.write(context)
        actualResponseEvidenceWriter.write(context)
        expectedResponseEvidenceWriter.write(context)
    }

    private def convertRequestBody(String requestBody) {
        if (XmlUtils.isXmlContent(requestBody)) {
            XmlUtils.convertXmlToJson(requestBody)
            // JSON formatted content
        } else if (requestBody != null && requestBody.startsWith("[")) {
            JSONArray.fromObject(requestBody)
        } else {
            JSONObject.fromObject(requestBody)
        }
    }

    private def packageName() {
        return [endPoint(), "/", method().toString(), "/"].inject(
                new StringBuffer(), { a, b -> a.append(b); return a }
        ).toString()
    }

    private def evidenceBucket(String tcId) {
        String EVIDENCE_PATH = ConfigurationResource.instance().getConfigProp(PropertiesKey.EVIDENCE_PATH)
        return [EVIDENCE_PATH, packageName(), tcId].inject(
                new StringBuffer(), { a, b -> a.append(b); return a }
        ).toString()
    }

    void printRequestLog(ApiRequest apiRequest) {
        if (isRequestIndividualPrintEnable) {
            log.info("Request URL: {}", apiRequest.url)
            log.info("Request Headers: {}", apiRequest.requestHeaders)
            log.info("Request Params: {}", apiRequest.queryParameters)
            log.info("Request Body: {}", apiRequest.requestBody)
        }

        if (isRequestCurlPrintEnable) {
            log.info("Request curl: {}\n", CurlBuilder.build(apiRequest))
        }
    }

    void validateStatements(String statements) {
        String testEnvironment = System.getProperty(PropertiesKey.TEST_CASE_ENVIRONMENT)
        validationVisitor.valueStorage = valueStorage
        statements.split("\n")*.trim().each { statement ->
            if (testEnvironment.equals(Constants.ENVIRONMENT_STG)
                    && (!statement.startsWith(Constants.PROD_COLON))) {
                if (statement.startsWith(Constants.STG_COLON)) {
                    statement = statement.replaceFirst(Constants.STG_COLON, StringUtils.EMPTY)
                }
                validateStatement(statement)
            } else if ((testEnvironment.equals(Constants.ENVIRONMENT_PROD)
                    || testEnvironment.equals(Constants.ENVIRONMENT_TEST_PROD))
                    && (!statement.startsWith(Constants.STG_COLON))) {
                if (statement.startsWith(Constants.PROD_COLON)) {
                    statement = statement.replaceFirst(Constants.PROD_COLON, StringUtils.EMPTY)
                }
                validateStatement(statement)
            }
        }
    }

    void validateStatement(String statement) {
//        log.info("Statement: {}", statement)
//        validationVisitor.isAsserted = false
//        ValidationSimpleLexer lexer = new ValidationSimpleLexer(CharStreams.fromString(statement))
//        ValidationParser parser = new ValidationParser(new CommonTokenStream(lexer))
//        parser.setBuildParseTree(true)
//        ParseTree tree = parser.exprList()
//        def result = validationVisitor.visit(tree)
//        if (validationVisitor.isAsserted) {
//            assert result
//        } else {
//            throw new Exception("Make sure your statement is asserted")
//        }
    }

    void callBackend(Map backendRequestCurl, Map backendRequestCurlValuesToReplace, Map backendRequestCurlProxyEnable, Map backendRequestCurlSSLEnable) {
        backendRequestCurl.each {
            Map valueToReplace = backendRequestCurlValuesToReplace.get(it.key)
            Map replacements = valueToReplace == null ? applicationResource.getData() : applicationResource.getData() + valueToReplace
            String curl = ResourceReader.getTextAndReplaceWithValues(it.value, replacements)
            valueStorage.setUpstreamBody(HTTPUtils.executeCurl(applicationResource, curl, backendRequestCurlProxyEnable.get(it.key), backendRequestCurlSSLEnable.get(it.key)))
        }
    }

    def getPreRequisiteExecution() {
        // get annotation of class
        Class aClass = getClass()
        if (!aClass.isAnnotationPresent(PreExecuteCases.class))
            return
        Annotation annotation = aClass.getAnnotation(PreExecuteCases.class)
        String[] casesToExecute = annotation.value()


        URL url = aClass.getResource("/pre-module-csv")
        String path = url ? url.getPath() : null
        if (path) {
            File files = new File(path)
            files.eachFile { file ->
                def headers = CsvParser.parseCsv(file.getText(), separator: Constants.COMMA, readFirstLine: true).getAt(0).values
                CsvParser.parseCsv(file.getText(), separator: Constants.COMMA, readFirstLine: false)
                        .findAll { filterTestData(it, casesToExecute) }.each {
                    row ->
                        TestRow testRow1 = TestRow.builder()
                                .headers(headers)
                                .row(row)
                                .apiVersion(StringUtils.EMPTY).build()
                        TestCase testCase = csvTestCaseParser.buildTestCase(getClass(), testRow1, [:], false)
                        testCase.apiRequest.methodName = "POST"
                        log.info("Pre Request curl (module): {}\n", CurlBuilder.build(testCase.apiRequest))
                        HttpResponse httpResponse = RequestExecutor.executeRequest(testCase.apiRequest, proxyHost, proxyPort)
                        ApiResponse apiResponse1 = RequestExecutor.prepareResponse(httpResponse)
                        log.info("Pre Request Response (module): {}\n", apiResponse1.getResponseBody())
                        // PreExecutionStorage.instance.executedList.add(CSVUtils.getRowValue(row, CSVColumn.CASE_NO.value))
                        testCase.preExecutedData.each { pair ->
                            preExecutionStorage.data.put(pair.key, JsonPathUtils.getDataByKey(apiResponse1.responseBody, pair.value))
                        }
                        applicationResource.setPreExecutedStorageData(preExecutionStorage.data)
                }
            }
        }
    }

    boolean filterTestData(def row, String[] casesToExecute) {
        String caseNo = CSVUtils.getRowValue(row, CSVColumn.CASE_NO.value)
        boolean isExecutable = StringUtils.indexOfAny(caseNo, casesToExecute) != -1
        isExecutable && CSVTestCaseParser.isTestEnvironmentMatch(row) && CSVTestCaseParser.isTestMethodMatch(row)
        //&& !PreExecutionStorage.instance.executedList.contains(caseNo)
    }

    def postRequestExecution() {
        // get annotation of class
        Class aClass = getClass()
        if (!aClass.isAnnotationPresent(PostExecuteCases.class))
            return
        Annotation annotation = aClass.getAnnotation(PostExecuteCases.class)
        String[] casesToExecute = annotation.value()
        URL url = aClass.getResource("/post-module-csv")
        String path = url ? url.getPath() : null
        if (path) {
            File files = new File(path)
            files.eachFile { file ->
                def headers = CsvParser.parseCsv(file.getText(), separator: Constants.COMMA, readFirstLine: true).getAt(0).values
                CsvParser.parseCsv(file.getText(), separator: Constants.COMMA, readFirstLine: false)
                        .findAll { filterTestData(it, casesToExecute) }.each {
                    row ->
                        TestRow testRow1 = TestRow.builder()
                                .headers(headers)
                                .row(row)
                                .apiVersion(StringUtils.EMPTY).build()
                        TestCase testCase = csvTestCaseParser.buildTestCase(getClass(), testRow1, [:], false)
                        testCase.apiRequest.methodName = "POST"
                        log.info("Post Request curl (module): {}\n", CurlBuilder.build(testCase.apiRequest))
                        HttpResponse response = RequestExecutor.executeRequest(testCase.apiRequest, proxyHost, proxyPort)
                        log.info("Post Request Response (module): {}\n", response.getBody())
                }
            }
        }
    }

}
