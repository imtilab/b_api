package com.imtilab.bittracer.constant

/**
 * @author Mehadi on 10/18/19
 *
 * Properties files keys
 */
class PropertiesKey {

    static final String API_BASE_URL = "base.url"
    static final String TOKEN_ISSUE_BASE_URL = "token.issue.base.url"
    static final String TOKEN_ISSUE_PATH = "token.issue.path"
    static final String TOKEN_CANCEL_PATH = "token.cancel.path"
    static final String CONNECT_TIMEOUT = "connect.timeout"

    //api key
    static final String WEB_BFF_APIKEY = "web_bff_apikey"
    static final String ICHIBA_TOP_CLIENT_APIKEY = "ichiba_top_client_apikey"
    static final String R_BIC_CLIENT_APIKEY = "r_bic_client_apikey"

    // Proxy Configuration
    static final String PROXY_ENABLE = "proxy.enable"
    static final String PROXY_HOST = "proxy.host"
    static final String PROXY_PORT = "proxy.port"

    // Log print
    static final String IS_CURL_PRINT_ENABLE = "request.curl.print.enable"
    static final String IS_INDIVIDUAL_SECTION_PRINT_ENABLE = "request.individual.print.enable"

    // Test case filter
    static final String TEST_CASE_TYPE = "testCase"
    static final String TEST_CASE_ENVIRONMENT = "profile"
    static final String TEST_CASE_METHOD = "testMethod"
    static final String VERSION_ABLE_MODULE_PACKAGE_NAME = "versionAbleModulePackageName"

    static final String EVIDENCE_PATH = "evidence.path"
    static final String DEFAULT_TOKEN_TYPE = "default.token.type"
    static final String DEPARTURE_AFTER = "token.departure.after"

    // layer
    static final String IS_LAYER_ENABLE = "layer.enable"
    static final String LAYER_URL = "layer.url"
    static final String LAYER_HEADER_TO_INCLUDE = "layer.include.header"

    //error analysis
    static final String IS_ERROR_ANALYSIS_ENABLE = "is.error.analysis.enable"
}
