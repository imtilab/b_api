package com.imtilab.bittracer.test.validationv2

import com.jayway.jsonpath.JsonPath
import com.imtilab.bittracer.antlr.expression.ValidationParser
import com.imtilab.bittracer.constant.Constants
import com.imtilab.bittracer.model.DataType
import com.imtilab.bittracer.utils.DateUtil
import net.minidev.json.JSONArray
import org.apache.commons.collections.comparators.ComparatorChain
import org.apache.commons.lang3.StringUtils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ValidationVisitorHelper {
    static def convertTypeAll(def value, int type) {
        if (value instanceof Collection) {
            List result = []
            value.each {
                result.add(convertType(it, type))
            }
            value = result
        } else {
            value = convertType(value, type)
        }
        value
    }

    static def convertType(def value, int type) {
        if(value == null){
            return null
        }
        switch (type) {
            case ValidationParser.TypeInteger:
                if (value instanceof Double || value instanceof Float) {
                    return (Integer) value
                } else {
                    return Integer.parseInt(value.toString())
                }
                break
            case ValidationParser.TypeString:
                return value.toString()
                break
            case ValidationParser.TypeBoolean:
                if (value.toString().equals("1")) return true
                else return Boolean.parseBoolean(value.toString())
                break
            case ValidationParser.TypeDouble:
                return Double.parseDouble(value.toString())
                break
            case ValidationParser.TypeFloat:
                return Float.parseFloat(value.toString())
                break
            case ValidationParser.TypeLong:
                if (value instanceof Double || value instanceof Float) {
                    return (Long) value
                } else {
                    return Long.parseLong(value.toString())
                }
                break
            default:
                new ValidationException("Unknown conversion type")
        }
    }

    /**
     * sort jsonArray based on properties
     * @param jsonArray
     * @param properties
     * @param sortType
     * @return
     */
    static void sort(JSONArray jsonArray, List properties, int sortType) {
        if (!jsonArray || jsonArray.size() <= 1) {
            return
        }
        switch (sortType) {
            case ValidationParser.Ascending:
                sortJsonObjectAsc(jsonArray, properties)
                break
            case ValidationParser.Decending:
                sortJsonObjectDesc(jsonArray, properties)
                break
        }
    }

    static void sortJsonObjectAsc(JSONArray list, List properties) {
        if (!(list.get(0) instanceof Map)) {
            Collections.sort(list)
        } else {
            Collections.sort(list, getChainComparator(properties))
        }
    }

    static void sortJsonObjectDesc(JSONArray list, List properties) {
        if (!(list.get(0) instanceof Map)) {
            Collections.sort(list, Comparator.reverseOrder())
        } else {
            Collections.sort(list, getChainComparator(properties, true))
        }
    }

    static ComparatorChain getChainComparator(List properties, boolean reversed = false) {
        ComparatorChain chain = new ComparatorChain()
        properties.each {
            chain.addComparator(new Comparator<Map>() {
                @Override
                int compare(Map first, Map second) {
                    def firstValue, secondValue
                    try {
                        firstValue = JsonPath.read(first, it as String)
                    } catch (Exception e) {
                    }
                    try {
                        secondValue = JsonPath.read(second, it as String)
                    } catch (Exception e) {
                    }
                    if (firstValue == null && secondValue == null) {
                        return 0
                    } else if (firstValue != null && secondValue == null) {
                        return reversed ? 1 : -1
                    } else if (firstValue == null && secondValue != null) {
                        return reversed ? -1 : 1
                    } else {
                        return firstValue.compareTo(secondValue)
                    }
                }
            }, reversed)
        }
        return chain
    }


    /**
     * Replace searchString with replacement
     * @param target
     * @param searchString
     * @param replacement
     * @param replaceType
     * @return
     */
    static String replace(String target, String searchString, String replacement, int replaceType){
        switch (replaceType){
            case ValidationParser.First:
                return StringUtils.replaceOnce(target, searchString, replacement)
            case ValidationParser.IgnoreCase:
                return StringUtils.replaceIgnoreCase(target,searchString,replacement)
            default:
                return StringUtils.replace(target, searchString, replacement)
        }
    }

    static LocalDateTime convertToTimeZone(String date, String pattern, String timeZone, String toPattern, String toTimeZone){
        LocalDateTime dateTime = DateUtil.getFormattedDateTime(date, pattern)
        LocalDateTime toDateTime = DateUtil.convertDateToZone(dateTime, timeZone, toTimeZone)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(toPattern)
        DateUtil.getFormattedDateTime(formatter.format(toDateTime), toPattern)
    }

    static boolean validateDataType(def data, DataType dataType){
        switch (dataType){
            case DataType.STRING:
                assert data instanceof String
                break
            case DataType.INTEGER:
                assert data instanceof Number
                assert data <= Integer.MAX_VALUE
                String checkValue = data as String
                assert checkValue.isInteger(), ["${Constants.NEW_LINE}Exception :: ${Constants.NEW_LINE}Expected : Integer ${Constants.NEW_LINE}Actual: NOT a integer number${Constants.NEW_LINE}"]
                break
            case DataType.LONG:
                assert data instanceof Number
                assert data <= Long.MAX_VALUE
                assert data == data.longValue()
                break
            case DataType.DOUBLE:
                assert data instanceof Number
                assert data <= Double.MAX_VALUE
                assert data == data.doubleValue()
                break
            case DataType.FLOAT:
                assert data instanceof Number
                assert data <= Float.MAX_VALUE
                String checkValue = data as String
                assert checkValue.isFloat(), ["${Constants.NEW_LINE}Exception :: ${Constants.NEW_LINE}Expected : Float ${Constants.NEW_LINE}Actual: NOT a fraction number${Constants.NEW_LINE}"]
                break
            case DataType.OBJECT:
                assert data instanceof Object
                break
        }
    }
}
