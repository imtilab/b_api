package com.imtilab.bittracer.utils

import com.imtilab.bittracer.constant.AdvancedOperator
import com.imtilab.bittracer.test.validation.RuleValidation
import net.sf.json.JSONArray
import net.sf.json.JSONObject
import org.apache.commons.collections.CollectionUtils

/**
 * Collection utils for JSONObject
 * Used Classes are  JSONDataUtils.groovy, AdvancedOperator.groovy
 */
class JSONCollection {

    private static JSONDataUtils jsonDataUtils = new JSONDataUtils()

    /**
     * Sort a list on second key
     *
     * @param list : Sorted by first key
     * @param firstKey
     * @param secondKey
     * @param operator - ACS or DESC
     * @return list sorted by first then second key
     */
    static private List getSubSortedList(List list, String firstKey, String secondKey, AdvancedOperator operator) {
        def firstObj = list.get(0)
        JSONArray subSortedList = []
        JSONArray subList = []
        subList.add(firstObj)

        for (int i = 1; i < list.size(); i++) {
            def secondObj = list.get(i)
            if (!(jsonDataUtils.getDataByKey(firstObj, firstKey)).equals(jsonDataUtils.getDataByKey(secondObj, firstKey))) {
                if (subList.size() > 1) {
                    subSortedList.addAll(getSortedList(subList, secondKey, operator))
                } else {
                    subSortedList.add(subList.get(0))
                }
                subList.clear()
            }
            firstObj = secondObj
            subList.add(firstObj)
        }
        /*Last set set of subset*/
        if (subList.size() > 1) {
            subSortedList.addAll(getSortedList(subList, secondKey, operator))
        } else {
            subSortedList.add(subList.get(0))
        }
        subSortedList
    }

    /**
     * Sort a list by multiple keys
     *
     * @param list
     * @param sortKeys
     * @param operator
     * @return sorted list
     */
    static List getSortedList(List list, List sortKeys, AdvancedOperator operator) {
        String firstKey = sortKeys.pop()
        firstKey = firstKey.trim()

        List result = getSortedList(list, firstKey, operator)

        for (int i = 0; i < sortKeys.size(); i++) {
            String secondKey = sortKeys.get(i)
            secondKey = secondKey.trim()
            result = getSubSortedList(result, firstKey, secondKey, operator)
            firstKey = secondKey
        }
        result
    }


    /**
     * Sort by single Key
     *
     * @param list must be sorted by first key
     * @param Key
     * @param operator ACS or DESC
     * @return sorted list
     */
    static List getSortedList(List list, String key, AdvancedOperator operator) {
        List source =  list.collect()
        List sortValues
        if (key.contains("##DATE_FORMAT#")) {
            String format
            (key, format) = key.split("##DATE_FORMAT#")
            sortValues = CollectionUtils.isNotEmpty(list) ? jsonDataUtils.getDataByKey(list, "[]." + key) : list
            sortValues = DateUtil.getDateByFormat(sortValues, format)
            sortValues = getSortedList(sortValues, operator)
            sortValues = DateUtil.getDate(sortValues, format)
        } else {
            sortValues = CollectionUtils.isNotEmpty(list) ? jsonDataUtils.getDataByKey(list, "[]." + key) : list
            sortValues = getSortedList(sortValues, operator)
        }

        if (list.size() != sortValues.size()) {
            throw new Exception("Sort key is not available at all element of list")
        }

        JSONArray sortedList = []
        /*Sort object list according to sorted values of given key*/
        for (int i = 0; i < sortValues.size(); i++) {
            for (int j = 0; j < source.size(); j++) {
                if ((sortValues.get(i)).equals(jsonDataUtils.getDataByKey(source.get(j), key))) {
                    sortedList.add(source.get(j))
                    source.remove(j)
                    break
                }
            }
        }
        sortedList
    }

    /**
     * Sort List (Core)
     *
     * @param list : List of Integer, float, double, String, Date, DateTime
     * @param operator
     * @return sorted list
     */
    static List getSortedList(List list, AdvancedOperator operator) {
        if (CollectionUtils.isEmpty(list) || (list.get(0) instanceof JSONObject || list.get(0) instanceof JSONArray)) {
            throw new Exception("NOT a comparable object : $list")
        }
        List result = list.collect()
        if (operator.equals(AdvancedOperator.SORT_ASC)) {
            Collections.sort(result)
        } else if (operator.equals(AdvancedOperator.SORT_DESC)) {
            Collections.sort(result, Collections.reverseOrder())
        } else {
            throw new Exception("Invalid operator for sorting : $operator")
        }
        result
    }


}
