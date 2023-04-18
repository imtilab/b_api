package com.imtilab.bittracer.model

import com.xlson.groovycsv.PropertyMapper
import groovy.transform.builder.Builder

@Builder
class TestRow {
    PropertyMapper row
    def headers
    String apiVersion
}
