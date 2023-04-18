package com.imtilab.bittracer.utils

import groovy.util.logging.Slf4j
import com.imtilab.bittracer.constant.Constants
import com.imtilab.bittracer.constant.PropertiesKey
import org.yaml.snakeyaml.Yaml

/**
 * Read application resource files
 */
@Slf4j
class ConfigurationResource {

    static ConfigurationResource instance
    Map configuration
    Map yamlData

    static ConfigurationResource instance() {
        if (instance == null) {
            instance = new ConfigurationResource()
            instance.init()
        }
        return instance
    }

    def init() {
        if (!configuration) {
            configuration = loadData(ResourceReader.loadAsInputStream(Constants.CONFIG_FILE), false)
            String customDomain = System.getProperty(PropertiesKey.API_BASE_URL)
            if (customDomain) {
                configuration['base.url'] = customDomain
            }
        }

        if (!yamlData) {
            yamlData = loadYamlData(ResourceReader.loadAsInputStream(Constants.YAML_DATA_FILE))
        }
    }

    def loadYamlData(InputStream inputStream) {
        Yaml yaml = new Yaml()
        Map<String, Object> yamlMaps = yaml.load(inputStream)
        return yamlMaps
    }

    def loadData(InputStream inputStream, boolean enableTypeConversion) {
        def map = [:]
        (map).with {
            putAll(ResourceReader.loadInputStreamToMap(inputStream, enableTypeConversion))
        }
        replaceSystemData(map)
    }

    def replaceSystemData(def map) {
        String webBffApiKey = System.getProperty(PropertiesKey.WEB_BFF_APIKEY)
        String rBicClientApiKey = System.getProperty(PropertiesKey.R_BIC_CLIENT_APIKEY)
        String ichibaTopClientApiKey = System.getProperty(PropertiesKey.ICHIBA_TOP_CLIENT_APIKEY)
        if (webBffApiKey) {
            map.put(PropertiesKey.WEB_BFF_APIKEY, webBffApiKey)
        }
        if (rBicClientApiKey) {
            map.put(PropertiesKey.R_BIC_CLIENT_APIKEY, rBicClientApiKey)
        }
        if (ichibaTopClientApiKey) {
            map.put(PropertiesKey.ICHIBA_TOP_CLIENT_APIKEY, ichibaTopClientApiKey)
        }
        map
    }

    def valueOfConfig(String key) {
        configuration.get(key)
    }

    def configMap() {
        configuration
    }

    def getConfigProp(String propertyName) {
        valueOfConfig(propertyName)
    }

    Map getConfig() {
        configMap()
    }

    def yamlDataMap(String key) {
        yamlData.get(key)
    }
}
