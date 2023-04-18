package com.imtilab.bittracer.test.validationv2

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.Format
import com.networknt.schema.JsonMetaSchema
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.NonValidationKeyword
import com.networknt.schema.SpecVersionDetector
import com.networknt.schema.ValidatorTypeCode
import com.imtilab.bittracer.test.validationv2.schemavalidator.RequiredInAnyValidator

class JsonSchemaValidator {

    static Set validate(String json, String schema) {
        JsonNode schemaNode = getJsonNodeFromStringContent(schema)
        JsonSchemaFactory factory = getSchemaFactory(schemaNode)
        JsonSchema jsonSchema = factory.getSchema(schemaNode)
        JsonNode jsonNode = getJsonNodeFromStringContent(json)
        jsonSchema.validate(jsonNode)
    }

    protected static JsonNode getJsonNodeFromStringContent(String content) throws IOException {
        ObjectMapper mapper = new ObjectMapper()
        return mapper.readTree(content)
    }

    // Automatically detect version for given JsonNode
    protected static JsonSchema getJsonSchemaFromJsonNodeAutomaticVersion(JsonNode jsonNode) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersionDetector.detect(jsonNode))
        return factory.getSchema(jsonNode)
    }

    protected static JsonSchemaFactory getSchemaFactory(JsonNode jsonNode) {
        String URI = "https://json-schema.org/draft-07/schema"
        String ID = "\$id"
        List<Format> BUILTIN_FORMATS = new ArrayList<Format>(JsonMetaSchema.COMMON_BUILTIN_FORMATS)
        JsonMetaSchema myJsonMetaSchema = new JsonMetaSchema.Builder(URI)
                .idKeyword(ID)
                .addFormats(BUILTIN_FORMATS)
                .addKeywords(ValidatorTypeCode.getNonFormatKeywords(SpecVersionDetector.detect(jsonNode)))
        // keywords that may validly exist, but have no validation aspect to them
                .addKeywords(Arrays.asList(
                        new NonValidationKeyword("\$schema"),
                        new NonValidationKeyword("\$id"),
                        new NonValidationKeyword("title"),
                        new NonValidationKeyword("description"),
                        new NonValidationKeyword("default"),
                        new NonValidationKeyword("definitions"),
                        new NonValidationKeyword("\$defs")  // newly added in 2018-09 release.
                ))
        // add your custom keyword
                .addKeyword(new RequiredInAnyValidator())
                .build()

        return new JsonSchemaFactory.Builder().defaultMetaSchemaURI(myJsonMetaSchema.getUri())
                .addMetaSchema(myJsonMetaSchema)
                .build()
    }
}