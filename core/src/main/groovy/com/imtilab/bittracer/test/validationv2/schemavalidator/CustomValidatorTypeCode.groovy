package com.imtilab.bittracer.test.validationv2.schemavalidator

import com.fasterxml.jackson.databind.JsonNode
import com.networknt.schema.ErrorMessageType
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonValidator
import com.networknt.schema.Keyword
import com.networknt.schema.SpecVersion
import com.networknt.schema.ValidationContext

import java.lang.reflect.Constructor
import java.text.MessageFormat

enum CustomValidatorTypeCode implements Keyword, ErrorMessageType {
    REQUIRED_IN_ANY("requiredInAny", "C-0001", new MessageFormat("{0}: is missing but it is required at least one"), RequiredInAnyValidator.class, 15)

    private static Map<String, CustomValidatorTypeCode> constants = new HashMap<String, CustomValidatorTypeCode>()
    private static SpecVersion specVersion = new SpecVersion()

    static {
        for (CustomValidatorTypeCode c : values()) {
            constants.put(c.value, c)
        }
    }

    private final String value
    private final String errorCode
    private final MessageFormat messageFormat
    private final String errorCodeKey
    private final Class validator
    private final long versionCode


    private CustomValidatorTypeCode(String value, String errorCode, MessageFormat messageFormat, Class validator, long versionCode) {
        this.value = value
        this.errorCode = errorCode
        this.messageFormat = messageFormat
        this.errorCodeKey = value + "ErrorCode"
        this.validator = validator
        this.versionCode = versionCode
    }

    static CustomValidatorTypeCode fromValue(String value) {
        CustomValidatorTypeCode constant = constants.get(value)
        if (constant == null) {
            throw new IllegalArgumentException(value)
        } else {
            return constant
        }
    }

    JsonValidator newValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext) throws Exception {
        if (validator == null) {
            throw new UnsupportedOperationException("No suitable validator for " + getValue())
        }
        // if the config version is not match the validator
        @SuppressWarnings("unchecked")
        Constructor<JsonValidator> c = ((Class<JsonValidator>) validator).getConstructor(
                [String.class, JsonNode.class, JsonSchema.class, ValidationContext.class])
        return c.newInstance(schemaPath + "/" + getValue(), schemaNode, parentSchema, validationContext)
    }

    @Override
    String toString() {
        return this.value
    }

    String getValue() {
        return value
    }

    String getErrorCode() {
        return errorCode
    }

    MessageFormat getMessageFormat() {
        return messageFormat
    }

    String getErrorCodeKey() {
        return errorCodeKey
    }

    long getVersionCode() {
        return versionCode
    }
}