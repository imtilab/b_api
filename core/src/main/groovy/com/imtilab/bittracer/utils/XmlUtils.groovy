package com.imtilab.bittracer.utils


import net.sf.json.JSONObject
import org.json.XML

class XmlUtils {

    /**
     * Take String content and check if it's xml or not;
     * return true if xml content else return false.
     *
     * @param content
     * @return boolean
     */
    static boolean isXmlContent(String content) {
        return content.matches("(?s).*(<(\\w+)[^>]*>.*</\\2>|<(\\w+)[^>]*/>)")
    }

    /**
     * Take a xml and a list of jsonArray names;
     * then convert the xml to JSONObject and return.
     *
     * @param xml
     * @param jsonArrayNames
     * @return JSONObject
     */
    static JSONObject convertXmlToJson(String xml, List xmlArrayNames = []) {
        def json = XML.toJSONObject(xml)
        JSONObject jsonObject = JSONObject.fromObject(json.toString())
        return new JSONDataUtils().defineArrayInJsonObj(jsonObject, xmlArrayNames)
    }

    /**
     * Take JSONObject, convert it to xml format (String type) and return.
     *
     * @param jsonObject
     * @return xml in String type
     */
    static String convertJsonToXml(JSONObject jsonObject) {
        def orgJson = new org.json.JSONObject(jsonObject.toString())
        return XML.toString(orgJson)
    }
}
