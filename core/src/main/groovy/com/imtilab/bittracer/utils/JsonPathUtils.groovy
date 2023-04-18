package com.imtilab.bittracer.utils

import com.jayway.jsonpath.JsonPath

class JsonPathUtils {
    static def getDataByKey(def jsonData, String key, def postFilter = null/*currently only support for array index*/) {
        key = "\$." + key.replaceAll("\\[\\]", "[*]")
        def data = JsonPath.read(jsonData, key)
        if (postFilter) {
            if (postFilter.contains("-")) {
                String start, end
                (start, end) = postFilter.split("-")
                List values = []
                int endIndex = Integer.parseInt(end)
                for (int i = Integer.parseInt(start) - 1; i < endIndex; i++) {
                    values.add(data[i])
                }
                data = values
            } else {
                data = data[Integer.parseInt(postFilter) - 1]
            }
        }
        data
    }
}
