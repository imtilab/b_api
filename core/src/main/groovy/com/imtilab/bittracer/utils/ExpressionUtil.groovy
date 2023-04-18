package com.imtilab.bittracer.utils

import com.imtilab.bittracer.antlr.expression.ExpressionParser
import com.imtilab.bittracer.constant.Constants
import com.imtilab.bittracer.model.ValueStorage
import net.sf.json.JSONArray
import net.sf.json.JSONNull
import org.apache.commons.lang3.StringUtils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ExpressionUtil {
    static JSONDataUtils jsonDataUtils = new JSONDataUtils()
    /**
     * Resolve value for function
     * @param function
     * @param value
     * @return
     */
    static def evalFunction(ValueStorage valueStorage, String function, def values) {
        def value
        switch (function) {
            case "ROUND":
                try {
                    value = Math.round(values[0])
                } catch (IndexOutOfBoundsException v) {
                    throw new Exception(Constants.NUMBER_OF_ARGUMENT_NOT_MATCHED('ROUND_UP', 1))
                }
                break
            case "ROUND_UP":
                try {
                    value = Math.ceil(values[0])
                } catch (IndexOutOfBoundsException v) {
                    throw new Exception(Constants.NUMBER_OF_ARGUMENT_NOT_MATCHED('ROUND_UP', 1))
                }
                break;
            case "ROUND_DOWN":
                try {
                    value = Math.floor(values[0])
                } catch (IndexOutOfBoundsException v) {
                    throw new Exception(Constants.NUMBER_OF_ARGUMENT_NOT_MATCHED('ROUND_DOWN', 1))
                }
                break
            case "ALTER_VALUE":
                try {
                    if (values[0] == values[1]) {
                        value = values[2]
                    } else if (values[0] == values[2]) {
                        value = values[1]
                    } else {
                        value = values[0]
                    }
                } catch (IndexOutOfBoundsException v) {
                    throw new Exception(Constants.NUMBER_OF_ARGUMENT_NOT_MATCHED('ALTER_VALUE', 3))
                }
                break
            case "EVALFROM":
                try {
                    value = evalValue(valueStorage, values[0], values[1])
                } catch (IndexOutOfBoundsException v) {
                    throw new Exception(Constants.NUMBER_OF_ARGUMENT_NOT_MATCHED('EVALFROM', 2))
                }
                break
            case "TO_PRIMITIVE":
                try {
                    value = CommonUtils.convertToPrimitive(values[0])
                } catch (IndexOutOfBoundsException v) {
                    throw new Exception(Constants.NUMBER_OF_ARGUMENT_NOT_MATCHED('TO_PRIMITIVE', 1))
                }
                break
        /**
         * It takes a collection and convert all elements to primitive types
         */
            case "TO_PRIMITIVE_LIST":
                try {
                    if (values[0] instanceof Collection) {
                        value = []
                        values[0].each {
                            value.add(CommonUtils.convertToPrimitive(it))
                        }
                    } else {
                        throw new Exception(values[0] + "is not instance of list")
                    }
                } catch (IndexOutOfBoundsException v) {
                    throw new Exception(Constants.NUMBER_OF_ARGUMENT_NOT_MATCHED('TO_PRIMITIVE_LIST', 1))
                }
                break
        /**
         * This will take a series of arguments and return concat value of all
         */
            case "CONCAT":
                try {
                    value = ""
                    values.each {
                        value = value + it
                    }
                } catch (IndexOutOfBoundsException v) {
                    throw new Exception(Constants.NUMBER_OF_ARGUMENT_NOT_MATCHED('CONCAT', 1))
                }
                break
        /**
         * It will take three parameter.
         * 1st param: List to which we concat value
         * 2nd param: String the value to be concat
         * 3rd param: String It should be "START" or "END" indicates where to concat
         */
            case "CONCAT_WITH_LIST":
                try {
                    value = []
                    def list = values[0]
                    def text = values[1]
                    def indicator = values[2]
                    if (list instanceof Collection && (indicator.equals("START") || indicator.equals("END"))) {
                        boolean isStart = indicator.equals("START")
                        list.each {
                            if (isStart)
                                value.add(text + it)
                            else
                                value.add(it + text)
                        }
                    } else {

                    }
                } catch (IndexOutOfBoundsException v) {
                    throw new Exception(Constants.NUMBER_OF_ARGUMENT_NOT_MATCHED('CONCAT', 1))
                }
                break
        /**
         * Convert value to String
         */
            case "TO_STRING":
                try {
                    if (values[0] instanceof Collection) {
                        value = []
                        values[0].each {
                            value.add(it as String)
                        }
                    } else {
                        value = values[0] != null ? values[0] as String : null
                    }
                } catch (IndexOutOfBoundsException v) {
                    throw new Exception(Constants.NUMBER_OF_ARGUMENT_NOT_MATCHED('TO_STRING', 1))
                }
                break
        /**
         * Convert value to Double
         */
            case "TO_DOUBLE":
                try {
                    if (values[0] instanceof Collection) {
                        value = []
                        values[0].each {
                            value.add(it != null ? Double.parseDouble(it as String) : it)
                        }
                    } else {
                        value = values[0] != null ? Double.parseDouble(values[0] as String) : values[0]
                    }
                } catch (IndexOutOfBoundsException v) {
                    throw new Exception(Constants.NUMBER_OF_ARGUMENT_NOT_MATCHED('TO_STRING', 1))
                }
                break
        /**
         * Convert value to Long
         */
            case "TO_LONG":
                try {
                    if (values[0] instanceof Collection) {
                        value = []
                        values[0].each {
                            value.add(it != null ? Long.parseLong(it as String) : it)
                        }
                    } else {
                        value = values[0] != null ? Long.parseLong(values[0] as String) : values[0]
                    }
                } catch (IndexOutOfBoundsException v) {
                    throw new Exception(Constants.NUMBER_OF_ARGUMENT_NOT_MATCHED('TO_STRING', 1))
                }
                break
        /**
         * Convert value to Integer
         */
            case "TO_INT":
                try {
                    if (values[0] instanceof Collection) {
                        value = []
                        values[0].each {
                            value.add(it != null ? Integer.parseInt(it as String) : it)
                        }
                    } else {
                        value = values[0] != null ? Integer.parseInt(values[0] as String) : values[0]
                    }
                } catch (IndexOutOfBoundsException v) {
                    throw new Exception(Constants.NUMBER_OF_ARGUMENT_NOT_MATCHED('TO_STRING', 1))
                }
                break
        /**
         * Remove empty and null  from list
         */
            case "FILTER_EMPTY":
                try {
                    def list = evalValue(valueStorage, values[0], values[1])
                    if (list instanceof Collection) {
                        value = removeEmptyFromList(list)
                        value = removeEmptyFromList(list)
                    } else {
                        // filter object param
                        throw new Exception('Not support for object')
                    }
                } catch (IndexOutOfBoundsException v) {
                    throw new Exception(Constants.NUMBER_OF_ARGUMENT_NOT_MATCHED('FILTER_EMPTY', 1))
                }
                break
        /**
         * Remove empty and null  from list
         */
            case "FILTER_BY_PROPERTY":
                try {
                    List list = evalValue(valueStorage, values[0], values[1])
                    for (int i = 2; i < values.size(); i++) {
                        def val
                        String property, dataType, referValue
                        (property, dataType, referValue) = values[i].split(":")
                        val = CommonUtils.convertToType(referValue, dataType)
                        value = list.findAll {
                            obj ->
                                if (obj == null) {
                                    false
                                } else if (obj[property] == null && val != null) {
                                    false
                                } else {
                                    obj[property] == val
                                }
                        }
                    }
                } catch (IndexOutOfBoundsException v) {
                    throw new Exception(Constants.NUMBER_OF_ARGUMENT_NOT_MATCHED('FILTER_BY_PROPERTY', 3))
                }
                break
        /**
         * Convert date into a given time zone
         */
            case "DATE_CONVERT_TO_TIMEZONE":
                try {
                    def dateString = values[0]
                    String currentPattern = values[1]
                    String currentTimeZone = values[2]
                    String toPattern = values[3]
                    String toTimeZone = values[4]
                    if (dateString instanceof Collection) {
                        value = []
                        dateString.each {
                            value.add(formatAndConvertDateToTimeZone(it, currentPattern, currentTimeZone, toPattern, toTimeZone))
                        }
                    } else {
                        value = formatAndConvertDateToTimeZone(dateString, currentPattern, currentTimeZone, toPattern, toTimeZone)
                    }
                } catch (IndexOutOfBoundsException ex) {
                    throw new Exception(Constants.NUMBER_OF_ARGUMENT_NOT_MATCHED('DATE_CONVERT_TO_TIMEZONE', 5))
                }
                break

            case "REPLACE":
                try {
                    def val = values[1]
                    String searchText = values[2]
                    String replaceBy = values[3]
                    if (val instanceof Collection) {
                        value = val.stream().map{ x -> StringUtils.replace(x, searchText, replaceBy)}.collect()
                    } else {
                        value = StringUtils.replace(val, searchText, replaceBy)
                    }
                } catch (IndexOutOfBoundsException v) {
                    throw new Exception(Constants.NUMBER_OF_ARGUMENT_NOT_MATCHED('REPLACE', 2))
                }
                break

        }
        if (value instanceof JSONNull)
            null
        else
            value
    }

    /**
     *
     * @param valueForOperation
     * @param array
     * @param operation
     * @return
     */
    static def performOperationOnArray(def array, def valueForOperation, int operator) {
        def result = []
        array.each {
            result.add(performOperationOnPrimitive(it, valueForOperation, operator))
        }
        result
    }

    /**
     * Addition, Subtraction, Division and Multiplication of two operand
     * @param left
     * @param right
     * @param operation
     * @return
     */
    static def performOperationOnPrimitive(def left, def right, int operator) {
        if (left instanceof String) {
            left = CommonUtils.convertToPrimitive(left)
        }

        if (right instanceof String) {
            right = CommonUtils.convertToPrimitive(right)
        }

        switch (operator) {
            case ExpressionParser.PLUS:
                return left + right
            case ExpressionParser.MINUS:
                return left - right
            case ExpressionParser.MULTIPLY:
                return left * right
            case ExpressionParser.DIVIDE:
                if (right == 0)
                    throw new UnsupportedOperationException("Cannot divide by zero")
                return left / right
            default:
                throw new Exception("Operator not supported")
        }
    }

    static def evalValue(ValueStorage valueStorage, String from, String key) {
        def data = getDataObject(valueStorage, from)
        jsonDataUtils.getDataByKey(data, key)
    }

    static def getDataObject(ValueStorage valueStorage, String from) {
        def data = null
        switch (from) {
            case 'REQUEST':
                data = valueStorage.getRequestBody()
                break
            case 'RESPONSE':
                data = valueStorage.getResponseBody()
                break
            case 'EXPECTED':
                data = valueStorage.getExpectedBody()
                break
            case 'UPSTREAM':
                data = valueStorage.getUpstreamBody()
                break
        }
        data
    }

    static def evalValueAndFilterEmpty(def data , String key) {
        def value = data
        List keys = key.split("\\.")
        for (int i = 0; i < keys.size(); i++) {
            String it = keys.get(i)
            if (value instanceof Collection) {
                it = "[]" + it
            }
            value = jsonDataUtils.getDataByKey(value, it)
            if (value == null || value instanceof JSONNull) {
                break
            }
            if (value instanceof Collection) {
                JSONArray temp = []
                value.each { item ->
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
                value = temp
            }
        }
        if (value instanceof JSONNull) {
            null
        } else {
            value
        }
    }

    static removeEmptyFromList(List list) {
        List filteredList = []
        list.each {
            if (!(it == null || it instanceof JSONNull ||
                    (it instanceof String && it == "") ||
                    (it instanceof Collection && it.isEmpty()))) {
                filteredList.add(it)
            }
        }
        filteredList
    }

    static LocalDateTime formatAndConvertDateToTimeZone(String dateString, String currentPattern, String cuttentZone, String toPattern, String toZone) {
        LocalDateTime dateTime = DateUtil.getFormattedDateTime(dateString, currentPattern)
        LocalDateTime toDateTime = DateUtil.convertDateToZone(dateTime, cuttentZone, toZone)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(toPattern)
        DateUtil.getFormattedDateTime(formatter.format(toDateTime), toPattern)
    }
}
