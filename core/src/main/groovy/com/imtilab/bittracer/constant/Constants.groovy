package com.imtilab.bittracer.constant
/**
 * Constant file
 */
class Constants {

    static final String STATEMENT_SEPARATOR = "\n"
    static final String KEY_SPLITTER = "###"
    static final String MULTI_FUN_IDENTIFIER = "##"
    static final String DATE_FORMAT_SPLITTER = "::"
    static final String DATE_FORMAT_VALUE_SPLITTER = "#"
    static final String VALUE_SPLITTER = "--"

    static final String NEW_LINE = "\n"
    static final String COLON = ":"
    static final char COMMA = ','
    static final String UNDER_SCORE = "_"
    static final String CURLY_OPENING_BRACKETS = "{"
    static final String CURLY_CLOSING_BRACKETS = "}"
    static final String SQUARE_OPENING_BRACKETS = "["
    static final String CSV_EXTENSION = ".csv"
    static final String SPACE = " "
    static final String NEW_LINE_COMMA = "\n|,"
    static final String LAST_INDEX = "n"

    static final String KEY_SPLITTER_REGEX = "^(\\[[].]+)|\\[|([].]+)"
    static final String DIGIT_REGEX = "\\[[0-9]+\\]"
    static final String START_WITH_ARRAY_INDEX_REGEX = "^(\\[[0-9]+]).*"

    static final String API_VERSION_VARIABLE = '${version}'
    static final String API_VERSION_KEY = "version"
    static final String API_CALL_TYPE = '${apiCallType}'

    static final String CHAR_SET_UTF8 = "utf-8"

    static final String TEST_CSV_FOLDER = "/test-csv/" /*TODO: need to change file separator*/
    static final String IGNORE_IN = "IgnoreIn"
    static final String DATA_FOLDER = "/data/"
    static final String AUTO_CONVERTER_PATH = "from-autoconverter/"

    static final String ENVIRONMENT_STG = "stg"
    static final String ENVIRONMENT_TEST_PROD = "test-prod"
    static final String ENVIRONMENT_PROD = "prod"
    static final String STG_COLON = "STG::"
    static final String PROD_COLON = "PROD::"

    static final String DATA_FILE = "data.properties"
    static final String CONFIG_FILE = "config.properties"
    static final String GRADLE_FILE = "gradle.properties"
    static final String YAML_DATA_FILE = "data.yaml"

    static final String DATA_TYPE_NUMBER = "Number"
    static final String DATA_TYPE_OBJECT = "Object"
    static final String JSON_OBJECT_CLASS = "net.sf.json.JSONObject"
    static final String DATA_TYPE_LIST = "List"
    static final String DATA_TYPE_DECIMAL = "Decimal"
    static final String DATA_TYPE_OF_ARRAY = "Array of "
    static final String JSON_ARRAY_CLASS = "net.sf.json.JSONArray"

    static final String DOT_TXT = ".txt"

    static final int STATUS_CODE_200 = 200

    static final String JSON_RESPONSE_ERROR = "Invalid Json response format"
    public static def DASH_CONTAINING_ERROR_MSG = { resourceName -> return "${resourceName}, variable name should not contain - (dash). You can use _ (underscore)" }
    public static def RESOURCE_NOT_IN_CLASSPATH = { resourceName -> return "${resourceName} not exists in classpath" }
    public static def PROPERTY_NOT_IN_RESOURCE = { propertyName -> return "${propertyName} not exists in resource" }
    public static def NUMBER_OF_ARGUMENT_NOT_MATCHED = { functionName, numberOfArgument -> return "${functionName} function required ${numberOfArgument} arguments" }

    public static def GET_SPLITTER_REGX = { splitter -> return "${splitter}(?=(?:[^\"]*\"[^\"]*\")*[^\"]*\$)" }

    public final static String CURRENT_DATETIME = "CURRENT_DATETIME"

    static final String DASH = "-"
    static final String DASH_DOT_REGEX = "[-.]"
    public static boolean APPEND_ON_FILE_WRITE = true
    public static String REQUEST_EVIDENCE_FILE = "request_"
    public static String ACTUAL_RESPONSE_EVIDENCE_FILE = "actual-response_"
    public static String EXPECTED_RESPONSE_EVIDENCE_FILE = "expected-response_"
    public static String TXT_FILE = ".txt"
    public static String TIMESTAMP_FORMAT = "yyyyMMddHHmmss"

    public static String RESPONSE_HEADERS = "======================== Response Headers ========================\n"
    public static String RESPONSE_STATUS = "\n======================== Response Status ========================\n"
    public static String RESPONSE_BODY = "\n======================== Response Body ========================\n"
    public static String REQUEST_URL = "======================== Request Url ========================\n"
    public static String REQUEST_HEADERS = "\n======================== Request Headers ========================\n"
    public static String QUERY_PARAMETERS = "\n======================== Query Parameters ========================\n"
    public static String REQUEST_BODY = "\n======================== Request Body ========================\n"

    // Token properties
    // response
    static final String ACCESS_TOKEN = "access_token"
    static final String REFRESH_TOKEN = "refresh_token"
    static final String TOKEN_TYPE = "token_type"
    static final String EXPIRES_IN = "expires_in"
    static final String SCOPE = "scope"
    static final String ERROR_DESCRIPTION = "error_description"
    static final String ERROR = "error"
    //request
    static final String CLIENT_ID = "client_id"
    static final String CLIENT_SECRET = "client_secret"

    static final String GENERATE_FROM_SET = "GENERATE_FROM_SET::"
    static final String UUID_AUTO_GENERATE = "UUID_AUTO_GENERATE"
    static final String START_DATE_AUTO_GENERATE = "START_DATE_AUTO_GENERATE"
    static final String END_DATE_AUTO_GENERATE = "END_DATE_AUTO_GENERATE"
    public static String AUTO_GEN_START_DATE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:00'Z'"
    public static String AUTO_GEN_END_DATE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:59'Z'"
    static final String TEST_METHOD_ALL = "ALL"
    static final String GENERATE_TOKEN = "generate_token->"

    //Supported data types
    static final List DATA_TYPES = ["String", "Integer", "Float", "Double", "Object"]

    static final String UTF8_BOM = "\uFEFF"

    static final String EXPONENTIAL_REGEX = "([0-9]+)\\.([0-9]+)E(\\+|-)?([0-9]+)"
    static String EQUAL = "="
}
