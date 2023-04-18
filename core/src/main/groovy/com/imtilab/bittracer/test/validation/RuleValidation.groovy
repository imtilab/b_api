package com.imtilab.bittracer.test.validation


import groovy.util.logging.Slf4j
import com.imtilab.bittracer.constant.AdvancedOperator
import com.imtilab.bittracer.constant.Constants
import com.imtilab.bittracer.constant.PropertiesKey
import com.imtilab.bittracer.model.MetaData
import com.imtilab.bittracer.model.ValueStorage
import com.imtilab.bittracer.utils.CommonUtils
import com.imtilab.bittracer.utils.ConfigurationResource
import com.imtilab.bittracer.utils.DateUtil
import com.imtilab.bittracer.utils.ExpressionEvaluation
import com.imtilab.bittracer.utils.JSONCollection
import com.imtilab.bittracer.utils.JSONDataUtils
import net.sf.json.JSONArray
import net.sf.json.JSONNull
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang.math.NumberUtils

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * For Advanced rule validation
 * Single rule and combination
 */
@Slf4j
class RuleValidation {

    JSONDataUtils jsonDataUtils
    def responseBody
    def expectedBody
    ValueStorage valueStorage

    String actualKeySet
    String expectedKeySet
    Map backendKeyMap = [:]
    boolean isErrorAnalysisEnable = Boolean.parseBoolean(ConfigurationResource.instance().getConfigProp(PropertiesKey.IS_ERROR_ANALYSIS_ENABLE))
    List errors = [] // TODO: will be remove

    /**
     * Set & validate actual and expected JSONBody
     *
     * @param responseBody
     * @param expectedBody
     */
    private void validateResponseBody(def responseBody, def expectedBody) {
        if (responseBody == null) {
            throw new Exception("Invalid response body(Actual value) $responseBody")
        }
        if (expectedBody == null) {
            throw new Exception("Invalid expected body $expectedBody")
        }
        this.responseBody = responseBody
        this.expectedBody = expectedBody
    }

    /**
     *  Rule validation
     *
     * @param responseBody in def
     * @param expectedBody in def
     * @param ruleData : single or multiple rule
     */
    void validateRules(def responseBody, def expectedBody, def ruleData, Map backendKeyMap = [:], ValueStorage valueStorage) {
        this.valueStorage = valueStorage
        if (StringUtils.isNotEmpty(ruleData)) {
            validateResponseBody(responseBody, expectedBody)
            this.backendKeyMap = backendKeyMap
            jsonDataUtils = new JSONDataUtils()
            List rules = ruleData.split(Constants.NEW_LINE)
            rules.each { rule ->
                if (!StringUtils.isBlank(rule)) {
                    rule = rule.trim()
                    // if(!isIgnorableRule(rule,metaData)){
                    try {
                        prepareRuleValidation(rule)
                    } catch (Exception error) {
                        if (isIgnorableRule(rule)) {
                            log.info("Assertion ignored: " + rule)
                        } else {
                            log.info("Assertion failed: " + rule)
                            errors.add(error)
                        }
                    }
                    //}
                }
            }
            assert errors.size() == 0 // TODO: will be remove
            errors.clear() // TODO: will be remove
        }
    }

    boolean isIgnorableRule(String rule) {
        boolean isIgnore = false
        for (String ignoreParam : MetaData.ignoreTests) {
            if (rule.startsWith(ignoreParam + "#") || rule.startsWith(ignoreParam + ".")) {
                isIgnore = true
                break
            }
        }
        isIgnore
    }

    /**
     * Explicit combine validation and separate date and other validation
     *
     * @param rule
     */
    private void prepareRuleValidation(String rule) {
        log.info("${Constants.NEW_LINE}Rule: $rule")
        String actual
        List validations
        validations = rule.split(Constants.GET_SPLITTER_REGX(Constants.KEY_SPLITTER))
        actual = validations.pop()
        if (CollectionUtils.isEmpty(validations)) {
            throw new Exception("Invalid Rule")
        }
        actualKeySet = actual
        validations.each {
            log.debug("Validation: ${actual}${Constants.KEY_SPLITTER}${it}")
            String operatorSt, expected
            (operatorSt) = it.split(Constants.GET_SPLITTER_REGX(Constants.VALUE_SPLITTER) + "|#|" + Constants.GET_SPLITTER_REGX("->"))
            expected = it - operatorSt
            AdvancedOperator operator
            try {
                operator = AdvancedOperator.valueOf(operatorSt)
            } catch (Exception ex) {
                throw new Exception("Invalid Rule (Syntax not valid near ${operatorSt})")
            }
            validateKey(operator, actual, expected)
        }
    }

    /**
     * Key validation block. other than pass
     *
     * @param operator
     * @param key
     * @param expected
     */
    private void validateKey(AdvancedOperator operator, String key, String expected) {
        log.debug("validateKey- Operator: $operator, Key: $key, Expected Expression: $expected")
        boolean isAsserted = true
        if (isErrorAnalysisEnable) {
            try {
                isAsserted = validateKeyExist(operator, key)
            } catch (AssertionError error) {
                errors.add(error)
            }
        } else {
            try {
                isAsserted = validateKeyExist(operator, key)
            } catch (AssertionError error) {
                throw new Exception(error)
            }
        }
        if (!isAsserted) {
            def actualValue = getValue(responseBody, key)
            /*For no expected,  expected will same as actual key */
            if (StringUtils.isEmpty(expected) && !operator.toString().matches("SORT_(A|DE)SC")) {
                expected = key
            } else if (key.contains("DATE_FORMAT") && expected.startsWith("DATE_FORMAT")) {
                expected = key.substring(0, key.indexOf("DATE_FORMAT")) + expected
            }
            try {
                validateList(operator, actualValue, expected)
            } catch (AssertionError error) {
                String rule = key
                if (isIgnorableRule(rule + "#")) {
                    log.info("Assertion ignored: " + rule)
                } else {
                    log.info("Assertion failed: " + rule)
                    errors.add(error)
                }
            }
            //validateList(operator, actualValue, expected)
        }
    }

    boolean validateKeyExist(AdvancedOperator operator, String key) {
        boolean isAsserted = true
        switch (operator) {
            case AdvancedOperator.EXIST_KEY:
                assert jsonDataUtils.isValidKey(this.responseBody, key)
                break
            case AdvancedOperator.NOT_EXIST_KEY:
                assert !jsonDataUtils.isValidKey(this.responseBody, key)
                break
            case AdvancedOperator.EXIST_KEY_IN_ANY:
                def res = this.responseBody
                def keyList = key.split("\\.")
                keyList.eachWithIndex { it, index ->
                    if (res instanceof Collection) {
                        it = "[]" + it
                    }
                    res = jsonDataUtils.getDataByKey(res, it)
                    if (res instanceof Collection && index < keyList.length - 1) {
                        JSONArray temp = []
                        res.each { item ->
                            if (item instanceof Collection) {
                                item.each {
                                    if (!(it == null || it instanceof JSONNull)) {
                                        temp.add(it)
                                    }
                                }
                            } else if (!(item == null || item instanceof JSONNull)) {
                                temp.add(item)
                            }
                        }
                        res = temp
                    }
                }
                if (res == null || !(res instanceof Collection) || res.isEmpty()) {
                    assert jsonDataUtils.isValidKey(this.responseBody, key)
                } else {
                    assert res.size() > 0
                }
                break
            default:
                isAsserted = false
        }
        isAsserted
    }

    /**
     * List operation block
     *
     * @param operator
     * @param actual
     * @param expected
     */
    private void validateList(AdvancedOperator operator, def actualValue, String expected) {
        log.debug("validateList- Operator: $operator, Actual Value: $actualValue, Expected Expression: $expected")
        switch (operator) {
            case AdvancedOperator.EMPTY:
                if (!(actualValue instanceof Collection)) {
                    throw new Exception("NOT A VALID LIST : $actualValue")
                }
                assert actualValue.size() == NumberUtils.INTEGER_ZERO
                break
            case AdvancedOperator.NOT_EMPTY:
                if (!(actualValue instanceof Collection)) {
                    throw new Exception("NOT A VALID LIST : $actualValue")
                }
                assert actualValue.size() != NumberUtils.INTEGER_ZERO
                break
            case AdvancedOperator.SORT_ASC:
            case AdvancedOperator.SORT_DESC:
                if (!(actualValue instanceof Collection)) {
                    throw new Exception("NOT A VALID LIST : $actualValue")
                }

                List sortedList

                /*To identify sort by part. Ex #id, name, dob##DATE_FORMAT#yyyy-MM-ddHH:mm:ss*/
                if ((expected.startsWith('#'))) {
                    expected = expected.substring(1)
                    List keys = expected.split(',')
                    sortedList = JSONCollection.getSortedList(actualValue, keys, operator)
                } else {
                    sortedList = JSONCollection.getSortedList(actualValue, operator)
                }
                assert actualValue.collect() == sortedList
                break
            case AdvancedOperator.NOT_CONTAINS:
                def expectedValue = getValue(expectedBody, expected)
                if (!(expectedValue instanceof Collection && actualValue instanceof Collection)) {
                    throw new Exception("NOT_CONTAINS operator only support for list.")
                }
                int sizeOfExpected = expectedValue.size()
                int difference = (expectedValue - actualValue).size()
                assert sizeOfExpected == difference
                break
            case AdvancedOperator.CONTAINS_ANY:
                def expectedValue = getValue(expectedBody, expected)
                if (!(actualValue instanceof Collection)) {
                    actualValue = [actualValue]
                }
                if (!(expectedValue instanceof Collection)) {
                    throw new Exception("CONTAINS_ANY operator only support for list.")
                }
                assert expectedValue.containsAll(actualValue)
                break
            case AdvancedOperator.CONTAINS_ALL:
                def expectedValue = getValue(expectedBody, expected)
                if (!(expectedValue instanceof Collection)) {
                    List valueAsList = [expectedValue];
                    assert actualValue.containsAll(valueAsList)
                    break
                }
                if (!(actualValue instanceof Collection)) {
                    throw new Exception("CONTAINS_ALL operator only support for list.")
                }
                if (CollectionUtils.isEmpty(expectedValue) && CollectionUtils.isNotEmpty(actualValue)) {
                    assert CollectionUtils.isNotEmpty(expectedValue) && expectedValue.containsAll(actualValue)
                } else {
                    assert actualValue.containsAll(expectedValue)
                }
                break
            default:
                def expectedValue = getValue(expectedBody, expected)

                String matchingKey = expectedKeySet
                if (!backendKeyMap.isEmpty()) {
                    matchingKey = backendKeyMap.get(expectedKeySet)
                }

                if (actualKeySet.equals(matchingKey) && actualValue.getClass() != expectedValue.getClass()) {
                    throw new Exception("Same keys holding different type values")
                }
                validation(operator, actualValue, expectedValue)
        }
    }

    /**
     * Process list of validation and single validation,
     *
     * @param operator
     * @param actualValue
     * @param expectedValue
     */
    private void validation(AdvancedOperator operator, def actualValue, def expectedValue) {
        log.debug("validation- Operator: $operator, Actual value: $actualValue, Expected value: $expectedValue")
        if (operator.toString().equals(AdvancedOperator.DATA_TYPE.toString()) && expectedValue.equals(Constants.DATA_TYPE_LIST)) {
            ruleValidate(operator, actualValue, expectedValue)
        } else if (actualValue instanceof Collection) {
            if (expectedValue instanceof Collection) {
                String currentOperator = operator.toString()
                if (currentOperator.contains(AdvancedOperator.EQUAL.toString())) {
                    ruleValidate(operator, actualValue, expectedValue)
                } else {
                    throw new Exception("$operator is not supported for Two list compare")
                }
            } else {
                // actualValue list is empty, so each elem not equal to expecedValue
                if (actualValue.size() == 0) {
                    assert actualValue == expectedValue
                }
                actualValue.each { it ->
                    ruleValidate(operator, it, expectedValue)
                }
            }
        } else if (!(expectedValue instanceof Collection)) {
            ruleValidate(operator, actualValue, expectedValue)
        } else {
            throw new Exception("Unsupported validation, where actual value is non-collection but expected value")
        }
    }

    private void ruleValidate(AdvancedOperator operator, def actualValue, def expectedValue) {
        log.debug("ruleValidate- Operator: $operator, Actual value: $actualValue, Expected value: $expectedValue")
        if (actualValue instanceof Collection && expectedValue instanceof Collection) {
            actualValue = new ArrayList(actualValue)
            expectedValue = new ArrayList(expectedValue)
        }
        switch (operator) {
            case AdvancedOperator.EQUAL:
            case AdvancedOperator.DATE_EQUAL:
                assert actualValue == expectedValue
                break
            case AdvancedOperator.NOT_EQUAL:
            case AdvancedOperator.DATE_NOT_EQUAL:
                assert actualValue != expectedValue
                break
            case AdvancedOperator.GREATER_THAN:
                assert actualValue > expectedValue
                break
            case AdvancedOperator.GREATER_THAN_EQUAL:
                assert actualValue >= expectedValue
                break
            case AdvancedOperator.LESS_THAN:
                assert actualValue < expectedValue
                break
            case AdvancedOperator.LESS_THAN_EQUAL:
                assert actualValue <= expectedValue
                break
            case AdvancedOperator.BETWEEN:
                def (lowerLimit, upperLimit) = expectedValue.split(Constants.GET_SPLITTER_REGX(":"))
                lowerLimit = CommonUtils.convertToPrimitive(lowerLimit)
                upperLimit = CommonUtils.convertToPrimitive(upperLimit)
                assert actualValue >= lowerLimit && actualValue <= upperLimit
                break
            case AdvancedOperator.CONTAIN:
                assert actualValue.contains(expectedValue)
                break
            case AdvancedOperator.NOT_CONTAIN:
                assert !actualValue.contains(expectedValue)
                break
            case AdvancedOperator.PATTERN:
                assert actualValue.matches(expectedValue)
                break
            case AdvancedOperator.DATE_TIME_PATTERN:
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(expectedValue)
                try {
                    assert LocalDateTime.parse(actualValue, formatter)
                } catch (Exception ex) {
                    throw new Exception("${actualValue} can't be formated as ${expectedValue}")
                }
                break
            case AdvancedOperator.DATE_PATTERN:
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(expectedValue)
                try {
                    assert LocalDate.parse(actualValue, formatter)
                } catch (Exception ex) {
                    throw new Exception("${actualValue} can't be formated as ${expectedValue}")
                }
                break
            case AdvancedOperator.TIME_PATTERN:
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(expectedValue)
                try {
                    assert LocalTime.parse(actualValue, formatter)
                } catch (Exception ex) {
                    throw new Exception("${actualValue} can't be formated as ${expectedValue}")
                }
                break
            case AdvancedOperator.START_WITH:
                assert actualValue.startsWith(expectedValue)
                break
            case AdvancedOperator.ENDS_WITH:
                assert actualValue.endsWith(expectedValue)
                break
            case AdvancedOperator.DATA_TYPE:
                if (expectedValue.startsWith(Constants.DATA_TYPE_OF_ARRAY)) {
                    String dataType = expectedValue - Constants.DATA_TYPE_OF_ARRAY
                    validateArrayDataType(actualValue, dataType)
                } else {
                    validateDataType(actualValue, expectedValue)
                }
                break
            case AdvancedOperator.DATE_GREATER_THAN:
                assert actualValue.isAfter(expectedValue)
                break
            case AdvancedOperator.DATE_GREATER_THAN_EQUAL:
                assert (actualValue.equals(expectedValue) || actualValue.isAfter(expectedValue))
                break
            case AdvancedOperator.DATE_LESS_THAN:
                assert actualValue.isBefore(expectedValue)
                break
            case AdvancedOperator.DATE_LESS_THAN_EQUAL:
                assert (actualValue.equals(expectedValue) || actualValue.isBefore(expectedValue))
                break
            default:
                throw new Exception("${operator} NOT SUPPORTED YET !!!")
        }
    }

    private void validateArrayDataType(def actualValue, String expectedValue) {
        if (actualValue instanceof Collection) {
            actualValue = removeEmptyFromList(actualValue)
            actualValue.each {
                validateArrayDataType(it, expectedValue)
            }
        } else {
            validateDataType(actualValue, expectedValue)
        }
    }

    private void validateDataType(def actualValue, String expectedValue) {
        if (expectedValue.equalsIgnoreCase(Double.class.getSimpleName())) {
            String checkValue = actualValue as String
            assert checkValue.isDouble(), ["${Constants.NEW_LINE}Exception :: ${Constants.NEW_LINE}Expected : Double ${Constants.NEW_LINE}Actual: NOT a fraction number${Constants.NEW_LINE}"]
        } else if (expectedValue.equalsIgnoreCase(Float.class.getSimpleName())) {
            String checkValue = actualValue as String
            assert checkValue.isFloat(), ["${Constants.NEW_LINE}Exception :: ${Constants.NEW_LINE}Expected : Float ${Constants.NEW_LINE}Actual: NOT a fraction number${Constants.NEW_LINE}"]
        } else if (expectedValue.equalsIgnoreCase(Integer.class.getSimpleName())) {
            String checkValue = actualValue as String
            assert checkValue.isInteger(), ["${Constants.NEW_LINE}Exception :: ${Constants.NEW_LINE}Expected : Integer ${Constants.NEW_LINE}Actual: NOT a integer number${Constants.NEW_LINE}"]
        } else if (expectedValue.equalsIgnoreCase(Long.class.getSimpleName())) {
            String checkValue = actualValue as String
            assert checkValue.isLong(), ["${Constants.NEW_LINE}Exception :: ${Constants.NEW_LINE}Expected : Long ${Constants.NEW_LINE}Actual: NOT a long number${Constants.NEW_LINE}"]
        } else if (expectedValue.equalsIgnoreCase(Constants.DATA_TYPE_NUMBER)) {
            String checkValue = actualValue as String
            assert checkValue.isNumber(), ["${Constants.NEW_LINE}Exception :: ${Constants.NEW_LINE}Expected : Number ${Constants.NEW_LINE}Actual: NOT a number${Constants.NEW_LINE}"]
        } else if (expectedValue.equalsIgnoreCase(Constants.DATA_TYPE_OBJECT)) {
            actualValue = actualValue.getClass().getName().toString()
            assert actualValue.equals(Constants.JSON_OBJECT_CLASS)
        } else if (expectedValue.equalsIgnoreCase(Constants.DATA_TYPE_LIST)) {
            assert actualValue instanceof Collection
        } else if (expectedValue.equalsIgnoreCase(Constants.DATA_TYPE_DECIMAL)) {
            assert actualValue instanceof Number
        } else {
            expectedValue = "java.lang.$expectedValue"
            actualValue = actualValue.getClass().getName().toString()
            expectedValue = expectedValue.toString()
            assert actualValue.equalsIgnoreCase(expectedValue)
        }
    }

    /**
     * Get value from def
     * Search, Size are applicable for list
     *
     * @param body in def
     * @param expression
     * @return value according to provided key
     */
    private def getValue(def body, String expression) {
        def result
        /*for hard coded value*/
        if (expression.startsWith("--")) {
            def value = expression.substring(2, expression.length())
            if (value.contains("##DATE_FORMAT#")) {
                String date, format
                (date, format) = value.split("##DATE_FORMAT#")
                result = DateUtil.getFormattedDateTime(date, format)
            } else {
                if (value instanceof String && value.equalsIgnoreCase(Constants.CURRENT_DATETIME)) {
                    result = LocalDateTime.now()
                } else {
                    result = ExpressionEvaluation.evaluate(body, value, valueStorage)
                }
            }
            expectedKeySet = null
        } else { /*for key*/
            if (expression.startsWith("->")) { /*for different key*/
                expression = expression.substring(2, expression.length())
            }
            expectedKeySet = expression

            /* ##(Data operator) for FIND_BY, SIZE, DATE_FORMAT, LENGTH operation */
            if (expression.contains("##")) {
                String listKeys
                List opFindSize = expression.split(Constants.GET_SPLITTER_REGX("##"))
                listKeys = opFindSize.pop()
                def source = ExpressionEvaluation.evaluate(body, listKeys, valueStorage)

                result = source
                opFindSize.each {
                    if (it.equals(AdvancedOperator.SIZE.toString())) {
                        if (!(source instanceof Collection)) {
                            throw new Exception("NOT A VALID LIST for $listKeys : $source")
                        }
                        result = result.size()
                    } else if (it.equals(AdvancedOperator.LENGTH.toString())) {
                        if (result instanceof Collection) {
                            List values = []
                            result.each { element ->
                                values.add(element == null ? null : element.toString().length())
                            }
                            result = values
                        } else {
                            result = result == null ? null : result.toString().length()
                        }
                    } else if (it.startsWith("DATE_FORMAT#")) {
                        result = DateUtil.getDateByFormat(result, it.split("DATE_FORMAT#").getAt(1))
                    } else if (it.startsWith(AdvancedOperator.FIND_BY.toString())) {
                        result = findBy(result, it)
                    } else {
                        throwInvalidExpressionSyntax("getValue- invalid ## operation, :$it")
                    }
                }
            } else {
                result = ExpressionEvaluation.evaluate(body, expression, valueStorage)
            }
        }
        return result
    }

    /**
     * Filter a list
     *
     * @param data : list
     * @param searchBy : key paired data. paired by : and separated by ,
     * @return filtered data
     */
    private List findBy(def data, String searchBy) {

        if (CollectionUtils.isEmpty(data)) {
            throw new Exception("No data found for $searchBy : $data")
        }

        String searchPart, getPart
        if (searchBy.contains('#')) {
            try {
                (searchPart, getPart) = searchBy.split(Constants.GET_SPLITTER_REGX('#'))
            } catch (Exception ex) {
                throwInvalidExpressionSyntax(searchBy)
            }
        } else {
            searchPart = searchBy
        }

        List searchIteams = searchPart.substring(searchBy.indexOf('[') + 1, searchBy.indexOf(']')).split(Constants.GET_SPLITTER_REGX(Constants.COMMA))
        data = CommonUtils.makeSingleList(data)
        searchIteams.each {
            searchIteam ->
                data = data.findAll {
                    datum ->
                        String key, value
                        try {
                            (key, value) = searchIteam.split(':', 2)
                        } catch (Exception ex) {
                            throwInvalidExpressionSyntax(searchBy)
                        }
                        def actualVal = ExpressionEvaluation.evaluate(datum, key, valueStorage)
                        actualVal == null ? false : actualVal == CommonUtils.convertToPrimitive(value)
                }
                log.debug("Find By: $searchIteam is $data")
        }

        /*Get expected value from list*/
        if (StringUtils.isNotEmpty(getPart)) {
            List getList = []
            data.each {
                if (jsonDataUtils.isValidKey(it, getPart)) {
                    def value = ExpressionEvaluation.evaluate(it, getPart, valueStorage)
                    if (value instanceof Collection) {
                        getList.addAll(value)
                    } else {
                        getList.add(value)
                    }
                }
            }
            data = getList
            log.debug("Get From list : $getPart is $data")
        }
        return data
    }

    private void throwInvalidExpressionSyntax(String expression) {
        throw new Exception("Syntax error near :" + expression)
    }

    private def removeEmptyFromList(def list) {
        def filterResult = []
        list.each {
            if (it != null) {
                filterResult.add(it)
            }
        }
        filterResult
    }

}