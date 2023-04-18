package com.imtilab.bittracer.utils


import com.imtilab.bittracer.constant.Constants
import com.imtilab.bittracer.exceptions.InvalidKeyException
import com.imtilab.bittracer.exceptions.JSONFormatException
import net.sf.json.JSONArray
import net.sf.json.JSONNull
import net.sf.json.JSONObject
import org.apache.commons.lang.ArrayUtils
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang.math.NumberUtils

/**
 * json data comparison
 * json data search by key
 */
class JSONDataUtils {
    private static Integer ONE = NumberUtils.INTEGER_ONE
    private static Integer TWO = new Integer(2)
    private static Integer ZERO = NumberUtils.INTEGER_ZERO

    // to store current traverse key
    private keyStack
    // to store all result data while traversing json data by key
    private def resultDataList
    private boolean isListOfResultData

    /**
     * Check if key is valid for jsonData;
     * If valid then return true or false instead.
     * @param jsonData
     * @param key
     * @return boolean
     */
    boolean isValidKey(def jsonData, String key) {
        if (!isObject(jsonData) && !isArray(jsonData)) {
            throw new MissingMethodException(new Object() {
            }.getClass().getEnclosingMethod().getName(), this.getClass(), jsonData, key)
        }
        def keys = prepareKeys(key)
        if (keys.size() == ZERO) {
            return isArray(jsonData)
        }
        return validateKey(jsonData, keys, ZERO)
    }

    /**
     * Recursively traverse jsonData by keys;
     * If key is valid then return true or false instead.
     * @param data
     * @param keys
     * @param keyIndex
     * @return boolean
     */
    private boolean validateKey(def data, String[] keys, int keyIndex) {
        // no more keys
        if (keyIndex >= keys.size()) {
            return true
        }
        def key = keys[keyIndex]
        // data type Object
        if (isObject(data)) {
            // key-value exist
            if (data.containsKey(key)) {
                return validateKey(data.get(key), keys, ++keyIndex)
            } else {
                return false
            }
            //data type Array
        } else if (isArray(data)) {
            // key format: []
            if (key.equals(StringUtils.EMPTY)) {
                keyIndex++
                if (keyIndex >= keys.size()) {
                    return false
                }
                key = keys[keyIndex]
                def innerIndex = ++keyIndex
                def innerCounter = ZERO
                for (item in data) {
                    if (isObject(item) && (item.containsKey(key))) {
                        def innerResult = validateKey(item.get(key), keys, innerIndex)
                        if (innerResult) {
                            innerCounter++
                        }
                    }
                }
                // all items in array are valid
                if (innerCounter == data.size() && innerCounter != 0) {
                    return true
                } else {
                    return false
                }
                // key format: [index]
            } else if (key.toString().isNumber()) {
                key = key as int
                if (key >= ZERO && key < data.size()) {
                    return validateKey(data.get(key), keys, ++keyIndex)
                } else {
                    return false
                }
            } else {
                return false
            }
        } else {
            return false
        }
    }

    /**
     * Search json formatted data by key;
     * and return value if found or null instead.
     *
     * @param jsonData
     * @param key
     * @return data
     */
    def getDataByKey(def jsonData, String key) {
        if (!isObject(jsonData) && !isArray(jsonData)) {
            throw new MissingMethodException(new Object() {
            }.getClass().getEnclosingMethod().getName(), this.getClass(), jsonData, key)
        }
        def keys = prepareKeys(key)
        if (keys.size() == ZERO) {
            if (isArray(jsonData)) {
                return jsonData
            } else {
                throw new InvalidKeyException("Key $key not found in JSONObject.")
            }
        }
        resultDataList = []
        isListOfResultData = false
        // multi-dimensional array to store result data
        def result = traverse(jsonData, keys, ZERO)
        if (isListOfResultData) {
            if (resultDataList.size() > ZERO) {
                return resultDataList
            } else {
                throw new InvalidKeyException("Key '${generateKeyFromList(keys)}' not found in JSONObject.")
            }
        } else {
            return result
        }
    }

    /**
     * Recursively search json formatted data by keys;
     * If found then return value else return null.
     *
     * @param data
     * @param keys
     * @param keyIndex
     * @return searched data or null
     */
    private def traverse(def data, String[] keys, int keyIndex) {
        // no more keys
        if (keyIndex >= keys.size()) {
            if(data instanceof JSONNull)
                data = null
            if (isListOfResultData) {
                resultDataList.add(data)
            }
            return data
        }
        def key = keys[keyIndex]
        // data type Object
        if (isObject(data)) {
            // key-value exist
            if (data.containsKey(key)) {
                return traverse(data.get(key), keys, ++keyIndex)
            } else {
                throw new InvalidKeyException("Key '${generateKeyFromList(keys)}' not found in JSONObject.")
            }
            //data type Array
        } else if (isArray(data)) {
            // key format: []
            if (key.equals(StringUtils.EMPTY)) {
                keyIndex++
                if (keyIndex >= keys.size()) {
                    throw new IndexOutOfBoundsException()
                }
                key = keys[keyIndex]
                // key format array[].key
                isListOfResultData = true
                def innerIndex = ++keyIndex
                for (item in data) {
                    if (isObject(item) && (item.containsKey(key))) {
                        traverse(item.get(key), keys, innerIndex)
                    }
                }
                // key format: [index]
            } else if (key.toString().isNumber()) {
                key = key as int
                if (key >= ZERO && key < data.size()) {
                    return traverse(data.get(key), keys, ++keyIndex)
                } else {
                    throw new IndexOutOfBoundsException("Key: '${generateKeyFromList(keys)}', Index: $key, Size: ${data.size()}.")
                }
            } else {
                throw new InvalidKeyException("Key '${generateKeyFromList(keys)}' not found in JSONObject.")
            }
        } else {
            throw new InvalidKeyException("Key '${generateKeyFromList(keys)}' not found in JSONObject.")
        }
    }

    /**
     * Search json formatted data by key;
     * replace all the key-values with 'replacements'
     * and return JsonObject data.
     * For unusual cases throws exceptions.
     *
     * @param jsonData
     * @param key
     * @param replacements
     * @return JSONObject data
     */
    def replaceDataByKey(def jsonData, String key, def replacement) {
        if (!isObject(jsonData) && !isArray(jsonData)) {
            throw new MissingMethodException(new Object() {
            }.getClass().getEnclosingMethod().getName(), this.getClass(), jsonData, key, replacement)
        }
        def keys = prepareKeys(key)
        if (keys.size() == ZERO) {
            if (isArray(jsonData)) {
                return replacement
            } else {
                throw new InvalidKeyException("Key $key not found in JSONObject.")
            }
        }
        def jsonDataCopy
        if (isArray(jsonData)) {
            jsonDataCopy = JSONArray.fromObject(jsonData.toString())
        } else {
            jsonDataCopy = JSONObject.fromObject(jsonData.toString())
        }
        return traverseAndReplace(jsonDataCopy, keys, ZERO, replacement)
    }

    /**
     * Recursively search json formatted data by keys;
     * If found then replace the key-value with 'replacement' and return data
     * Else throws exceptions
     *
     * @param data
     * @param keys
     * @param keyIndex
     * @param replacement
     * @return data
     */
    private def traverseAndReplace(def data, String[] keys, int keyIndex, def replacement) {
        // no more keys
        if (keyIndex >= keys.size()) {
            // replace 'data' with 'replacement' and return
            return replacement
        }
        def key = keys[keyIndex]
        // data type Object
        if (isObject(data)) {
            // key-value exist
            if (data.containsKey(key)) {
                def valueToReplace = traverseAndReplace(data.get(key), keys, ++keyIndex, replacement)
                if (valueToReplace == null)
                    data[key] = JSONObject.fromObject(null)
                else
                    data[key] = valueToReplace
                return data
            } else {
                throw new InvalidKeyException("Key '${generateKeyFromList(keys)}' not found in JSONObject.")
            }
            //data type Array
        } else if (isArray(data)) {
            // key format: []
            if (key.equals(StringUtils.EMPTY)) {
                keyIndex++
                if (keyIndex >= keys.size()) {
                    throw new IndexOutOfBoundsException()
                }
                key = keys[keyIndex]
                // key format array[].key
                def innerIndex = ++keyIndex
                def innerCounter = ZERO
                data.eachWithIndex { item, itemIndex ->
                    if (isObject(item) && (item.containsKey(key))) {
                        item[key] = traverseAndReplace(item.get(key), keys, innerIndex, replacement)
                        data[itemIndex] = item
                        innerCounter++
                    }
                }
                // all items key valid and value has been replaced
                if (innerCounter == data.size()) {
                    return data
                } else {
                    throw new JSONFormatException("JSONArray does not contain all the objects/items it's required to have.")
                }
                // key format: [index]
            } else if (key.toString().isNumber()) {
                key = key as int
                if (key >= ZERO && key < data.size()) {
                    data[key] = traverseAndReplace(data.get(key), keys, ++keyIndex, replacement)
                    return data
                } else {
                    throw new IndexOutOfBoundsException("Key: '${generateKeyFromList(keys)}', Index: $key, Size: ${data.size()}.")
                }
            } else {
                throw new InvalidKeyException("Key '${generateKeyFromList(keys)}' not found in JSONObject.")
            }
        } else {
            throw new InvalidKeyException("Key '${generateKeyFromList(keys)}' not found in JSONObject.")
        }
    }

    /**
     * Search json formatted data by a list of keys;
     * remove all the matched key-values'
     * and return JsonObject data.
     * For unusual cases throws exceptions.
     *
     * @param jsonData
     * @param keys
     * @return JSONObject data
     */
    def removeAllKeys(def jsonData, List keys) {
        if (!isObject(jsonData) && !isArray(jsonData)) {
            throw new MissingMethodException(new Object() {
            }.getClass().getEnclosingMethod().getName(), this.getClass(), jsonData, keys)
        }
        def jsonDataCopy
        if (isArray(jsonData)) {
            jsonDataCopy = JSONArray.fromObject(jsonData.toString())
        } else {
            jsonDataCopy = JSONObject.fromObject(jsonData.toString())
        }
        for (key in keys) {
            jsonDataCopy = removeKey(jsonDataCopy, key)
        }
        return jsonDataCopy
    }

    /**
     * Search json formatted data by a single key;
     * remove all the matched key-values'
     * and return JsonObject data.
     * For unusual cases throws exceptions.
     *
     * @param jsonData
     * @param key
     * @return JSONObject data
     */
    def removeKey(def jsonData, String key) {
        if (!isObject(jsonData) && !isArray(jsonData)) {
            throw new MissingMethodException(new Object() {
            }.getClass().getEnclosingMethod().getName(), this.getClass(), jsonData, key)
        }
        def keys = prepareKeys(key)
        if (keys.size() == ZERO) {
            if (isArray(jsonData)) {
                return JSONArray.fromObject("[]")
            } else {
                throw new InvalidKeyException("Key $key not found in JSONObject.")
            }
        }
        def jsonDataCopy
        if (isArray(jsonData)) {
            jsonDataCopy = JSONArray.fromObject(jsonData.toString())
        } else {
            jsonDataCopy = JSONObject.fromObject(jsonData.toString())
        }
        return traverseAndRemove(jsonDataCopy, keys, ZERO)
    }

    /**
     * Recursively search json formatted data by keys;
     * If found then remove the key-value and return data
     * Else throws exceptions
     *
     * @param data
     * @param keys
     * @param keyIndex
     * @return data
     */
    private def traverseAndRemove(def data, String[] keys, int keyIndex) {
        def key = keys[keyIndex]
        // last key
        if ((keyIndex >= keys.size() - 1)) {
            // data type Object and contains key
            if (isObject(data) && data.containsKey(key)) {
                data.remove(key)
                //data type Array and int key
            } else if (isArray(data) && key.toString().isNumber()) {
                data.remove(key as int)
            } else {
                throw new InvalidKeyException("Key '${generateKeyFromList(keys)}' not found in JSONObject.")
            }
            return data
        }
        // data type Object
        if (isObject(data)) {
            // key-value exist
            if (data.containsKey(key)) {
                data[key] = traverseAndRemove(data.get(key), keys, ++keyIndex)
                return data
            } else {
                throw new InvalidKeyException("Key '${generateKeyFromList(keys)}' not found in JSONObject.")
            }
            // data type Array
        } else if (isArray(data)) {
            // key format: []
            if (key.equals(StringUtils.EMPTY)) {
                keyIndex++
                if (keyIndex >= keys.size()) {
                    throw new IndexOutOfBoundsException()
                }
                key = keys[keyIndex]
                // key format array[].key
                def innerCounter = ZERO
                data.eachWithIndex { item, itemIndex ->
                    if (isObject(item) && (item.containsKey(key))) {
                        data[itemIndex] = traverseAndRemove(item, keys, keyIndex)
                        innerCounter++
                    }
                }
                // all items key valid and key-value has been removed
                if (innerCounter == data.size()) {
                    return data
                } else {
                    throw new JSONFormatException("JSONArray does not contain all the objects/items it's required to have.")
                }
                // key format: [index]
            } else if (key.toString().isNumber()) {
                key = key as int
                if (key >= ZERO && key < data.size()) {
                    data[key] = traverseAndRemove(data.get(key), keys, ++keyIndex)
                    return data
                } else {
                    throw new IndexOutOfBoundsException("Key: '${generateKeyFromList(keys)}', Index: $key, Size: ${data.size()}.")
                }
            } else {
                throw new InvalidKeyException("Key '${generateKeyFromList(keys)}' not found in JSONObject.")
            }
        } else {
            throw new InvalidKeyException("Key '${generateKeyFromList(keys)}' not found in JSONObject.")
        }
    }

    /**
     * Search json formatted data by key;
     * add value matched by the key'
     * and return JsonObject data;
     * for unusual cases throws exceptions.
     *
     * Note: i) Add the new key at the end of reference key;
     * Ex. If ref object key "parent.child" and want to add value "testValue" to new key "name" then
     * the key will be "parent.child.name".
     * ii) Use "n" inside [] (Ex. list[n]) to ref an array;
     * Ex. If "list" is an array and want to add new item there then key will be "parent.list[n]".
     *
     *
     * @param jsonData
     * @param key
     * @param appendingData
     * @return json data
     */
    def appendDataByKey(def jsonData, String key, def appendingData) {
        if (!isObject(jsonData) && !isArray(jsonData)) {
            throw new MissingMethodException(new Object() {
            }.getClass().getEnclosingMethod().getName(), this.getClass(), jsonData, key, appendingData)
        }
        def keys = prepareKeys(key)
        if (keys.size() == ZERO) {
            throw new InvalidKeyException("Key $key not found in JSONObject.")
        }
        def jsonDataCopy
        if (isArray(jsonData)) {
            jsonDataCopy = JSONArray.fromObject(jsonData.toString())
        } else {
            jsonDataCopy = JSONObject.fromObject(jsonData.toString())
        }
        appendingData = appendingData == null ? JSONObject.fromObject(null) : appendingData
        return traverseAndAppend(jsonDataCopy, keys, ZERO, appendingData)
    }

    /**
     * Recursively search json formatted data by keys;
     * add value by the valid key and return data,
     * for unusual cases throws exceptions.
     *
     * @param data
     * @param keys
     * @param keyIndex
     * @param appendingData
     * @return json data
     */
    private def traverseAndAppend(def data, String[] keys, int keyIndex, def appendingData) {
        def key = keys[keyIndex]
        // last key
        if ((keyIndex >= keys.size() - 1)) {
            // data type Object and unique key
            if (isObject(data) && !data.containsKey(key)) {
                data[key] = appendingData
                //data type Array and key contains symbol (Ex. [n])
            } else if (isArray(data) && key.equalsIgnoreCase(Constants.LAST_INDEX)) {
                data[data.size()] = appendingData
            } else {
                throw new InvalidKeyException("JSONObject Key '${generateKeyFromList(keys)}' already exist.")
            }
            return data
        }
        // data type Object
        if (isObject(data)) {
            // key-value exist
            if (data.containsKey(key)) {
                data[key] = traverseAndAppend(data.get(key), keys, ++keyIndex, appendingData)
                return data
            } else {
                throw new InvalidKeyException("Key '${generateKeyFromList(keys)}' not found in JSONObject.")
            }
            // data type Array
        } else if (isArray(data)) {
            // key format: []
            if (key.equals(StringUtils.EMPTY)) {
                keyIndex++
                if (keyIndex >= keys.size()) {
                    throw new IndexOutOfBoundsException()
                }
                def innerCounter = ZERO
                data.eachWithIndex { item, itemIndex ->
                    if (isObject(item)) {
                        data[itemIndex] = traverseAndAppend(item, keys, keyIndex, appendingData)
                        innerCounter++
                    }
                }
                // all items key valid and key-value has been added
                if (innerCounter == data.size()) {
                    return data
                } else {
                    throw new JSONFormatException("JSONArray does not contain all the objects/items it's required to have.")
                }
                // key format: [index]
            } else if (key.toString().isNumber()) {
                key = key as int
                if (key >= ZERO && key < data.size()) {
                    data[key] = traverseAndAppend(data.get(key), keys, ++keyIndex, appendingData)
                    return data
                } else {
                    throw new IndexOutOfBoundsException("Key: '${generateKeyFromList(keys)}', Index: $key, Size: ${data.size()}.")
                }
            } else {
                throw new InvalidKeyException("Key '${generateKeyFromList(keys)}' not found in JSONObject.")
            }
        } else {
            throw new InvalidKeyException("Key '${generateKeyFromList(keys)}' not found in JSONObject.")
        }
    }

    /**
     * Call compareData() function to compare between two json data;
     * Shows error on assert failure.
     *
     * @param expectedData
     * @param actualData
     * @param excludedKeys
     * @param orderKeys
     */
    void isMatchIgnoreExcludedKeys(def expectedData, def actualData, List excludedKeys = [], List orderKeys = []) {
        if ((!isObject(expectedData) && !isArray(expectedData)) || (!isObject(actualData) && !isArray(actualData))) {
            throw new MissingMethodException(new Object() {
            }.getClass().getEnclosingMethod().getName(), this.getClass(), expectedData, actualData, excludedKeys, orderKeys)
        }
        keyStack = []
        compareData(expectedData, actualData, prepareRegexKeys(excludedKeys), orderKeys)
    }

    /**
     * Compare between two json data recursively and assert;
     * Ignore keys in excludedKeys while comparing;
     * Show error log when mismatch.
     * @param expectedData
     * @param actualData
     * @param excludedKeys
     * @param orderKeys
     */
    private void compareData(def expectedData, def actualData, List excludedKeys, List orderKeys) {
        // both object type
        if (isObject(expectedData) && isObject(actualData)) {
            def expectedKeys = expectedData.keySet()
            def actualKeys = actualData.keySet()
            if (expectedKeys.size() < actualKeys.size()) {
                assert expectedKeys.size() == actualKeys.size(),
                        ["Parent node: " + generateKeyFromStack(keyStack), "Found more keys in actual response than exist in expected response."]
            }
            def keyTraverseCounter = ZERO
            for (key in expectedKeys) {
                keyStack.push(key)
                // key not excluded
                if (!isExcludedKey(excludedKeys, generateKeyFromStack(keyStack))) {
                    compareData(expectedData[key], actualData[key], excludedKeys, orderKeys)
                    //key excluded but absent in actual response
                } else if ((expectedKeys.size() == actualKeys.size()) && !actualKeys.contains(key)) {
                    keyStack.pop()
                    assert actualKeys.contains(key),
                            ["Parent node: " + generateKeyFromStack(keyStack), "Found mismatched key in actual response which doesn't exist in expected response."]
                } else {
                    keyStack.pop()
                }
                keyTraverseCounter++
            }
            // all keys traversal finished of parent object
            if ((keyTraverseCounter == expectedKeys.size()) && !keyStack.isEmpty()) {
                keyStack.pop()
            }
            // both array type
        } else if (isArray(expectedData) && isArray(actualData)) {
            if (expectedData.size() < actualData.size()) {
                assert expectedData.size() == actualData.size(),
                        ["Parent node: " + generateKeyFromStack(keyStack), "Found more node in actual response than exist in expected response."]
            }
            def orderBy
            if (!keyStack.isEmpty()) {
                orderBy = getOrderBy(keyStack.first(), orderKeys)
            }
            int index = ZERO
            for (; index < expectedData.size(); index++) {
                keyStack.push(index)
                // not excluded key
                if (!isExcludedKey(excludedKeys, generateKeyFromStack(keyStack))) {
                    def expectedValue = expectedData[index]
                    def actualValue = getActualValueInProperOrder(expectedValue, actualData, orderBy, index)
                    compareData(expectedValue, actualValue, excludedKeys, orderKeys)
                } else {
                    keyStack.pop()
                }
            }
            // all keys traversal finished of parent object
            if ((index == expectedData.size()) && !keyStack.isEmpty()) {
                keyStack.pop()
            }
            // one data is object type but the other is not
        } else if ((isObject(expectedData) && !isObject(actualData)) || (isArray(expectedData) && !isArray(actualData))) {
            def expectedDataType = expectedData.getClass()
            def actualDataType = actualData.getClass()
            def keyString = generateKeyFromStack(keyStack)
            assert expectedDataType.equals(actualDataType), ["Key: " + keyString, "Expected Data Type: " + expectedDataType, "Type mismatch."]
        } else {
            def keyString = generateKeyFromStack(keyStack)
            keyStack.pop()
            assert expectedData.equals(actualData), ["Key: " + keyString, "Expected: " + expectedData, "Actual: " + actualData]
        }
    }

    /**
     * Find which object of actualData has the same orderBy key value
     * as expectedValue. If found then return the object
     * else return object of same index.
     * @param expectedValue
     * @param actualData
     * @param orderBy
     * @return actualValue
     */
    private def getActualValueInProperOrder(def expectedValue, def actualData, String orderBy, int currentIndex) {
        if (StringUtils.isNotEmpty(orderBy)) {
            for (item in actualData) {
                // match value in actual with expected by orderBy key
                if (expectedValue[orderBy].equals(item[orderBy])) {
                    return item
                }
            }
        }
        return actualData[currentIndex]
    }

    /**
     * Split each items from orderKeys list and match array name with
     * the dataArrayName, then return matched orderBy or null.
     * Examples: dataArrayName="arrayA" and orderKeys=["arrayA.id", "arrayB.primaryKey"] return "id"
     * @param dataArrayName
     * @param orderKeys
     * @return orderBy
     */
    private def getOrderBy(String dataArrayName, List orderKeys) {
        def orderBy = null
        if (orderKeys.size() == ZERO) {
            return orderBy
        }
        for (item in orderKeys) {
            def array = item.split("\\.")
            //data array name found in provided orderKeys
            if (array.size() == TWO && dataArrayName.equals(array[ZERO])) {
                orderBy = array[ONE]
                break
            }
        }
        return orderBy
    }

    /**
     * Match key with each regex from excludeKeys list
     * and return boolean value.
     * Examples: excludedKeys=["A.B\\[[0-9]+].C\\[1].D"] and key="A.B[2].C[1].D" return true
     * and excludedKeys=["A.B\\[[0-9]+].C\\[].D"] and key="A.B[2].C[0].D" return false
     * @param excludedKeys
     * @param key
     * @return boolean
     */
    private def isExcludedKey(List excludedKeys, String key) {
        for (regex in excludedKeys) {
            if (key.matches(regex)) {
                return true
            }
        }
        return false
    }

    /**
     * Take a keyString and return array of valid keys.
     * Throws InvalidKeyException for invalid key.
     * Example: KeyString key "A.B[].C[1].D" returns ["A", "B","", "C", "1", "D"].
     * NB: "[]" returns empty list [].
     * @param key
     * @return keys[]
     */
    private def prepareKeys(String key) {
        if (!key?.trim()) {
            throw new InvalidKeyException("Key may not be empty or null.")
        }
        def keys = key.split(Constants.KEY_SPLITTER_REGEX)
        // key starts with array index (Ex. [0].A)
        if (key.matches(Constants.START_WITH_ARRAY_INDEX_REGEX)) {
            keys = ArrayUtils.remove(keys, ZERO)
        }
        return keys
    }

    /**
     * Prepare list of regex key from excludedKeys list
     * and return the prepared list.
     * Example: Key "A.B[].C[1].D" generates regex pattern "A.B\\[[0-9]+].C\\[1].D"
     * @param excludedKeys
     * @return regexKeys
     */
    private def prepareRegexKeys(List excludedKeys) {
        def regexKeys = []
        for (key in excludedKeys) {
            if (key.contains("[")) {
                key = key.replaceAll("\\[", "\\\\[")
            }
            if (key.contains("[]")) {
                key = key.replaceAll("\\[]", Constants.DIGIT_REGEX)
            }
            regexKeys.add(key)
        }
        return regexKeys
    }

    /**
     * Takes a key stack and generate key.
     * Example: keyStack ["D", "1", "C", "", "B", "A"] return key "A.B[].C[1].D"
     * @param keyStack
     * @return key
     */
    private def generateKeyFromStack(def keyStack) {
        def key = ""
        keyStack.eachWithIndex { item, index ->
            if (index == ZERO) {
                if (item.toString().isNumber()) {
                    key = "[" + item + "]"
                } else {
                    key = item
                }
            } else if (item.toString().isNumber()) {
                key = "[" + item + "]." + key
            } else if (keyStack[index - ONE].toString().isNumber()) {
                key = item + key
            } else {
                key = item + "." + key
            }
        }
        return key.equals(StringUtils.EMPTY) ? "{}" : key
    }

    /**
     * Takes a key list and generate key.
     * Example: keyList ["A", "B","" , "C", "1", "D"] return key "A.B[].C[1].D"
     * @param keyList
     * @return key
     */
    private def generateKeyFromList(String[] keyList) {
        def key = ""
        keyList.eachWithIndex { item, index ->
            if (index == ZERO) {
                if (item == StringUtils.EMPTY) {
                    key = key + "[]"
                } else if (item.toString().isNumber()) {
                    key = key + "[" + item + "]"
                } else {
                    key = item
                }
            } else if (item.toString().isNumber()) {
                key = key + "[" + item + "]"
            } else if (item.equals(StringUtils.EMPTY)) {
                key = key + "[]"
            } else {
                key = key + "." + item
            }
        }
        return key
    }

    /**
     * Check if a value is an instance of JSONObject or not.
     * @param value
     * @return boolean
     */
    boolean isObject(def value) {
        return (value instanceof JSONObject)
    }

    /**
     * Check if a value is an instance of JSONArray or not.
     * @param value
     * @return boolean
     */
    boolean isArray(def value) {
        return (value instanceof JSONArray)
    }

    /**
     * Take JSONObject and list of JSONArray names;
     * find the objects which key match with the array names,
     * then convert the objects into arrays and return
     *
     * @param JSONObject jsonData
     * @param List jsonArrayNames
     * @return JSONObject jsonData
     */
    def defineArrayInJsonObj(def jsonData, List jsonArrayNames) {
        if (isObject(jsonData)) {
            def keySet = jsonData.keySet()
            for (key in keySet) {
                if (jsonArrayNames.contains(key) && !isArray(jsonData[key])) {
                    JSONArray jsonArray = JSONArray.fromObject([])
                    jsonArray[ZERO] = defineArrayInJsonObj(jsonData[key], jsonArrayNames)
                    jsonData[key] = jsonArray
                } else {
                    jsonData[key] = defineArrayInJsonObj(jsonData[key], jsonArrayNames)
                }
            }
            return jsonData
        } else if (isArray(jsonData)) {
            for (int index = ZERO; index < jsonData.size(); index++) {
                jsonData[index] = defineArrayInJsonObj(jsonData[index], jsonArrayNames)
            }
            return jsonData
        } else {
            return jsonData
        }
    }
}
