package com.imtilab.bittracer.test.validationv2.schemavalidator

import com.fasterxml.jackson.databind.JsonNode
import com.networknt.schema.AbstractJsonValidator
import com.networknt.schema.AbstractKeyword
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaException
import com.networknt.schema.JsonValidator
import com.networknt.schema.ValidationContext
import com.networknt.schema.ValidationMessage
import groovy.util.logging.Slf4j

@Slf4j
class RequiredInAnyValidator extends AbstractKeyword {
    RequiredInAnyValidator() {
        super(CustomValidatorTypeCode.REQUIRED_IN_ANY.value)
    }

    @Override
    JsonValidator newValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext) throws JsonSchemaException, Exception {
        List<String> fieldNames = new ArrayList<String>()
        List<Boolean> isAlreadyExist = new ArrayList<Boolean>()
        if (schemaNode.isArray()) {
            for (JsonNode fieldNme : schemaNode) {
                fieldNames.add(fieldNme.asText())
                isAlreadyExist.add(false)
            }
        }

        return new AbstractJsonValidator(getValue()) {
            @Override
            Set<ValidationMessage> validate(JsonNode node, JsonNode rootNode, String at) {
                Set<ValidationMessage> errors = new LinkedHashSet<ValidationMessage>();

                for (int i = 0; i < fieldNames.size() && !isAlreadyExist.get(i); i++) {
                    String fieldName = fieldNames.get(i)

                    if (node.isArray()) {
                        for (JsonNode jsonNode : node) {
                            if (jsonNode.get(fieldName) != null) {
                                isAlreadyExist.set(i, true)
                                break
                            }
                        }
                    } else if (node.get(fieldName) != null) {
                        isAlreadyExist.set(i, true)
                    }
                }

                for (int i = 0; i < isAlreadyExist.size(); i++) {
                    Boolean isExist = isAlreadyExist.get(i)
                    if (!isExist) {
                        errors.add(ValidationMessageBuilder.build(
                                CustomValidatorTypeCode.REQUIRED_IN_ANY,
                                "$at.${fieldNames.get(i)}"))
                    }
                }
                return Collections.unmodifiableSet(errors);
            }
        }
    }
}
