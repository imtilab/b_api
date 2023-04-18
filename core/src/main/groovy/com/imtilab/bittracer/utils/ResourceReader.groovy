package com.imtilab.bittracer.utils

import groovy.text.GStringTemplateEngine
import groovy.util.logging.Commons
import com.imtilab.bittracer.constant.Constants

/**
 * Read file contents
 */
@Commons
class ResourceReader {

    /**
     * Return file content as input stream
     * @param fileName
     * @return
     */
    static InputStream loadAsInputStream(String fileName) {
        InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream(fileName)
        throwResourceNotFoundException(inputStream, fileName)
        return inputStream
    }

    /**
     * Return file url
     * @param fileName
     * @return
     */
    static URL getUrl(String fileName) {
        URL url = ClassLoader.getSystemClassLoader().getResource(fileName)
        throwResourceNotFoundException(url, fileName)
        return url
    }

    private static def throwResourceNotFoundException(def resource, String resourceName) {
        if (resource == null)
            throw new FileNotFoundException(Constants.RESOURCE_NOT_IN_CLASSPATH(resourceName))
    }


    /**
     * Convert input stream of properties to Map
     * @param inputStream
     * @return
     */
    static Map loadInputStreamToMap(InputStream inputStream, boolean enableTypeConversion) {
        try {
            Map tmp = [:]
            Properties properties = new Properties()
            properties.load(inputStream)
            properties.each { prop, val ->
                if (enableTypeConversion) {
                    val = CommonUtils.convertToPrimitive(val)
                }
                tmp.put(prop, val)
            }
            return tmp
        } catch (ex) {
        }
        return Collections.EMPTY_MAP
    }

    /**
     * Read file from url and return file contents after replacing variable
     * @param url
     * @return
     */
    static def getTextFromUrlAndReplaceWithValues(URL url, Map replacements) {
        try {
            GStringTemplateEngine engine = new GStringTemplateEngine()
            Writable make = engine.createTemplate(url).make(replacements)
            return make.toString()
        } catch (MissingPropertyException e) {
            log.info Constants.PROPERTY_NOT_IN_RESOURCE(e.property)
            throw new MissingPropertyException(Constants.PROPERTY_NOT_IN_RESOURCE(e.property))
        }
    }

    /**
     * Replace variables of contents from replacements map
     * @param contents
     * @param replacements
     * @return
     */
    static def getTextAndReplaceWithValues(String contents, Map replacements) {
        try {
            GStringTemplateEngine engine = new GStringTemplateEngine()
            Writable make = engine.createTemplate(contents).make(replacements)
            return make.toString()
        } catch (MissingPropertyException e) {
            log.info Constants.PROPERTY_NOT_IN_RESOURCE(e.property)
            throw new MissingPropertyException(Constants.PROPERTY_NOT_IN_RESOURCE(e.property))
        }
    }

    static String setDataFromProperties(String contents, ApplicationResource applicationResource) {
        try {
            GStringTemplateEngine engine = new GStringTemplateEngine()
            Writable make = engine.createTemplate(contents).make(applicationResource.getData())
            return make.toString()
        } catch (MissingPropertyException e) {
            if(contents.contains(Constants.DASH)){
                log.info Constants.PROPERTY_NOT_IN_RESOURCE(e.property)+"\n"+Constants.DASH_CONTAINING_ERROR_MSG(contents)
                throw new MissingPropertyException(Constants.PROPERTY_NOT_IN_RESOURCE(e.property)+"\n"+Constants.DASH_CONTAINING_ERROR_MSG(contents))
            }else{
                log.info Constants.PROPERTY_NOT_IN_RESOURCE(e.property)
                throw new MissingPropertyException(Constants.PROPERTY_NOT_IN_RESOURCE(e.property))
            }
        }
    }
}
