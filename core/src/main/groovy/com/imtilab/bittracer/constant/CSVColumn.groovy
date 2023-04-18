package com.imtilab.bittracer.constant

/**
 * For CSV file column
 * Header name, cell identifier
 */
enum CSVColumn {
    CASE_NO("case_no"),
    CASE_DESCRIPTION("case_description"),
    PRECONDITION("precondition"),
    API_VERSION("api_version"),
    CUSTOM_URL('custom_url'),
    PRE_EXECUTED_DATA('pre_executed_data'),
    RESPONSE_STATUS("response_status"),
    REQUEST_HEADER_PREFIX("rqh_"),
    RESPONSE_HEADER_PREFIX("rsh_"),
    REQUEST_PARAMETER_PREFIX("rqp_"),
    REQUEST_BODY_PREFIX("rqb_"),
    VARIABLE_PREFIX("variable_"),
    BACKEND_CURL_PREFIX("backend_request_curl_"),
    BACKEND_ADVANCED_VALIDATION_PREFIX("backend_advanced_validation_"),
    BACKEND_COMPARE_KEY_PREFIX("compare_backend_key_mapping_"),
    IGNORE("ignore"),
    EXPECTED_RESPONSE("expected_response"),
    REQUEST_BODY("request_body"),
    REQUEST_BODY_VALUE_REPLACEMENT("request_body_replacement"),
    REQUEST_BODY_XML_ARRAY_NAMES("request_body_xml_array_names"),
    EXPECTED_RESPONSE_XML_ARRAY_NAMES("expected_response_xml_array_names"),
    API_RESPONSE_XML_ARRAY_NAMES("api_response_xml_array_names"),
    RESPONSE_KEY_EXCLUDE("response_key_exclude"),
    EXPECTED_RESPONSE_REPLACEMENTS("expected_response_replacements"),
    REQUEST_BODY_REPLACEMENTS("request_body_replacements"),
    CURL_VALUE_REPLACEMENTS_PREFIX("curl_value_replacement_"),
    CURL_PROXY_ENABLE_PREFIX("curl_proxy_enable_"),
    CURL_SSL_ENABLE_PREFIX("curl_ssl_enable_"),
    PRE_REQUEST_CURL_PREFIX("pre_request_curl_"),
    PRE_REQUEST_CURL_VALUE_REPLACEMENT_PREFIX("pre_request_key_value_replacement_"),
    PRE_REQUEST_CURL_RESPONSE_KEY_PREFIX("pre_request_response_key_"),
    COMMON_PRE_REQUEST_CURL_PREFIX("common_pre_request_curl_"),
    COMMON_PRE_REQUEST_CURL_VALUE_REPLACEMENT_PREFIX("common_pre_request_key_value_replacement_"),
    COMMON_PRE_REQUEST_CURL_RESPONSE_KEY_PREFIX("common_pre_request_response_key_"),
    POST_REQUEST_CURL_PREFIX("post_request_curl_"),
    POST_REQUEST_CURL_VALUE_REPLACEMENT_PREFIX("post_request_key_value_replacement_"),
    POST_REQUEST_CURL_RESPONSE_KEY_PREFIX("post_request_response_key_"),
    ADVANCED_VALIDATION("advanced_validation"),
    ADVANCED_VALIDATION_V2("advanced_validation_v2"),
    MATCH_RESPONSE_FLAG("match_response_flag"),
    COLLECT_BACKEND_RESPONSE("collect_backend_response"),
    VERIFY_LOG("verify_log"),
    VERIFY_BACKEND_CALL("verify_backend_call"),
    GENERATE_TOKEN_PREFIX("generate_token_"),
    REQUEST_BODY_EXCLUDE_KEY("request_body_exclude_key"),
    REQUEST_BODY_INCLUDE("request_body_include"),
    API_CALL_TYPE("api_call_type"),
    IS_IGNORE("is_ignore"),
    REQUEST_HEADERS("request_headers"),
    QUERY_PARAMETERS("query_parameters"),
    VARIABLES("variables")

    private final String value

    CSVColumn(final String text) {
        this.value = text
    }

    String getValue() {
        return value
    }

}
